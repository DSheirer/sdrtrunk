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

import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit testing for an RSPduo device
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class RspDuoTest
{
    private static Logger mLog = LoggerFactory.getLogger(RspDuoTest.class);

//    /**
//     * Tests an RSPduo configured for single tuner 1.
//     */
//    @Test
//    @DisplayName("Test an RSPduo device using single tuner mode with tuner 1")
//    public void testSingleTuner1()
//    {
//        testDuo(DeviceSelectionMode.SINGLE_TUNER_1);
//    }
//
//    /**
//     * Tests an RSPduo configured for single tuner 2.
//     */
//    @Test
//    @DisplayName("Test an RSPduo device using single tuner mode with tuner 2")
//    public void testSingleTuner2()
//    {
//        testDuo(DeviceSelectionMode.SINGLE_TUNER_2);
//    }
//
//    /**
//     * Tests an RSPduo configured for master with tuner 1.
//     */
//    @Test
//    @DisplayName("Test an RSPduo device using master tuner mode with tuner 1")
//    public void testMasterTuner1()
//    {
//        testDuo(DeviceSelectionMode.MASTER_TUNER_1);
//    }
//
//    /**
//     * Tests the RSPduo with the specified device selection mode
//     * @param deviceSelectionMode
//     */
//    private void testDuo(DeviceSelectionMode deviceSelectionMode)
//    {
//        SDRplay api = new SDRplay();
//
//        DeviceInfo deviceInfo = api.getDeviceInfo(DeviceType.RSPduo);
//
//        if(deviceInfo != null)
//        {
//            mLog.info("Testing: " + deviceInfo + " With Device Selection Mode: " + deviceSelectionMode);
//
//            if(deviceInfo.getDeviceSelectionModes().contains(deviceSelectionMode))
//            {
//                try
//                {
//                    Device device = api.select(deviceInfo, deviceSelectionMode);
//
//                    if(device instanceof RspDuoDevice)
//                    {
//                        mLog.info("Selected: " + device.getClass());
//
//                        if(deviceSelectionMode.isMasterMode())
//                        {
//                            mLog.info("Setting IF Mode");
//                            device.getTuner().setIfMode(IfMode.IF_2048);
//
//                            mLog.info("Setting Sample Rate");
////                            device.setSampleRate(SampleRate.DUO_RATE_0_500);
//                        }
//                        else
//                        {
//                            mLog.info("Setting Sample Rate");
////                            device.setSampleRate(SampleRate.RATE_10_000);
//                        }
//
//                        mLog.info("Setting Frequencies");
//                        device.getTuner().setFrequency(460_450_000);
//
//                        mLog.info("Gain Reduction: " + device.getTuner().getGainReduction());
//                        mLog.info("AGC Mode: " + device.getTuner().getAGC());
//                        mLog.info("Gain: " + device.getTuner().getGain());
//                        mLog.info("LO Mode:" + device.getTuner().getLoMode());
//                        mLog.info("IF Mode:" + device.getTuner().getIfMode());
//
//                        mLog.info("Capturing Samples ...");
//                        LoggingStreamConsumer loggingStreamConsumer = new LoggingStreamConsumer(device);
//                        loggingStreamConsumer.process(5);
//                        loggingStreamConsumer.logSpectrumA();
//                    }
//                    else
//                    {
//                        mLog.error("Unrecognized Device Type: " + device.getClass());
//                    }
//
//                    device.release();
//                    mLog.info("Released");
//                }
//                catch(SDRPlayException se)
//                {
//                    mLog.error("Error testing device", se);
//                }
//            }
//        }
//        else
//        {
//            mLog.info("Unable to obtain RSPduo device to test");
//        }
//
//        try
//        {
//            api.close();
//        }
//        catch(Exception se)
//        {
//            mLog.error("Error closing api");
//        }
//    }
}
