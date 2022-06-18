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

package io.github.dsherer.sdrplay.test.listener;

import com.github.dsheirer.sdrplay.SDRPlayException;
import com.github.dsheirer.sdrplay.callback.IDeviceEventListener;
import com.github.dsheirer.sdrplay.device.Device;
import com.github.dsheirer.sdrplay.device.TunerSelect;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Streaming samples consumer that can initialize a device, process streaming data for 5 seconds, and un-initialize
 * the device.
 */
public class LoggingStreamConsumer
{
    private static final Logger mLog = LoggerFactory.getLogger(LoggingStreamConsumer.class);
    private Device mDevice;
    private LoggingStreamListener mStreamAListener;
    private LoggingStreamListener mStreamBListener;
    private IDeviceEventListener mDeviceEventListener;

    public LoggingStreamConsumer(Device device, TunerSelect tunerA, TunerSelect tunerB)
    {
        mDevice = device;
        mDeviceEventListener = new LoggingDeviceEventListener(device.getDeviceType().toString(), device);
        mStreamAListener = new LoggingStreamListener("Stream A", tunerA);
        mStreamBListener = new LoggingStreamListener("Stream B", tunerB);
    }

    public LoggingStreamConsumer(Device device)
    {
        this(device, TunerSelect.TUNER_1, TunerSelect.TUNER_2);
    }

    /**
     * Initializes the device, consumes samples for the specified period and then uninitializes the device.
     * @param durationSeconds duration of the test in seconds.
     * @throws SDRPlayException
     */
    public void process(int durationSeconds) throws SDRPlayException
    {
        mLog.info("Initializing device ...");
        mDevice.initStreamA(mDeviceEventListener, mStreamAListener);

        final long startTimestamp = System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.schedule(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    mLog.info("Un-initializing the device");
                    mDevice.uninitialize();

                    mStreamAListener.logSampleRate(System.currentTimeMillis() - startTimestamp);
                    mStreamBListener.logSampleRate(System.currentTimeMillis() - startTimestamp);

                    mLog.info("releasing device");
                    mDevice.release();
                }
                catch(SDRPlayException se)
                {
                    mLog.error("Error releasing RSPduo device", se);
                }

                latch.countDown();
            }
        }, durationSeconds, TimeUnit.SECONDS);

        try
        {
            latch.await(durationSeconds + 10, TimeUnit.SECONDS);
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Logs the spectral (FFT) data captured from stream A.
     */
    public void logSpectrumA()
    {
        mStreamAListener.logSpectrum();
    }

    /**
     * Logs the spectral (FFT) data captured from stream B.
     */
    public void logSpectrumB()
    {
        mStreamBListener.logSpectrum();
    }
}
