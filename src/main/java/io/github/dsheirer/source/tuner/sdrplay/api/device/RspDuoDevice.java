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

package io.github.dsheirer.source.tuner.sdrplay.api.device;

import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRplay;
import io.github.dsheirer.source.tuner.sdrplay.api.UpdateReason;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.composite.RspDuoCompositeParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.IfMode;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.LoMode;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_DeviceT;

/**
 * RSPduo Device
 */
public class RspDuoDevice extends Device<RspDuoCompositeParameters, RspDuoTuner>
{
    private RspDuoTuner mTuner;

    /**
     * Constructs an SDRPlay RSPduo device from the foreign memory segment
     *
     * @param sdrPlay api instance that created this device
     * @param deviceStruct parser
     */
    RspDuoDevice(SDRplay sdrPlay, IDeviceStruct deviceStruct)
    {
        super(sdrPlay, deviceStruct);
    }

    /**
     * Tuner
     * @return tuner
     * @throws SDRPlayException if there is an error
     */
    @Override
    public RspDuoTuner getTuner() throws SDRPlayException
    {
        if(!isSelected())
        {
            throw new SDRPlayException("Device must be selected before accessing the tuner");
        }

        if(mTuner == null)
        {
            if(getTunerSelect().equals(TunerSelect.TUNER_1))
            {
                mTuner = new RspDuoTuner1(RspDuoDevice.this, getAPI(),
                        getCompositeParameters().getDeviceParameters(), getCompositeParameters().getTunerAParameters(),
                        getCompositeParameters().getControlAParameters());
            }
            else
            {
                mTuner = new RspDuoTuner2(RspDuoDevice.this, getAPI(),
                        getCompositeParameters().getDeviceParameters(), getCompositeParameters().getTunerBParameters(),
                        getCompositeParameters().getControlBParameters());
            }
        }

        return mTuner;
    }

    /**
     * Selected tuner(s).
     */
    @Override
    public TunerSelect getTunerSelect()
    {
        return TunerSelect.fromValue(sdrplay_api_DeviceT.tuner(getDeviceMemorySegment()));
    }

    /**
     * Sets the selected tuner(s)
     */
    public void setTunerSelect(TunerSelect tunerSelect)
    {
        sdrplay_api_DeviceT.tuner(getDeviceMemorySegment(), tunerSelect.getValue());
    }

    /**
     * RSPduo mode
     */
    public RspDuoMode getRspDuoMode()
    {
        return getDeviceStruct().getRspDuoMode();
    }

    /**
     * Sets RSPduo mode.  Note this must be set before selecting the device for use.
     * @param mode to set
     * @throws SDRPlayException if the device has already been selected
     */
    public void setRspDuoMode(RspDuoMode mode) throws SDRPlayException
    {
        if(isSelected())
        {
            throw new SDRPlayException("RSPduo device is already selected.  Mode can only be set/changed before the " +
                    "device is selected for use.");
        }

        getDeviceStruct().setRspDuoMode(mode);
    }

    /**
     * Sample rate when in master/slave mode
     */
    public double getRspDuoSampleFrequency()
    {
        return getDeviceStruct().getRspDuoSampleFrequency();
    }

    /**
     * Sets the sample rate when the device is configured for master mode.
     * @param frequency
     */
    public void setRspDuoSampleFrequency(double frequency) throws SDRPlayException
    {
        if(!getRspDuoMode().equals(RspDuoMode.MASTER))
        {
            throw new SDRPlayException("This method can only be used to set the overall sample rate when the RSPduo " +
                    "device is configured for master mode.");
        }

        getDeviceStruct().setRspDuoSampleFrequency(frequency);
    }

    /**
     * Sets the decimation factor for the final sample rate.
     * @param decimation as an integer multiple of two (e.g. 1, 2, 4, 8)
     * @throws SDRPlayException if there is an error while setting decimation
     */
    @Override
    public void setDecimation(Decimate decimation) throws SDRPlayException
    {
        if(getTunerSelect() == TunerSelect.TUNER_1)
        {
            getCompositeParameters().getControlAParameters().getDecimation().setDecimationFactor(decimation.getValue());
            getCompositeParameters().getControlAParameters().getDecimation().setEnabled(decimation.isEnabled());
            update(getTunerSelect(), UpdateReason.CONTROL_DECIMATION);
        }
        else if(getTunerSelect() == TunerSelect.TUNER_2)
        {
            getCompositeParameters().getControlBParameters().getDecimation().setDecimationFactor(decimation.getValue());
            getCompositeParameters().getControlBParameters().getDecimation().setEnabled(decimation.isEnabled());
            update(getTunerSelect(), UpdateReason.CONTROL_DECIMATION);
        }
        else if(getTunerSelect() == TunerSelect.TUNER_BOTH)
        {
            //Dual-synchronized tuner mode ... let the parent Device class set the value
            super.setDecimation(decimation);
        }
    }

    /**
     * Sets the IF mode when tuner 2 is selected in single-tuner mode.
     * @param ifMode to set
     */
    public void setIfModeTuner2(IfMode ifMode)
    {
        getCompositeParameters().getTunerBParameters().setIfMode(ifMode);
    }

    /**
     * Sets the LO mode when tuner 2 is selected in single-tuner mode.
     * @param loMode to set
     */
    public void setLoModeTuner2(LoMode loMode)
    {
        getCompositeParameters().getTunerBParameters().setLoMode(loMode);
    }


    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SDRPplay Device").append("\n");
        sb.append("\tType: " + getDeviceType()).append("\n");
        sb.append("\tSerial Number: " + getSerialNumber()).append("\n");
        sb.append("\tTuner: " + getTunerSelect()).append("\n");
        sb.append("\tRSP Duo: " + getRspDuoMode()).append("\n");
        sb.append("\tRSP Duo Sample Rate: " + getRspDuoSampleFrequency()).append("\n");
        sb.append("\tSelected: " + isSelected());
        if(hasCompositeParameters())
        {
            sb.append("\t").append(getCompositeParameters());
        }

        return sb.toString();
    }
}
