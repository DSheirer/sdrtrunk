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

import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.LibUsb;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
    private ByteBuffer mStatusBuffer = ByteBuffer.allocateDirect(4);
    private AtomicBoolean mRunning = new AtomicBoolean();
    private Processor mProcessor = new Processor();
    private ScheduledFuture mProcessorFuture;

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
            //Set periodicity to an odd multiple to avoid contention with transfer buffer receivers
            mProcessorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(mProcessor, 0L, 9L, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops processing of LibUSB timeout events
     */
    private void stop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            if(mProcessorFuture != null)
            {
                mProcessorFuture.cancel(true);
                mProcessorFuture = null;
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
        @Override
        public void run()
        {
            try
            {
                int result = LibUsb.handleEventsTimeoutCompleted(null, USB_TIMEOUT_MS, mStatusBuffer.asIntBuffer());

                if(result != LibUsb.SUCCESS)
                {
                    mLog.error("Error processing timeout events for LibUSB - error code:" + result);
                }

                mStatusBuffer.rewind();
            }
            catch(Throwable throwable)
            {
                mLog.error("Error while processing LibUSB timeout events", throwable);
            }
        }
    }
}
