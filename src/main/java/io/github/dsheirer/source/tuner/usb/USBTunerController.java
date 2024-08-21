/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
 * ****************************************************************************
 */
package io.github.dsheirer.source.tuner.usb;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.buffer.INativeBufferFactory;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.Transfer;
import org.usb4java.TransferCallback;

/**
 * Tuner controller implementation for USB tuners.  Manages general USB operations and incorporates threaded USB
 * Transfer processing.
 */
public abstract class USBTunerController extends TunerController
{
    private Logger mLog = LoggerFactory.getLogger(USBTunerController.class);
    private static final int USB_INTERFACE = 0x0;  //Common value for all currently supported devices
    private static final int USB_CONFIGURATION = 0x1;  //Common value for all currently supported devices
    private static final int USB_BULK_TRANSFER_BUFFER_POOL_SIZE = 8;
    protected static final byte USB_BULK_TRANSFER_ENDPOINT = (byte) 0x81;
    private static final long USB_BULK_TRANSFER_TIMEOUT_MS = 2000l;

    protected int mBus;
    protected String mPortAddress;
    private Context mDeviceContext = new Context();
    private Device mDevice;
    private DeviceHandle mDeviceHandle;
    private DeviceDescriptor mDeviceDescriptor;
    private TransferManager mTransferManager = new TransferManager();
    private UsbEventProcessor mEventProcessor = new UsbEventProcessor();
    private AtomicBoolean mStreaming = new AtomicBoolean();
    private boolean mRunning = false;

    //Troubleshooting libusb bug: https://github.com/DSheirer/sdrtrunk/issues/1253
    private int mAnomalousTransfersDetected = 0;

    /**
     * USB tuner controller class. Provides auto-start and auto-stop function when complex buffer listeners are added
     * or removed from this tuner controller.
     *
     * @param bus number USB
     * @param portAddress address USB
     * @param tunerErrorListener to receive errors from this tuner controller
     */
    public USBTunerController(int bus, String portAddress, ITunerErrorListener tunerErrorListener)
    {
        super(tunerErrorListener);
        mBus = bus;
        mPortAddress = portAddress;
    }

    /**
     * Constructs an instance
     * @param bus usb
     * @param portAddress usb
     * @param minimum tunable frequency in Hertz
     * @param maximum tunable frequency in Hertz
     * @param halfBandwidth that is unusable for DC spike avoidance
     * @param usablePercent bandwith in Hertz
     * @param tunerErrorListener to receive errors from this tuner controller
     */
    public USBTunerController(int bus, String portAddress, long minimum, long maximum, int halfBandwidth, double usablePercent,
                              ITunerErrorListener tunerErrorListener)
    {
        this(bus, portAddress, tunerErrorListener);
        setMinimumFrequency(minimum);
        setMaximumFrequency(maximum);
        setMiddleUnusableHalfBandwidth(halfBandwidth);
        setUsableBandwidthPercentage(usablePercent);
    }

    /**
     * Tuner type for this USB controller
     */
    public abstract TunerType getTunerType();

    /**
     * Factory for converting received streaming sample data into native buffers, as provided by sub-class.
     */
    protected abstract INativeBufferFactory getNativeBufferFactory();

    /**
     * Sub-class definition of transfer buffer sizes to use for the tuner.  Note: this should be a power-of-two
     * value for compatibility with downstream (SIMD) operations (e.g. 65,536, 131072, 262144, etc.)
     * @return transfer buffer size.
     */
    protected abstract int getTransferBufferSize();

    /**
     * Sub-class method to perform additional device setup steps after the USB interface has been claimed and before any
     * transfer operations start.
     * @throws SourceException if there is an issue in configuring the device
     */
    protected abstract void deviceStart() throws SourceException;

    /**
     * Sub-class method to perform additional device shutdown steps after transfer processing has stopped and before
     * the USB interface is released.
     */
    protected abstract void deviceStop();

    /**
     * Starts or initializes this tuner.
     *
     * Note: sub-class implementations should override and invoke this method and then perform additional initialization
     * operations for the tuner.
     *
     * @throws SourceException if there is an error and/or this tuner is unusable.
     */
    public final void start() throws SourceException
    {
        if(mDeviceContext == null)
        {
            throw new SourceException("Device cannot be reused once it has been shutdown");
        }

        int status = LibUsb.init(mDeviceContext);

        if(status != LibUsb.SUCCESS)
        {
            throw new SourceException("Can't initialize libusb library - " + LibUsb.errorName(status));
        }

        mDevice = findDevice();

        if(mDevice == null)
        {
            throw new SourceException("Couldn't find USB device at bus [" + mBus + "] port [" + mPortAddress + "]");
        }

        mDeviceDescriptor = new DeviceDescriptor();
        status = LibUsb.getDeviceDescriptor(mDevice, mDeviceDescriptor);

        if(status != LibUsb.SUCCESS)
        {
            mDeviceDescriptor = null;
            throw new SourceException("Can't obtain tuner's device descriptor - " + LibUsb.errorName(status));
        }

        mDeviceHandle = new DeviceHandle();
        status = LibUsb.open(mDevice, mDeviceHandle);

        //Now that we have opened the device and added an additional reference, remove the original reference placed on
        // the device during the findDevice() operation
        LibUsb.unrefDevice(mDevice);

        if(status == LibUsb.ERROR_ACCESS)
        {
            mDeviceHandle = null;
            mDeviceDescriptor = null;

            mLog.error("Access to USB tuner denied - (windows) reinstall zadig driver or (linux) blacklist driver and/or check udev rules");
            throw new SourceException("access denied - if using linux, blacklist the default driver and/or install udev rules");
        }
        else if(status != LibUsb.SUCCESS)
        {
            mDeviceHandle = null;
            mDeviceDescriptor = null;

            mLog.error("Can't open USB tuner - check driver or Linux udev rules");
            throw new SourceException("Can't open USB tuner - reinstall driver? - " + LibUsb.errorName(status));
        }

        //Detach the kernel driver if active and detach is supported.  Otherwise, let the claim interface fail.
        status = LibUsb.kernelDriverActive(mDeviceHandle, USB_INTERFACE);

        if(status == 1) //kernel driver is attached and detach operation is supported
        {
            status = LibUsb.detachKernelDriver(mDeviceHandle, USB_INTERFACE);

            if(status != LibUsb.SUCCESS)
            {
                mLog.error("Unable to detach kernel driver for USB tuner device - bus:" + mBus + " port:" + mPortAddress);
                mDeviceHandle = null;
                mDeviceDescriptor = null;
                throw new SourceException("Can't detach kernel driver");
            }
        }

        //Set the configuration which also invokes a soft reset on the device
        status = LibUsb.setConfiguration(mDeviceHandle, USB_CONFIGURATION);

        if(status == LibUsb.ERROR_BUSY)
        {
            mLog.error("Unable to set USB configuration on tuner - device is busy (in use by another application)");
            mDeviceHandle = null;
            mDeviceDescriptor = null;
            throw new SourceException("USB tuner is in-use by another application");
        }
        else if(status != LibUsb.SUCCESS)
        {
            mDeviceHandle = null;
            mDeviceDescriptor = null;
            throw new SourceException("Can't set configuration (ie reset) on the USB tuner - " + LibUsb.errorName(status));
        }

        //Claim the interface
        status = LibUsb.claimInterface(mDeviceHandle, USB_INTERFACE);

        if(status == LibUsb.ERROR_BUSY)
        {
            mDeviceHandle = null;
            mDeviceDescriptor = null;
            throw new SourceException("USB tuner is in-use by another application");
        }
        else if(status != LibUsb.SUCCESS)
        {
            mDeviceHandle = null;
            mDeviceDescriptor = null;
            throw new SourceException("Can't claim interface on USB tuner - " + LibUsb.errorName(status));
        }

        //Set running true for deviceStart() operations that require it.
        mRunning = true;

        try
        {
            deviceStart();
        }
        catch(Exception se)
        {
            mRunning = false;
            throw se;
        }
    }

    /**
     * Prepares the tuner for full shutdown by stopping streaming, shutdown the device, and releasing the USB resources.
     */
    public final void stop()
    {
        mRunning = false;

        //Spin the shutdown onto a new thread so that we can set a max wait threshold.
        Thread t = new Thread(() -> {
            stopStreaming();
            mNativeBufferBroadcaster.clear();
            deviceStop();
        });

        t.start();

        try
        {
            //Wait up to 500 milliseconds for the shutdown to complete ... otherwise interrupt it and finish shutdown
            t.join(500);

            if(t.isAlive())
            {
                mLog.info("Tuner shutdown exceeded 500ms - forcing shutdown");
                t.interrupt();
            }
        }
        catch(InterruptedException ie)
        {
        }

        //Release transfers
        mTransferManager.freeTransfers();

        if(mDeviceHandle != null)
        {
            LibUsb.releaseInterface(mDeviceHandle, USB_INTERFACE);
            LibUsb.close(mDeviceHandle);
            mDeviceHandle = null;
            mDevice = null;
            mDeviceDescriptor = null;
        }

        LibUsb.exit(mDeviceContext);
        mDeviceContext = null;
    }

    /**
     * Starts streaming data from the tuner
     */
    private void startStreaming()
    {
        if(mStreaming.compareAndSet(false, true))
        {
            try
            {
                prepareStreaming();
                List<Transfer> transfers = mTransferManager.getTransfers();
                mEventProcessor.start();
                mTransferManager.setAutoResubmitTransfers(true);
                mTransferManager.submitTransfers(transfers);
            }
            catch(SourceException se)
            {
                mLog.error("Error starting streaming on USB tuner", se);
            }
        }
    }

    /**
     * Prepares to start streaming.  This method can be overridden by sub-class to implement additional actions
     * need to prepare before start streaming.
     */
    protected void prepareStreaming()
    {
    }

    /**
     * Stop streaming data from the tuner
     */
    private void stopStreaming()
    {
        if(mStreaming.compareAndSet(true, false))
        {
            //Turn off auto-resubmit of USB transfer buffers
            mTransferManager.setAutoResubmitTransfers(false);

            //Stop event processing thread to put all submitted tranfers in a stable state - blocks until stopped
            mEventProcessor.stop();

            //Cancel all currently submitted transfers
            mTransferManager.cancelTransfers();

            //Perform final event processing iteration so LibUsb returns all of our cancelled tranfers
            mEventProcessor.handleFinalEvents();

            streamingCleanup();
        }
    }

    /**
     * Post streaming cleanup actions.  This method can be overridden by sub-class to implement additional actions
     * needed to cleanup after streaming stops.
     */
    protected void streamingCleanup()
    {
    }

    /**
     * Finds the USB device for this tuner at the specified USB bus and port.
     * @return discovered USB device
     * @throws SourceException if there is an error or the device is not discovered.
     */
    private Device findDevice() throws SourceException
    {
        Device foundDevice = null;

        DeviceList deviceList = new DeviceList();
        int count = LibUsb.getDeviceList(mDeviceContext, deviceList);

        if(count >= 0)
        {
            for(Device device: deviceList)
            {
                int bus = LibUsb.getBusNumber(device);
                int port = LibUsb.getPortNumber(device);

                if(port > 0)
                {
                    String portAddress = TunerManager.getPortAddress(device);

                    if(mBus == bus && mPortAddress != null && mPortAddress.equals(portAddress))
                    {
                        foundDevice = device;
                    }
                    else
                    {
                        LibUsb.unrefDevice(device);
                    }
                }
                else
                {
                    LibUsb.unrefDevice(device);
                }
            }
        }

        //Free the device list but don't auto-unref all the devices ... we already did that during iteration
        LibUsb.freeDeviceList(deviceList, false);

        if(foundDevice != null)
        {
            return foundDevice;
        }

        throw new SourceException("LibUsb couldn't discover USB device [" + mBus + ":" + mPortAddress +
                "] from device list" + (count < 0 ? " - error: " + LibUsb.errorName(count) : ""));
    }

    /**
     * Access the discovered USB device.
     */
    protected Device getDevice()
    {
        return mDevice;
    }

    /**
     * LibUsb context for this device.
     */
    protected Context getDeviceContext()
    {
        return mDeviceContext;
    }

    /**
     * LibUsb device descriptor for this device
     */
    protected DeviceDescriptor getDeviceDescriptor()
    {
        return mDeviceDescriptor;
    }

    /**
     * USB Device Handle for the claimed device
     */
    protected DeviceHandle getDeviceHandle()
    {
        return mDeviceHandle;
    }

    /**
     * Indicates if the device handle is non-null
     */
    protected boolean hasDeviceHandle()
    {
        return getDeviceHandle() != null;
    }

    /**
     * Indicates if this device is usable, meaning it has been started and is not yet stopping.
     *
     * Note: this is a general usability flag for controlling all code that touches the USB interface(s)
     */
    protected boolean isRunning()
    {
        return mRunning;
    }

    /**
     * Adds the IQ buffer listener and automatically starts stream buffer transfer processing, if not already started.
     */
    @Override
    public void addBufferListener(Listener<INativeBuffer> listener)
    {
        if(isRunning())
        {
            getLock().lock();

            try
            {
                boolean hasExistingListeners = hasBufferListeners();

                super.addBufferListener(listener);

                if(!hasExistingListeners)
                {
                    startStreaming();
                }
            }
            finally
            {
                getLock().unlock();
            }
        }
    }

    /**
     * Removes the IQ buffer listener and stops stream buffer transfer processing if there are no more listeners.
     */
    @Override
    public void removeBufferListener(Listener<INativeBuffer> listener)
    {
        getLock().lock();

        try
        {
            super.removeBufferListener(listener);

            if(!hasBufferListeners())
            {
                stopStreaming();
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Manages USB transfer (ie zero-copy) buffer processing
     */
    class TransferManager implements TransferCallback
    {
        private List<Transfer> mAvailableTransfers;
        private LinkedTransferQueue<Transfer> mInProgressTransfers = new LinkedTransferQueue<>();
        private boolean mAutoResubmitTransfers = false;
        private int mTransferErrorCount = 0;
        private List<Transfer> mErrorTransfers = new ArrayList<>();
        private int mResubmitFailureLogCount = 0;

        /**
         * Creates USB Transfers to carry the streaming sample data.  Transfer buffers are backed by native memory
         * byte buffers outside the JVM.
         *
         * @return list of transfers
         * @throws SourceException if there is an error creating transfers
         */
        private List<Transfer> getTransfers() throws SourceException
        {
            if(mAvailableTransfers == null)
            {
                mAvailableTransfers = new ArrayList<>();

                for(int x = 0; x < USB_BULK_TRANSFER_BUFFER_POOL_SIZE; x++)
                {
                    Transfer transfer = LibUsb.allocTransfer();

                    if(transfer == null)
                    {
                        throw new SourceException("Couldn't allocate USB transfer buffer - out of memory");
                    }

                    final ByteBuffer buffer = ByteBuffer.allocateDirect(getTransferBufferSize());

                    LibUsb.fillBulkTransfer(transfer, mDeviceHandle, USB_BULK_TRANSFER_ENDPOINT, buffer,
                            TransferManager.this, "Transfer Buffer " + x, USB_BULK_TRANSFER_TIMEOUT_MS);

                    mAvailableTransfers.add(transfer);
                }
            }

            return mAvailableTransfers;
        }

        /**
         * Prepare to stop processing transfers when stopping streaming of data.
         */
        private void setAutoResubmitTransfers(boolean resubmit)
        {
            mAutoResubmitTransfers = resubmit;
        }

        /**
         * Submits the transfers to start sample stream processing
         * @param transfers to submit
         */
        private void submitTransfers(List<Transfer> transfers)
        {
            for(Transfer transfer: transfers)
            {
                submitTransfer(transfer);
            }
        }

        /**
         * (Re)Submits the transfer for stream processing
         *
         * Note: synchronized used here because there can be multiple threads can invoke LibUsb.handleTimeoutEvents
         * (scheduled thread pool and a dedicated shutdown thread) during tuner shutdown and this has caused transfer
         * tracking issues.
         *
         * @param transfer to (re)submit
         */
        private synchronized void submitTransfer(Transfer transfer)
        {
            int status = LibUsb.submitTransfer(transfer);

            if(status == LibUsb.SUCCESS)
            {
                mInProgressTransfers.add(transfer);

                //Attempt to resubmit any previous transfers that failed on submit
                if(!mErrorTransfers.isEmpty())
                {
                    Transfer toResubmit = mErrorTransfers.remove(0);
                    int resubmitStatus = LibUsb.submitTransfer(toResubmit);

                    if(resubmitStatus == LibUsb.SUCCESS)
                    {
                        mInProgressTransfers.add(transfer);

                        //Only log this if more than half of the total transfer buffers are in error-holding
                        if(mErrorTransfers.size() >= (mAvailableTransfers.size() / 2))
                        {
                            mLog.info("Successfully resubmitted previous error USB transfer buffer.  Current transfer buffer" +
                                    " status (error queue/total available) [" + mErrorTransfers.size() + "/" +
                                    mAvailableTransfers.size() + "]");
                        }
                    }
                    else if(resubmitStatus == LibUsb.ERROR_BUSY)
                    {
                        //Ignore - this indicates the transfer was previously submitted and libusb is still working it.
                    }
                    else
                    {
                        //Add it back to the queue to try again later.
                        mErrorTransfers.add(transfer);
                        mTransferErrorCount++;
                    }
                }
            }
            else if(status == LibUsb.ERROR_BUSY)
            {
                //Ignore - this indicates the transfer was previously submitted and libusb is still working it.  I'm not
                //sure how this happens because we give libusb a transfer and it hands it back when it's full.  If
                //libusb is still working it, then why did it indicate the transfer was completed?  So, we simply
                //ignore this error code.  Other libraries simply ignore the submit status code altogether.
            }
            else
            {
                mLog.error("USB transfer [" + transfer + "] submit attempt failed with error [" + LibUsb.errorName(status) +
                        "] - adding to error queue to resubmit later - this may be a temporary USB issue and has happened [" +
                        mTransferErrorCount + "] time(s) so far.  Current transfer error queue (error/total) [" +
                        mErrorTransfers.size() + "/" + mAvailableTransfers.size() + "]");

                mErrorTransfers.add(transfer);
                mTransferErrorCount++;
            }

            if(mErrorTransfers.size() >= mAvailableTransfers.size())
            {
                mLog.error("Maximum USB transfer buffer errors reached - transfer buffers exhausted - shutting down USB tuner");
                ThreadPool.CACHED.submit(() -> setErrorMessage("USB Error - Transfer Buffers Exhausted"));
            }
        }

        /**
         * Cancels any in-progress transfers to prepare for shutdown.
         *
         * Note: this should only be invoked after the LibUsb event processing thread has been stopped so that the
         * transfer buffers are in a stable (submitted vs callback) state and we can then flip their cancel state and
         * then finish processing the timeout events under the control of a single (shutdown) thread.
         */
        private void cancelTransfers()
        {
            for(Transfer transfer: mInProgressTransfers)
            {
                LibUsb.cancelTransfer(transfer);
            }
            for(Transfer transfer: mErrorTransfers)
            {
                LibUsb.cancelTransfer(transfer);
            }
        }

        /**
         * Frees/disposes allocated USB transfer buffers.
         */
        private void freeTransfers()
        {
            if(mAvailableTransfers != null)
            {
                for(Transfer transfer: mAvailableTransfers)
                {
                    try
                    {
                        LibUsb.freeTransfer(transfer);
                    }
                    catch(Exception e)
                    {
                        mLog.error("Error releasing allocated USB transfer buffer during tuner shutdown: " +
                                e.getLocalizedMessage());
                    }
                }

                mAvailableTransfers.clear();
                mAvailableTransfers = null;
            }
        }

        @Override
        public void processTransfer(Transfer transfer)
        {
            mInProgressTransfers.remove(transfer);

            if(mErrorTransfers.contains(transfer))
            {
                mLog.warn("USB transfer [" + transfer + "] that was being tracked as an error transfer, has just been " +
                        "delivered as completed with transfer status [" + LibUsb.errorName(transfer.status()) +
                        "] - removing it from the transfer error queue");
                mErrorTransfers.remove(transfer);
            }

            switch(transfer.status())
            {
                case LibUsb.TRANSFER_COMPLETED:
                case LibUsb.TRANSFER_STALL:
                case LibUsb.TRANSFER_TIMED_OUT:
                case LibUsb.TRANSFER_ERROR:
                //Note: cancel flag can be set by libusb, independent of commanded cancel of transfers - we simply
                //resubmit the transfer for continued use.
                case LibUsb.TRANSFER_CANCELLED:
                    int transferLength = transfer.actualLength();

                    if(transferLength > 0)
                    {
                        dispatchTransfer(transfer);
                    }

                    transfer.buffer().rewind();

                    if(mAutoResubmitTransfers)
                    {
                        submitTransfer(transfer);
                    }
                    break;
                default:
                    //Unexpected transfer error - shutdown the tuner
                    transfer.buffer().rewind();

                    //Only set an error if we're not shutting down
                    if(mAutoResubmitTransfers)
                    {
                        //spin this off onto the thread pool, so it doesn't impact the usb processor thread.
                        ThreadPool.CACHED.submit(() -> setErrorMessage("LibUsb Transfer Error - stopping device - " +
                                "status [" + transfer.status() + "] - " + LibUsb.errorName(transfer.status())));
                    }
                    break;
            }
        }

        /**
         * Makes a copy of the transfer's native memory byte array payload so that the transfer can be reused.
         * Dispatches the native buffer to registered listeners.
         * @param transfer to copy and dispatch
         */
        private void dispatchTransfer(Transfer transfer)
        {
            //Pass the transfer's byte buffer so the native buffer factory can make a copy of the byte array contents
            //and package it as a native buffer.
            INativeBuffer nativeBuffer = getNativeBufferFactory().getBuffer(transfer.buffer(), System.currentTimeMillis());
            mNativeBufferBroadcaster.broadcast(nativeBuffer);
        }
    }

    /**
     * Threaded LibUsb event processor - continuously polls LibUsb to process events exclusively for this USB tuner
     * device using the device context.
     */
    class UsbEventProcessor implements Runnable
    {
        private Thread mThread;
        private boolean mProcessing = false;

        /**
         * Start the event processing thread
         */
        public void start()
        {
            if(mThread == null)
            {
                mProcessing = true;
                mThread = new Thread(this);
                mThread.setName("sdrtrunk USB tuner - bus [" + mBus + "] port [" + mPortAddress + "]");
                mThread.setPriority(Thread.MAX_PRIORITY);
                mThread.start();
            }
        }

        /**
         * Set the stop processing flag and block until the thread stops, blocking up to 1000 ms.
         */
        public void stop()
        {
            mProcessing = false;

            try
            {
                //Give the thread a second to stop - it should happen quickly because it's only checking transfers
                //for completed status and returning them to us to dispatch.
                mThread.join(1000);
            }
            catch(Exception e)
            {
                mLog.error("Error stopping LibUsb event processing thread - " + e.getLocalizedMessage());
            }

            mThread = null;
        }

        /**
         * This performs a final handle-events invocation after the event processing thread has been shutdown and
         * transfers have been flagged as cancelled.  This should cause LibUsb to return all in-progress and now
         * canceled transfers back to us via the TransferManager.processTransfer() method.
         */
        public void handleFinalEvents()
        {
            try
            {
                //Use a short timeout since this is a shutdown operation
                LibUsb.handleEventsTimeout(mDeviceContext, 50);
            }
            catch(Throwable throwable)
            {
                mLog.error("Error while processing stop-streaming LibUsb timeout events", throwable);
            }
        }

        /**
         * LibUsb event/timeout processing loop
         */
        @Override
        public void run()
        {
            mProcessing = true;

            while(mProcessing)
            {
                try
                {
                    LibUsb.handleEventsTimeout(mDeviceContext, 250);
                }
                catch(Throwable throwable)
                {
                    mLog.error("Error while processing LibUsb timeout events", throwable);
                }
            }
        }
    }
}
