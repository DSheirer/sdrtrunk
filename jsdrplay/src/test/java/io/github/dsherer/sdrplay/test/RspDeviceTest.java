/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsherer.sdrplay.test;

import com.github.dsheirer.sdrplay.DeviceSelectionMode;
import com.github.dsheirer.sdrplay.SDRPlayException;
import com.github.dsheirer.sdrplay.SDRplay;
import com.github.dsheirer.sdrplay.device.Device;
import com.github.dsheirer.sdrplay.device.DeviceInfo;
import com.github.dsheirer.sdrplay.device.DeviceType;
import io.github.dsherer.sdrplay.test.listener.LoggingStreamConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit testing for an RSP1A device
 */
public class RspDeviceTest
{
    public static final Logger mLog = LoggerFactory.getLogger(RspDeviceTest.class);

    @Test
    @DisplayName("List available RSP devices")
    public void listDevices()
    {
        SDRplay api = new SDRplay();

        try
        {
            for(DeviceInfo deviceInfo: api.getDeviceInfos())
            {
                mLog.info(deviceInfo.toString());
            }

            api.close();
        }
        catch(Exception se)
        {
            mLog.error("Error closing API", se);
        }
    }

    /**
     * Tests an RSP1A.
     */
    @Test
    @DisplayName("Test an RSP1A device")
    public void testRSP1A()
    {
        testDevice(DeviceType.RSP1A, DeviceSelectionMode.SINGLE_TUNER_1);
    }

    /**
     * Tests an RSP1.
     */
    @Test
    @DisplayName("Test an RSP1 device")
    public void testRSP1()
    {
        testDevice(DeviceType.RSP1, DeviceSelectionMode.SINGLE_TUNER_1);
    }

    /**
     * Tests the RSPduo with the specified device selection mode
     * @param deviceSelectionMode
     */
    private void testDevice(DeviceType deviceType, DeviceSelectionMode deviceSelectionMode)
    {
        SDRplay api = new SDRplay();
        mLog.info("Version: " + api.getVersion());
        try
        {
            DeviceInfo deviceInfo = api.getDeviceInfo(deviceType);
            mLog.info("Testing: " + deviceInfo + " With Device Selection Mode: " + deviceSelectionMode);
            Device device = api.getDevice(deviceInfo);
            device.select();
            mLog.info("Device: " + device.toString());

            mLog.info("Selected: " + device.getClass());

            mLog.info("Setting Sample Rate");
//                    device.setSampleRate(SampleRate.RATE_0_250);

            mLog.info("Setting Frequencies");
            device.getTuner().setFrequency(460_450_000);

            mLog.info("Gain Reduction: " + device.getTuner().getGainReduction());
            mLog.info("AGC Mode: " + device.getTuner().getAGC());
            mLog.info("Gain: " + device.getTuner().getGain());
            mLog.info("LO Mode:" + device.getTuner().getLoMode());
            mLog.info("IF Mode:" + device.getTuner().getIfMode());

            mLog.info("Capturing Samples ...");
            LoggingStreamConsumer loggingStreamConsumer = new LoggingStreamConsumer(device);
            loggingStreamConsumer.process(5);
//                    loggingStreamConsumer.logSpectrum1();

            device.release();
            mLog.info("Released");
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error testing device", se);
        }

        try
        {
            api.close();
        }
        catch(Exception se)
        {
            mLog.error("Error closing api");
        }
    }

}
