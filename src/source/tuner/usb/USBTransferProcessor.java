/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package source.tuner.usb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;
import org.usb4java.Transfer;
import org.usb4java.TransferCallback;
import sample.Broadcaster;
import sample.Listener;
import sample.OverflowableTransferQueue;
import sample.adapter.ISampleAdapter;
import sample.complex.ComplexBuffer;
import source.tuner.TunerManager;
import util.ThreadPool;

import javax.usb.UsbException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
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
    private static final int TRANSFER_BUFFER_POOL_SIZE = 16;

    //Maximum number of filled buffers for the blocking queue
    private static final int FILLED_BUFFER_MAX_CAPACITY = 300;

    //Threshold for resetting buffer overflow condition
    private static final int FILLED_BUFFER_OVERFLOW_RESET_THRESHOLD = 200;

    private String mDeviceName;

    //Handle to the USB bulk transfer device
    private DeviceHandle mDeviceHandle;

    //Tuner format-specific byte to IQ float sample converter
    private ISampleAdapter mSampleAdapter;

    //Byte array transfer buffers size in bytes
    private int mBufferSize;

    private Broadcaster<ComplexBuffer> mComplexBufferBroadcaster = new Broadcaster<>();
    private OverflowableTransferQueue<byte[]> mFilledBuffers;
    private LinkedTransferQueue<Transfer> mAvailableTransfers = new LinkedTransferQueue<>();
    private LinkedTransferQueue<Transfer> mTransfersInProgress = new LinkedTransferQueue<>();

    private AtomicBoolean mRunning = new AtomicBoolean();

    private ByteBuffer mLibUsbHandlerStatus = ByteBuffer.allocateDirect(4);

    private BufferDispatcher mBufferDispatcher = new BufferDispatcher();
    private ScheduledFuture mBufferDispatcherFuture;

    /**
     * Manages stream of USB transfer buffers and converts buffers to complex buffer samples for distribution to
     * any registered listeners.
     *
     * @param deviceName to use when logging information or errors
     * @param deviceHandle to the USB bulk transfer device
     * @param sampleAdapter specific to the tuner's byte buffer format for converting to floating point I/Q samples
     * @param bufferSize in bytes.  Should be a multiple of two: 65536, 131072 or 262144.
     */
    public USBTransferProcessor(String deviceName,
                                DeviceHandle deviceHandle,
                                ISampleAdapter sampleAdapter,
                                int bufferSize)
    {
        mDeviceName = deviceName;
        mDeviceHandle = deviceHandle;
        mSampleAdapter = sampleAdapter;
        mBufferSize = bufferSize;

        mFilledBuffers = new OverflowableTransferQueue<>(FILLED_BUFFER_MAX_CAPACITY, FILLED_BUFFER_OVERFLOW_RESET_THRESHOLD);
        mFilledBuffers.setStateListener(new Listener<OverflowableTransferQueue.State>()
        {
            @Override
            public void receive(OverflowableTransferQueue.State state)
            {
                if(state == OverflowableTransferQueue.State.NORMAL)
                {
                    mLog.debug(mDeviceName + " - buffer overflow - temporary pause until processing catches up");
                }
                else
                {
                    mLog.debug(mDeviceName + " - buffer overflow cleared - resuming normal processing");
                }
            }
        });
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

            while(!mAvailableTransfers.isEmpty())
            {
                Transfer transfer = mAvailableTransfers.poll();

                if(transfer != null)
                {
                    int result = LibUsb.submitTransfer(transfer);

                    if(result == LibUsb.SUCCESS)
                    {
                        mTransfersInProgress.add(transfer);
                    }
                    else if(result == LibUsb.ERROR_PIPE)
                    {
                        int resetResult = LibUsb.clearHalt(mDeviceHandle, USB_BULK_TRANSFER_ENDPOINT);

                        if(resetResult == LibUsb.SUCCESS)
                        {
                            int resubmitResult = LibUsb.submitTransfer(transfer);

                            if(resubmitResult == LibUsb.SUCCESS)
                            {
                                mTransfersInProgress.add(transfer);
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

            //Start transferred buffer dispatcher
            mBufferDispatcherFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(mBufferDispatcher,
                0, 11, TimeUnit.MILLISECONDS);

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

            mFilledBuffers.clear();

            //Cancel the lib usb process timer
            for(Transfer transfer : mTransfersInProgress)
            {
                LibUsb.cancelTransfer(transfer);
            }


            //Unregister from LibUSB processor so that it auto-stops LibUSB processing
            TunerManager.LIBUSB_TRANSFER_PROCESSOR.registerTransferProcessor(this);

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
     * Indicates if there are complex buffer listeners registered with this processor
     */
    public boolean hasListeners()
    {
        return mComplexBufferBroadcaster.hasListeners();
    }

    public void addListener(Listener<ComplexBuffer> listener)
    {
        mComplexBufferBroadcaster.addListener(listener);

        start();
    }

    public void removeListener(Listener<ComplexBuffer> listener)
    {
        mComplexBufferBroadcaster.removeListener(listener);

        if(!hasListeners())
        {
            stop();
        }
    }

    public void removeAllListeners()
    {
        mComplexBufferBroadcaster.clear();
        stop();
    }

    /**
     * Prepares (allocates) a set of transfer buffers for use in transferring data from the USB device via the bulk
     * interface.  Since we're using direct allocation (native), buffers are retained and reused across multiple
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

            LibUsb.fillBulkTransfer(transfer, mDeviceHandle, USB_BULK_TRANSFER_ENDPOINT, buffer, this,
                "Buffer", USB_TIMEOUT_MS);

            mAvailableTransfers.add(transfer);
        }
    }

    /**
     * Process a filled transfer buffer received back from the USB device
     */
    @Override
    public void processTransfer(Transfer transfer)
    {
        mTransfersInProgress.remove(transfer);

        switch(transfer.status())
        {
            case LibUsb.TRANSFER_COMPLETED:
            case LibUsb.TRANSFER_STALL:
            case LibUsb.TRANSFER_TIMED_OUT:
                if(transfer.actualLength() > 0)
                {
                    ByteBuffer buffer = transfer.buffer();

                    byte[] data = new byte[transfer.actualLength()];
                    buffer.get(data);
                    buffer.rewind();

                    if(mRunning.get())
                    {
                        mFilledBuffers.offer(data);
                    }
                }
                break;
            case LibUsb.TRANSFER_CANCELLED:
                break;
            default:
                //Unexpected transfer error
                mLog.error(mDeviceName + " - transfer error [" + getTransferStatus(transfer.status()) +
                    "] transferred actual: " + transfer.actualLength());
        }


        if(mRunning.get())
        {
            int result = LibUsb.submitTransfer(transfer);

            if(result == LibUsb.SUCCESS)
            {
                mTransfersInProgress.add(transfer);
            }
            else if(result == LibUsb.ERROR_PIPE)
            {
                int resetResult = LibUsb.clearHalt(mDeviceHandle, USB_BULK_TRANSFER_ENDPOINT);

                if(resetResult == LibUsb.SUCCESS)
                {
                    int resubmitResult = LibUsb.submitTransfer(transfer);

                    if(resubmitResult == LibUsb.SUCCESS)
                    {
                        mTransfersInProgress.add(transfer);
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
                mAvailableTransfers.add(transfer);
                mLog.error(mDeviceName + " - error submitting transfer [" + LibUsb.errorName(result) + "]");

                if(mTransfersInProgress.isEmpty())
                {
                    mLog.warn(mDeviceName + " - all transfer buffer processing is stopped");
                    //TODO: no transfers are in progress ... need to alert the user and the registered complex
                    //TODO: buffer listeners so that they can respond accordingly
                }
            }
        }
        else
        {
            //We're stopping - park the transfer buffers
            if(!mAvailableTransfers.contains(transfer))
            {
                mAvailableTransfers.add(transfer);
            }
        }
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
     * Fetches byte[] chunks from the raw sample buffer.  Converts each byte
     * array and broadcasts the array to all registered listeners
     */
    public class BufferDispatcher implements Runnable
    {
        private List<byte[]> mBuffersToDispatch = new ArrayList<>();

        @Override
        public void run()
        {
            try
            {
                mFilledBuffers.drainTo(mBuffersToDispatch, 15);

                for(byte[] buffer : mBuffersToDispatch)
                {
                    float[] complexSamples = mSampleAdapter.convert(buffer);

                    mComplexBufferBroadcaster.broadcast(new ComplexBuffer(complexSamples));
                }
            }
            catch(Exception e)
            {
                mLog.error(mDeviceName + " - error while dispatching complex IQ buffer samples", e);
            }

            mBuffersToDispatch.clear();
        }
    }
}
