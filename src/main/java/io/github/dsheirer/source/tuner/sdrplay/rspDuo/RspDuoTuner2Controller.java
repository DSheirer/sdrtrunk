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

package io.github.dsheirer.source.tuner.sdrplay.rspDuo;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.RspSampleRate;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerController;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tuner controller for RSPduo tuner 2
 */
public class RspDuoTuner2Controller extends RspTunerController<IControlRspDuoTuner2>
{
    private Logger mLog = LoggerFactory.getLogger(RspDuoTuner2Controller.class);

    /**
     * Constructs an instance
     *
     * @param device that is the RSP tuner
     * @param tunerErrorListener to monitor errors produced from this tuner controller
     */
    public RspDuoTuner2Controller(IControlRspDuoTuner2 device, ITunerErrorListener tunerErrorListener)
    {
        super(device, tunerErrorListener);
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.RSP_DUO_2;
    }

    @Override
    public void apply(TunerConfiguration config) throws SourceException
    {
        if(config instanceof RspDuoTuner2Configuration rtc)
        {
            super.apply(config);

            if(!getControlRsp().isSlaveMode())
            {
                try
                {
                    getControlRsp().setAgcMode(rtc.getAgcMode());
                }
                catch(SDRPlayException se)
                {
                    mLog.error("Error setting RSP IF AGC Mode to " + rtc.getAgcMode());
                }
            }

            try
            {
                getControlRsp().setRfNotch(rtc.isRfNotch());
            }
            catch(SDRPlayException se)
            {
                mLog.error("Error setting RSPduo tuner 2 RF notch enabled to " + rtc.isRfNotch());
            }

            try
            {
                getControlRsp().setRfDabNotch(rtc.isRfDabNotch());
            }
            catch(SDRPlayException se)
            {
                mLog.error("Error setting RSPduo tuner 2 RF DAB notch enabled to " + rtc.isRfDabNotch());
            }

            try
            {
                getControlRsp().setBiasT(rtc.isBiasT());
            }
            catch(SDRPlayException se)
            {
                mLog.error("Error setting RSPduo tuner 2 Bias-T enabled to " + rtc.isBiasT());
            }

            try
            {
                getControlRsp().setExternalReferenceOutput(rtc.isExternalReferenceOutput());
            }
            catch(SDRPlayException se)
            {
                mLog.error("Error setting RSPduo tuner 2 external reference output enabled to " + rtc.isExternalReferenceOutput());
            }
        }
        else
        {
            mLog.error("Invalid RSPduo tuner configuration type: " + config.getClass());
        }
    }

    /**
     * Overrides the default RSP controller to ensure the sample rate is compatible with the
     * configuration of the RSP duo.
     * @param rspSampleRate to apply
     * @throws SDRPlayException
     */
    @Override
    public void setSampleRate(RspSampleRate rspSampleRate) throws SDRPlayException
    {
        if(getControlRsp().getDeviceSelectionMode().isSingleTunerMode())
        {
            if(rspSampleRate.isDualTunerSampleRate())
            {
                super.setSampleRate(RspTunerConfiguration.DEFAULT_SINGLE_TUNER_SAMPLE_RATE);
            }
            else
            {
                super.setSampleRate(rspSampleRate);
            }
        }
        else
        {
            if(rspSampleRate.isDualTunerSampleRate())
            {
                super.setSampleRate(rspSampleRate);
            }
            else
            {
                super.setSampleRate(RspTunerConfiguration.DEFAULT_DUAL_TUNER_SAMPLE_RATE);
            }
        }
    }
}
