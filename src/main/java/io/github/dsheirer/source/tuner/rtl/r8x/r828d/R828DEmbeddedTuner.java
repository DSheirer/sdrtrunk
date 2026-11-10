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
package io.github.dsheirer.source.tuner.rtl.r8x.r828d;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;
import io.github.dsheirer.source.tuner.rtl.r8x.R8xEmbeddedTuner;

import javax.usb.UsbException;

/**
 * Rafael Micro R828D Embedded Tuner implementation
 */
public class R828DEmbeddedTuner extends R8xEmbeddedTuner
{
    private static final byte I2C_WRITE_ADDRESS = (byte) 0x74;
    private static final byte I2C_READ_ADDRESS = (byte) 0x75;
    private static final int VCO_POWER_REF = 1;
    /**
     * RTL-SDR.com blog V4 dongle indicator.
     */
    private final boolean mIsV4Dongle;

    /**
     * Constructs an instance
     * @param adapter for accessing RTL2832USBController interfaces
     */
    public R828DEmbeddedTuner(RTL2832TunerController.ControllerAdapter adapter)
    {
        super(adapter, VCO_POWER_REF);

        //Set flag to indicate if this is a V4 dongle that supports notch filtering.
        mIsV4Dongle = adapter.isV4Dongle();
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.RAFAELMICRO_R828D;
    }

    @Override
    public byte getI2CWriteAddress()
    {
        return I2C_WRITE_ADDRESS;
    }

    @Override
    public byte getI2CReadAddress()
    {
        return I2C_READ_ADDRESS;
    }

    /**
     * Sets the center frequency.  Setting the frequency is a two-part process
     * of setting the multiplexer and then setting the Oscillator (PLL).
     */
    @Override
    public synchronized void setTunedFrequency(long frequency) throws SourceException
    {
        getAdapter().getLock().lock();

        try
        {
            getAdapter().enableI2CRepeater();
            boolean controlI2C = false;
            long offsetFrequency = frequency + IF_FREQUENCY;
            setMux(offsetFrequency, controlI2C);
            setPLL(offsetFrequency, controlI2C);

            //Select the RF input
            if(mIsV4Dongle)
            {
                //Disable notch filtering if frequency falls into a notched band, otherwise enable it.
                boolean isNotchFilterFrequency = (frequency < 2_200_000) ||
                        (85_000_000 < frequency && frequency < 112_000_000) ||
                        (172_000_000 < frequency && frequency < 242_000_000);
                writeRegister(Register.DRAIN, isNotchFilterFrequency ? (byte)0x00 : (byte)0x08, controlI2C);

                if(frequency <= 28_800_000) //Use cable 2 input
                {
                    writeRegister(Register.INPUT_SELECTOR_AIR, (byte)0x20, controlI2C); //disabled
                    writeRegister(Register.INPUT_SELECTOR_CABLE_1, (byte)0x00, controlI2C); //disabled
                    writeRegister(Register.INPUT_SELECTOR_CABLE_2, (byte)0x08, controlI2C); //enabled
                }
                else if(frequency < 250_000_000)
                {
                    writeRegister(Register.INPUT_SELECTOR_AIR, (byte)0x20, controlI2C); //disabled
                    writeRegister(Register.INPUT_SELECTOR_CABLE_1, (byte)0x40, controlI2C); //enabled
                    writeRegister(Register.INPUT_SELECTOR_CABLE_2, (byte)0x00, controlI2C); //disabled
                }
                else
                {
                    writeRegister(Register.INPUT_SELECTOR_AIR, (byte)0x00, controlI2C); //enabled
                    writeRegister(Register.INPUT_SELECTOR_CABLE_1, (byte)0x00, controlI2C); //disabled
                    writeRegister(Register.INPUT_SELECTOR_CABLE_2, (byte)0x00, controlI2C); //disabled
                }
            }
            else
            {
                byte air_cable_in = (frequency > 345_000_000) ? (byte)0x00 : (byte)0x60;
                writeRegister(Register.INPUT_SELECTOR_AIR_AND_CABLE_1, air_cable_in, controlI2C);
            }

            getAdapter().disableI2CRepeater();
        }
        catch(UsbException e)
        {
            throw new SourceException("R828DTunerController - exception while setting frequency [" + frequency + "] - " +
                    e.getLocalizedMessage());
        }
        finally
        {
            getAdapter().getLock().unlock();
        }
    }
}