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
package io.github.dsheirer.sample.decimator;

import io.github.dsheirer.sample.complex.ComplexSampleListener;

/**
 * Decimator for complex samples
 */
public class ComplexDecimator implements ComplexSampleListener
{
    private ComplexSampleListener mListener;
    private int mCounter = 0;
    private int mDecimationRate;

    /**
     * Constructs a new Decimator object with the specified decimation rate.
     */
    public ComplexDecimator(int rate)
    {
        mDecimationRate = rate;
    }

    public void dispose()
    {
        mListener = null;
    }

    /**
     * Sets a new decimation rate.  Rate can be changed after this object
     * is constructed.
     */
    public synchronized void setDecimationRate(int rate)
    {
        mDecimationRate = rate;
    }

    /**
     * Receives samples allowing only 1 of every (rate) sample to go on
     * to the registered listener
     */
    @Override
    public void receive(float i, float q)
    {
        mCounter++;

        if(mCounter >= mDecimationRate)
        {
            mListener.receive(i, q);

            mCounter = 0;
        }
    }

    /**
     * Sets the decimated output listener
     */
    public void setListener(ComplexSampleListener listener)
    {
        mListener = listener;
    }
}
