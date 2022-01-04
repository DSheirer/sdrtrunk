/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.LibUsb;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduled runnable for the primary LibUsb processing thread.  Each USB device's transfer processor
 * registers with this processor to support auto-start/stop of this event processing thread.
 */
public class USBMasterProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(USBMasterProcessor.class);

    private static final long USB_TIMEOUT_MS = 2000l; //milliseconds

    private List<USBTransferProcessor> mRegisteredProcessors = new CopyOnWriteArrayList<>();
    private AtomicBoolean mRunning = new AtomicBoolean();
    private Processor mProcessor = new Processor();
    private Thread mProcessorThread;

    /**
     * Registers the transfer processor so that LibUSB timeout processing will auto-start.
     * @param processor to register
     */
    public void registerTransferProcessor(USBTransferProcessor processor)
    {
        if(!mRegisteredProcessors.contains(processor))
        {
            mRegisteredProcessors.add(processor);
        }

        start();
    }

    /**
     * Unregisters the transfer processor so that LibUSB timeout processing will auto-stop once all processors have
     * been unregistered.
     *
     * @param processor to unregister
     */
    public void unregisterTransferProcessor(USBTransferProcessor processor)
    {
        mRegisteredProcessors.remove(processor);

        if(mRegisteredProcessors.isEmpty())
        {
            stop();
        }
    }

    /**
     * Starts processing of LibUSB timeout events
     */
    private void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            mLog.info("Starting USB master processor thread");
            if(mProcessorThread == null)
            {
                mProcessor.reset();
                mProcessorThread = new Thread(mProcessor);
                mProcessorThread.setName("sdrtrunk USB event processor");
                mProcessorThread.setPriority(Thread.MAX_PRIORITY);
                mProcessorThread.start();
            }
        }
    }

    /**
     * Stops processing of LibUSB timeout events
     */
    private void stop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            mLog.info("Stopping USB master processor thread");
            if(mProcessorThread != null)
            {
                mProcessor.stop();
                mProcessorThread.interrupt();

                try
                {
                    mProcessorThread.join(1000);
                }
                catch(InterruptedException ie)
                {
                    //No action
                }

                mProcessorThread = null;
            }
        }
    }

    /**
     * Stops the libusb timeout processor and prepares for shutdown.
     */
    public void shutdown()
    {
        stop();
    }

    /**
     * Runnable to invoke LibUSB timeout handler.  All downstream completed transfer processing will occur on this
     * thread instance.
     */
    class Processor implements Runnable
    {
        private boolean mRunning = true;

        /**
         * Sets stop flag to signal to stop processing
         */
        public void stop()
        {
            mRunning = false;
        }

        /**
         * Resets this processor so that it can be reused.
         */
        public void reset()
        {
            mRunning = true;
        }

        @Override
        public void run()
        {
            while(mRunning)
            {
                try
                {
                    LibUsb.handleEvents(null);
                }
                catch(Throwable throwable)
                {
                    mLog.error("Error while processing LibUSB timeout events", throwable);
                }
            }
        }
    }
}
