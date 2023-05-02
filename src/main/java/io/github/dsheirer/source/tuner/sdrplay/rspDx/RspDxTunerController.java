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

package io.github.dsheirer.source.tuner.sdrplay.rspDx;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerController;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tuner controller for RSPdx
 */
public class RspDxTunerController extends RspTunerController<IControlRspDx>
{
    private Logger mLog = LoggerFactory.getLogger(RspDxTunerController.class);

    /**
     * Constructs an instance
     *
     * @param control interface for the RSPdx tuner
     * @param tunerErrorListener to monitor errors produced from this tuner controller
     */
    public RspDxTunerController(IControlRspDx control, ITunerErrorListener tunerErrorListener)
    {
        super(control, tunerErrorListener);
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.RSP_DX;
    }

    @Override
    public void apply(TunerConfiguration config) throws SourceException
    {
        if(config instanceof RspDxTunerConfiguration rtc)
        {
            super.apply(config);

            try
            {
                getControlRsp().setAgcMode(rtc.getAgcMode());
            }
            catch(SDRPlayException se)
            {
                mLog.error("Error setting RSP IF AGC Mode to " + rtc.getAgcMode());
            }

            try
            {
                getControlRsp().setAntenna(rtc.getAntenna());
            }
            catch(SDRPlayException se)
            {
                mLog.error("Error setting RSPdx antenna to " + rtc.getAntenna());
            }

            try
            {
                getControlRsp().setBiasT(rtc.isBiasT());
            }
            catch(SDRPlayException se)
            {
                mLog.error("Error setting RSPdx bias-T enabled to " + rtc.isBiasT());
            }

            try
            {
                getControlRsp().setHighDynamicRange(rtc.isHdrMode());
            }
            catch(SDRPlayException se)
            {
                mLog.error("Error setting RSPdx high dynamic range mode enabled to " + rtc.isHdrMode());
            }

            try
            {
                getControlRsp().setRfNotch(rtc.isRfNotch());
            }
            catch(SDRPlayException se)
            {
                mLog.error("Error setting RSPdx RF Notch enabled to " + rtc.isRfNotch());
            }

            try
            {
                getControlRsp().setRfDabNotch(rtc.isRfDabNotch());
            }
            catch(SDRPlayException se)
            {
                mLog.error("Error setting RSPdx RF DAB Notch enabled to " + rtc.isRfDabNotch());
            }

            try
            {
                getControlRsp().setHdrModeBandwidth(rtc.getHdrModeBandwidth());
            }
            catch(SDRPlayException se)
            {
                mLog.error("Error setting RSPdx HDR mode bandwdidth to " + rtc.getHdrModeBandwidth());
            }
        }
        else
        {
            mLog.error("Invalid RSPdx tuner configuration type: " + config.getClass());
        }
    }
}
