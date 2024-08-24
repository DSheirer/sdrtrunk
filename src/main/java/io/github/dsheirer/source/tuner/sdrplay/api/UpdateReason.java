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

package io.github.dsheirer.source.tuner.sdrplay.api;

import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_h;
import java.util.EnumSet;

/**
 * Update reason.
 *
 * These entries are used to notify the SDRplay API once settings are updated on the device.
 */
public enum UpdateReason
{
    //Master Only mode
    DEVICE_SAMPLE_RATE(sdrplay_api_h.sdrplay_api_Update_Dev_Fs(), "Sample Rate"),
    DEVICE_PPM(sdrplay_api_h.sdrplay_api_Update_Dev_Ppm(), "PPM"),
    DEVICE_SYNC_UPDATE(sdrplay_api_h.sdrplay_api_Update_Dev_SyncUpdate(), "Sync Update"),
    DEVICE_RESET_FLAGS(sdrplay_api_h.sdrplay_api_Update_Dev_ResetFlags(), "Reset Flags"),

    RSP1A_BIAS_T_CONTROL(sdrplay_api_h.sdrplay_api_Update_Rsp1a_BiasTControl(), "RSP1A Bias-T Control"),
    RSP1A_RF_NOTCH_CONTROL(sdrplay_api_h.sdrplay_api_Update_Rsp1a_RfNotchControl(), "RSP1A RF Notch Control"),
    RSP1A_RF_DAB_NOTCH_CONTROL(sdrplay_api_h.sdrplay_api_Update_Rsp1a_RfDabNotchControl(), "RSP1A RF DAB Notch Control"),

    RSP2_BIAS_T_CONTROL(sdrplay_api_h.sdrplay_api_Update_Rsp2_BiasTControl(), "RSP2 Bias-T Control"),
    RSP2_AM_PORT_SELECT(sdrplay_api_h.sdrplay_api_Update_Rsp2_AmPortSelect(), "RSP2 AM Port Select"),
    RSP2_ANTENNA_CONTROL(sdrplay_api_h.sdrplay_api_Update_Rsp2_AntennaControl(), "RSP2 Antenna Control"),
    RSP2_RF_NOTCH_CONTROL(sdrplay_api_h.sdrplay_api_Update_Rsp2_RfNotchControl(), "RSP2 RF Notch Control"),
    RSP2_EXT_REF_CONTROL(sdrplay_api_h.sdrplay_api_Update_Rsp2_ExtRefControl(), "RSP2 External Reference Control"),

    RSP_DUO_EXT_REF_CONTROL(sdrplay_api_h.sdrplay_api_Update_RspDuo_ExtRefControl(), "RSPduo External Reference Control"),

    SPARE_1(sdrplay_api_h.sdrplay_api_Update_Master_Spare_1(), "Spare 1"),
    SPARE_2(sdrplay_api_h.sdrplay_api_Update_Master_Spare_2(), "Spare 2"),

    //Master and Slave mode
    TUNER_GAIN_REDUCTION(sdrplay_api_h.sdrplay_api_Update_Tuner_Gr(), "Tuner Gain Reduction"),
    TUNER_GAIN_REDUCTION_LIMITS(sdrplay_api_h.sdrplay_api_Update_Tuner_GrLimits(), "Tuner Gain Reduction Limits"),
    TUNER_FREQUENCY_RF(sdrplay_api_h.sdrplay_api_Update_Tuner_Frf(), "Tuner Frequency RF"),
    TUNER_BANDWIDTH_TYPE(sdrplay_api_h.sdrplay_api_Update_Tuner_BwType(), "Tuner Bandwidth Type"),
    TUNER_IF_TYPE(sdrplay_api_h.sdrplay_api_Update_Tuner_IfType(), "Tuner IF Type"),
    TUNER_DC_OFFSET(sdrplay_api_h.sdrplay_api_Update_Tuner_DcOffset(), "Tuner DC Offset"),
    TUNER_LO_MODE(sdrplay_api_h.sdrplay_api_Update_Tuner_LoMode(), "Tuner LO Mode"),

    CONTROL_DC_OFFSET_IQ_IMBALANCE(sdrplay_api_h.sdrplay_api_Update_Ctrl_DCoffsetIQimbalance(), "Control DC Offset IQ Imbalance"),
    CONTROL_DECIMATION(sdrplay_api_h.sdrplay_api_Update_Ctrl_Decimation(), "Control Decimation"),
    CONTROL_AGC(sdrplay_api_h.sdrplay_api_Update_Ctrl_Agc(), "Control AGC"),
    CONTROL_ADSB_MODE(sdrplay_api_h.sdrplay_api_Update_Ctrl_AdsbMode(), "Control ADSB Mode"),
    CONTROL_OVERLOAD_MESSAGE_ACK(sdrplay_api_h.sdrplay_api_Update_Ctrl_OverloadMsgAck(), "Control Overload Message Ack"),

    RSP_DUO_BIAS_T_CONTROL(sdrplay_api_h.sdrplay_api_Update_RspDuo_BiasTControl(), "RSPduo Bias-T Control"),
    RSP_DUO_AM_PORT_SELECT(sdrplay_api_h.sdrplay_api_Update_RspDuo_AmPortSelect(), "RSPduo AM Port Select"),
    RSP_DUO_TUNER_1_AM_NOTCH_CONTROL(sdrplay_api_h.sdrplay_api_Update_RspDuo_Tuner1AmNotchControl(), "RSPduo Tuner 1 AM Notch Control"),
    RSP_DUO_RF_NOTCH_CONTROL(sdrplay_api_h.sdrplay_api_Update_RspDuo_RfNotchControl(), "RSPduo RF Notch Control"),
    RSP_DUO_RF_DAB_NOTCH_CONTROL(sdrplay_api_h.sdrplay_api_Update_RspDuo_RfDabNotchControl(), "RSPduo RF DAB Notch Control"),

    //Extension 1
    EXTENSION_RSP_DX_HDR_ENABLE(sdrplay_api_h.sdrplay_api_Update_RspDx_HdrEnable(), "RSPdx HDR Enable"),
    EXTENSION_RSP_DX_BIAS_T_CONTROL(sdrplay_api_h.sdrplay_api_Update_RspDx_BiasTControl(), "RSPdx Bias-T Control"),
    EXTENSION_RSP_DX_ANTENNA_CONTROL(sdrplay_api_h.sdrplay_api_Update_RspDx_AntennaControl(), "RSPdx Antenna Control"),
    EXTENSION_RSP_DX_RF_NOTCH_CONTROL(sdrplay_api_h.sdrplay_api_Update_RspDx_RfNotchControl(), "RSPdx RF Notch Control"),
    EXTENSION_RSP_DX_RF_DAB_NOTCH_CONTROL(sdrplay_api_h.sdrplay_api_Update_RspDx_RfDabNotchControl(), "RSPdx RF DAB Notch Control"),
    EXTENSION_RSP_DX_HDR_BANDWIDTH(sdrplay_api_h.sdrplay_api_Update_RspDx_HdrBw(), "RSPdx HDR Bandwidth"),
    EXTENSION_RSP_DUO_RESET_SLAVE_FLAGS(io.github.dsheirer.source.tuner.sdrplay.api.v3_15.sdrplay_api_h.sdrplay_api_Update_RspDuo_ResetSlaveFlags(), "RSPDuo Reset Slave Flags"),
    EXTENSION_NONE(sdrplay_api_h.sdrplay_api_Update_Ext1_None(), "NONE"),


    NONE(sdrplay_api_h.sdrplay_api_Update_None(), "NONE");

    private int mValue;
    private String mDescription;

    UpdateReason(int value, String description)
    {
        mValue = value;
        mDescription = description;
    }

    public static EnumSet<UpdateReason> EXTENSIONS = EnumSet.range(EXTENSION_RSP_DX_HDR_ENABLE, EXTENSION_NONE);


    /**
     * Many of the update reasons described in this enumeration are submitted asynchronously, since the API is
     * non-blocking.  However, the device event listener responses are limited to just three events: Frequency, Gain,
     * and Sample Rate.  This constant describes the three update reasons that correspond to the device events.
     */
    public static EnumSet<UpdateReason> ASYNC_UPDATE_RESPONSES = EnumSet.of(DEVICE_SAMPLE_RATE, TUNER_FREQUENCY_RF, TUNER_GAIN_REDUCTION);

    /**
     * Numeric value
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Indicates if this is an extended update reason entry.
     */
    public boolean isExtended()
    {
        return EXTENSIONS.contains(this);
    }

    /**
     * Indicates if this update reason is one of the limited set of asynchronous update operation response types.
     */
    public boolean isAsyncUpdateResponse()
    {
        return ASYNC_UPDATE_RESPONSES.contains(this);
    }

    /**
     * Lookup the entry from a return code
     * @param value to lookup
     * @return entry or UNKNOWN if the code is not recognized
     */
    public static UpdateReason fromValue(int value)
    {
        for(UpdateReason status: UpdateReason.values())
        {
            if(status.getValue() == value)
            {
                return status;
            }
        }
        
        return NONE;
    }

    /**
     * Calculates a combined logical OR value of the non-extended update reasons.  Extended reason values will
     * be ignored for this calculation.
     *
     * @param reasons to combine
     * @return combined (logical OR) value.
     */
    public static int getReasons(UpdateReason ... reasons)
    {
        int combined = UpdateReason.NONE.getValue();

        for(UpdateReason reason: reasons)
        {
            if(!reason.isExtended())
            {
                combined += reason.getValue();
            }
        }

        return combined;
    }

    /**
     * Calculates a combined logical OR value of the extended update reasons.  Non-extended reason values will
     * be ignored for this calculation.
     *
     * @param reasons to combine
     * @return combined (logical OR) value.
     */
    public static int getExtendedReasons(UpdateReason ... reasons)
    {
        int combined = UpdateReason.EXTENSION_NONE.getValue();

        for(UpdateReason reason: reasons)
        {
            if(reason.isExtended())
            {
                combined += reason.getValue();
            }
        }

        return combined;
    }

    @Override
    public String toString()
    {
        return mDescription;
    }
}
