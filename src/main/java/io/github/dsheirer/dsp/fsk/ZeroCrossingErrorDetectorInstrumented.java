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
package io.github.dsheirer.dsp.fsk;

import javafx.beans.property.SimpleFloatProperty;

public class ZeroCrossingErrorDetectorInstrumented extends ZeroCrossingErrorDetector
{
    public SimpleFloatProperty timingError = new SimpleFloatProperty();

    /**
     * Constructs the detector for the specified samples per symbol.
     *
     * @param samplesPerSymbol
     */
    public ZeroCrossingErrorDetectorInstrumented(float samplesPerSymbol)
    {
        super(samplesPerSymbol);
    }

    @Override
    public float getError()
    {
        float error = super.getError();

        timingError.setValue(error);

        return error;
    }

    public float getZeroCrossingIdeal()
    {
        return mZeroCrossingIdeal;
    }

    public float getDetectedZeroCrossing()
    {
        return mDetectedZeroCrossing;
    }

    public boolean[] getBuffer()
    {
        return mBuffer;
    }
}
