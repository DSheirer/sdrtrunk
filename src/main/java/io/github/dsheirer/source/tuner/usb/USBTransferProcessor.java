/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.source.tuner.usb;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.source.tuner.TunerManager;
import io.github.dsheirer.source.tuner.usb.converter.NativeBufferConverter;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;
import org.usb4java.Transfer;
import org.usb4java.TransferCallback;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class USBTransferProcessor implements TransferCallback
{
    private final static Logger mLog = LoggerFactory.getLogger(USBTransferProcessor.class);

    private static final byte USB_BULK_TRANSFER_ENDPOINT = (byte) 0x81;
    private static final long USB_TIMEOUT_MS = 2000l; //milliseconds

    //Number of native byte buffers to allocate for transferring data from the USB device
    private static final int TRANSFER_BUFFER_POOL_SIZE = 20;
    private static final int MAX_TRANSFERS_IN_PROGRESS_SIZE = 10;
    private int mTransfersToSubmit = 0;

    private LinkedTransferQueue<Transfer> mAvailableTransfers = new LinkedTransferQueue<>();
    private LinkedTransferQueue<Transfer> mInProgressTransfers = new LinkedTransferQueue<>();
    private LinkedTransferQueue<Transfer> mCompletedTransfers = new LinkedTransferQueue<>();

    //Tuner format-specific byte buffer to IQ float sample converter
    private NativeBufferConverter mNativeBufferConverter;

    //Byte array transfer buffers size in bytes
    private int mBufferSize;

    private Listener<ReusableComplexBuffer> mComplexBufferListener;

    //Handle to the USB bulk transfer device
    private DeviceHandle mUsbBulkTransferDeviceHandle;
    private AtomicBoolean mRunning = new AtomicBoolean();
    private ByteBuffer mLibUsbHandlerStatus = ByteBuffer.allocateDirect(4);
    private CompletedTransferProcessor mCompletedTransferProcessor = new CompletedTransferProcessor();
    private ScheduledFuture mBufferDispatcherFuture;
    private ScheduledFuture mRestartFuture;
    private String mDeviceName;


    /**
     * Manages stream of USB transfer buffers and converts buffers to complex buffer samples for distribution to
     * any registered listeners.
     *
     * @param deviceName to use when logging information or errors
     * @param usbBulkTransferDeviceHandle to the USB bulk transfer device
     * @param nativeBufferConverter specific to the tuner's byte buffer format for converting to floating point I/Q samples
     * @param bufferSize in bytes.  Should be a multiple of two: 65536, 131072 or 262144.
     */
    public USBTransferProcessor(String deviceName, DeviceHandle usbBulkTransferDeviceHandle,
                                NativeBufferConverter nativeBufferConverter, int bufferSize)
    {
        mDeviceName = deviceName;
        mUsbBulkTransferDeviceHandle = usbBulkTransferDeviceHandle;
        mNativeBufferConverter = nativeBufferConverter;
        mBufferSize = bufferSize;
    }

    /**
     * Modifies the usb transfer buffer size used for transfering native byte buffers from the
     * USB device.  Note: changing the buffer size while the transfer processor is running causes
     * the transfer to stop momentarily while the existing buffers are destroyed and new buffers
     * with the correct size are recreated.
     *
     * Note: this method is not thread safe.  Ensure that no other threads invoke stop() or start()
     * while a buffer size change is in progress.
     *
     * @param bufferSize to use for native usb buffer transfers
     */
    public void setBufferSize(int bufferSize)
    {
        if(bufferSize % 2 == 1)
        {
            throw new IllegalArgumentException("Buffer size must be a multiple of 2 for complex samples");
        }

        if(mBufferSize != bufferSize)
        {
            stop();
            mBufferSize = bufferSize;
            start();
        }
    }

    /**
     * Start USB transfer buffer processing.  Subsequent calls to this method after started will be ignored.
     */
    private void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            prepareDeviceStart();
            prepareTransfers();
            mTransfersToSubmit = MAX_TRANSFERS_IN_PROGRESS_SIZE;
            submitTransfers();

            //Start transferred buffer dispatcher
            mBufferDispatcherFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(mCompletedTransferProcessor,
                0, 6, TimeUnit.MILLISECONDS);

            //Register with LibUSB processor so that it auto-starts LibUSB processing
            TunerManager.LIBUSB_TRANSFER_PROCESSOR.registerTransferProcessor(this);
        }
    }

    /**
     * Stop USB transfer buffer processing.  Subsequent calls to this method after stopped will be ignored.
     */
    private void stop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            mBufferDispatcherFuture.cancel(true);

            //Cancel all buffers that are currently in progress
            for(Transfer transfer : mInProgressTransfers)
            {
                LibUsb.cancelTransfer(transfer);
            }

            //Clear all completed buffers
            Transfer completedTransfer = mCompletedTransfers.poll();

            while(completedTransfer != null)
            {
                completedTransfer.buffer().rewind();
                mAvailableTransfers.add(completedTransfer);
                completedTransfer = mCompletedTransfers.poll();
            }

            //Unregister from LibUSB processor so that it auto-stops LibUSB processing
            TunerManager.LIBUSB_TRANSFER_PROCESSOR.unregisterTransferProcessor(this);

            //Directly invoke the timeout handler to ensure that our cancelled transfer buffers are flushed.
            int result = LibUsb.handleEventsTimeoutCompleted(null, USB_TIMEOUT_MS,
                mLibUsbHandlerStatus.asIntBuffer());

            if(result != LibUsb.SUCCESS)
            {
                mLog.error(mDeviceName + " - error while cancelling transfer buffers during shutdown/pause - error code:" + result);
            }

            mLibUsbHandlerStatus.rewind();

            executeDeviceStop();
        }
    }

    /**
     * Restarts the device after there is an error.  Initially stops the device and then schedules a start() to
     * occur in 10 milliseconds.
     */
    private void restart()
    {
        stop();

        //Attempt to clear any halt condition
        LibUsb.clearHalt(mUsbBulkTransferDeviceHandle, USB_BULK_TRANSFER_ENDPOINT);

        mLog.warn("USB tuner [" + mDeviceName + "] stopped due to a buffer transfer error.  Restarting in 10 milliseconds");

        if(mRestartFuture == null)
        {
            Runnable runnable = new Runnable()
            {
                @Override
                public void run()
                {
                    start();
                    mRestartFuture = null;
                }
            };

            mRestartFuture = ThreadPool.SCHEDULED.schedule(runnable, 10, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * (Re)Submits transfer buffers to the USB device.  Since we're offloading byte buffer to complex sample conversion
     * to another thread, there may be times when sufficient transfer buffers are not available to keep a level number
     * of transfer buffers in progress.  Therefore, we track the transfer buffers to submit deficit number and attempt
     * to play catch-up each time this method is invoked.
     */
    private void submitTransfers()
    {
        if(mRunning.get())
        {
            int submitted = 0;

            for(int x = 0; x < mTransfersToSubmit; x++)
            {
                Transfer transfer = mAvailableTransfers.poll();

                if(transfer != null)
                {
                    int result = LibUsb.submitTransfer(transfer);

                    if(result == LibUsb.SUCCESS)
                    {
                        mInProgressTransfers.add(transfer);
                        submitted++;
                    }
                    else if(result == LibUsb.ERROR_PIPE)
                    {
                        mLog.warn("USB pipe error - attempting to clear halt on USB device [" + mDeviceName + "]");

                        int resetResult = LibUsb.clearHalt(mUsbBulkTransferDeviceHandle, USB_BULK_TRANSFER_ENDPOINT);

                        if(resetResult == LibUsb.SUCCESS)
                        {
                            int resubmitResult = LibUsb.submitTransfer(transfer);

                            if(resubmitResult == LibUsb.SUCCESS)
                            {
                                mInProgressTransfers.add(transfer);
                                submitted++;
                            }
                            else
                            {
                                mLog.error(mDeviceName + " - error resubmitting transfer after endpoint clear halt");
                            }
                        }
                        else
                        {
                            mLog.error(mDeviceName + " - unable to clear device endpoint halt");
                        }
                    }
                    else
                    {
                        //TODO: broadcast to each listener that this source has an error and is shutting down
                        mLog.error(mDeviceName + "- error submitting transfer [" + LibUsb.errorName(result) + "]");
                    }
                }
            }

            mTransfersToSubmit -= submitted;
        }
    }

    /**
     * Allows sub-class implementations to execute any device-specific operations to prepare for starting USB transfers
     */
    protected void prepareDeviceStart()
    {

    }

    /**
     * Allows sub-class implementations to execute any device-specific operations after stopping USB transfers
     */
    protected void executeDeviceStop()
    {

    }

    /**
     * Sets the listener and auto-starts the buffer processor
     */
    public void setListener(Listener<ReusableComplexBuffer> listener)
    {
        mComplexBufferListener = listener;
        start();
    }

    /**
     * Auto-stops the buffer processor and removes the listener
     */
    public void removeListener()
    {
        if(mComplexBufferListener != null)
        {
            stop();
            mComplexBufferListener = null;
        }
    }

    /**
     * Prepares (allocates) a set of transfer buffers for use in transferring data from the USB device via the bulk
     * interface.  Since we're using direct memory allocation (native), buffers are retained and reused across multiple
     * start/stop cycles.
     */
    private void prepareTransfers() throws LibUsbException
    {
        while(mAvailableTransfers.size() < TRANSFER_BUFFER_POOL_SIZE)
        {
            Transfer transfer = LibUsb.allocTransfer();

            if(transfer == null)
            {
                throw new LibUsbException("Couldn't allocate USB transfer buffer", LibUsb.ERROR_NO_MEM);
            }

            final ByteBuffer buffer = ByteBuffer.allocateDirect(mBufferSize);

            LibUsb.fillBulkTransfer(transfer, mUsbBulkTransferDeviceHandle, USB_BULK_TRANSFER_ENDPOINT, buffer, this,
                "Buffer", USB_TIMEOUT_MS);

            mAvailableTransfers.add(transfer);
        }
    }

    /**
     * Process a filled transfer buffer received back from the USB device.  Note: this method is invoked on the USB
     * bus processing thread, so we try to keep processing to a minimum and place transfers in the completed
     * transfer queue so that the scheduled processor thread handles any conversion and additional downstream
     * processing workload.
     */
    @Override
    public void processTransfer(Transfer transfer)
    {
        mInProgressTransfers.remove(transfer);

        switch(transfer.status())
        {
            case LibUsb.TRANSFER_COMPLETED:
            case LibUsb.TRANSFER_STALL:
            case LibUsb.TRANSFER_TIMED_OUT:
                if(transfer.actualLength() > 0)
                {
                    mCompletedTransfers.add(transfer);
                }
                else
                {
                    transfer.buffer().rewind();
                    mAvailableTransfers.add(transfer);
                }
                break;
            case LibUsb.TRANSFER_CANCELLED:
                transfer.buffer().rewind();
                mAvailableTransfers.add(transfer);
                break;
            default:
                //Unexpected transfer error - need to reset the bulk transfer interface
                mLog.error(mDeviceName + " - transfer error [" + getTransferStatus(transfer.status()) +
                    "] transferred actual: " + transfer.actualLength());
                transfer.buffer().rewind();
                mAvailableTransfers.add(transfer);
                restart();
                return;
        }

        mTransfersToSubmit++;

        //Dispatch a new transfer
        submitTransfers();
    }

    /**
     * Converts the USB transfer status number into a descriptive label
     */
    public static String getTransferStatus(int status)
    {
        switch(status)
        {
            case 0:
                return "TRANSFER COMPLETED (0)";
            case 1:
                return "TRANSFER ERROR (1)";
            case 2:
                return "TRANSFER TIMED OUT (2)";
            case 3:
                return "TRANSFER CANCELLED (3)";
            case 4:
                return "TRANSFER STALL (4)";
            case 5:
                return "TRANSFER NO DEVICE (5)";
            case 6:
                return "TRANSFER OVERFLOW (6)";
            default:
                return "UNKNOWN TRANSFER STATUS (" + status + ")";
        }
    }

    /**
     * Processes completed USB buffer transfers, converts the transferred bytes into complex samples and dispatches the
     * sample buffer to the listener.
     */
    public class CompletedTransferProcessor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                Transfer transfer = mCompletedTransfers.poll();

                while(transfer != null)
                {
                    if(mRunning.get())
                    {
                        ByteBuffer nativeBuffer = transfer.buffer();

                        ReusableComplexBuffer reusableComplexBuffer =
                            mNativeBufferConverter.convert(nativeBuffer, transfer.actualLength());

                        if(mComplexBufferListener != null)
                        {
                            mComplexBufferListener.receive(reusableComplexBuffer);
                        }
                    }

                    transfer.buffer().rewind();
                    mAvailableTransfers.add(transfer);

                    //Fetch the next transfer to process
                    transfer = mCompletedTransfers.poll();
                }
            }
            catch(Throwable throwable)
            {
                mLog.error("Error while processing USB transfer buffers", throwable);
            }
        }
    }
}
