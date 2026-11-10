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
package io.github.dsheirer.source.tuner.rtl.r8x.r820t;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;
import io.github.dsheirer.source.tuner.rtl.r8x.R8xEmbeddedTuner;

import javax.usb.UsbException;

/**
 * Rafael Micro R820T and R820T2 Embedded Tuner implementation
 */
public class R820TEmbeddedTuner extends R8xEmbeddedTuner
{
    private static final byte I2C_WRITE_ADDRESS = (byte) 0x34;
    private static final byte I2C_READ_ADDRESS = (byte) 0x35;
    private static final int VCO_POWER_REF = 2;

    /**
     * Constructs an instance
     * @param adapter for accessing RTL2832USBController interfaces
     */
    public R820TEmbeddedTuner(RTL2832TunerController.ControllerAdapter adapter)
    {
        super(adapter, VCO_POWER_REF);
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.RAFAELMICRO_R820T;
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
            getAdapter().disableI2CRepeater();
        }
        catch(UsbException e)
        {
            throw new SourceException("R820TTunerController - exception while setting frequency [" + frequency + "] - " +
                    e.getLocalizedMessage());
        }
        finally
        {
            getAdapter().getLock().unlock();
        }
    }
}