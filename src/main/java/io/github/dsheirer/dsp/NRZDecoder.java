/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.dsp;

import io.github.dsheirer.bits.IBinarySymbolProcessor;
import io.github.dsheirer.sample.Listener;

public class NRZDecoder implements IBinarySymbolProcessor
{
    public final static boolean MODE_NORMAL = true;
    public final static boolean MODE_INVERTED = false;

    private IBinarySymbolProcessor mListener;
    private boolean mMode = MODE_NORMAL;
    private boolean mPrevious;

    /**
     * Non Return To Zero (NRZ) and NRZ Inverted (NRZ-I) Decoder.
     *
     * Use default constructor for NRZ output.  Use the alternate constructor
     * with the MODE_INVERTED argument for NRZ-I output.
     *
     * Performs XOR of incoming bit with previous output and sends the result
     * to the registered listener.
     */
    public NRZDecoder(boolean mode)
    {
        mMode = mode;
    }

    public NRZDecoder()
    {
        this(MODE_NORMAL);
    }

    public void dispose()
    {
        mListener = null;
    }

    @Override
    public void process(boolean symbol)
    {
        boolean result = mPrevious ^ symbol;

        if(mListener != null)
        {
            mListener.process(mMode ? result : !result);
        }

        mPrevious = result;
    }

    public void setListener(IBinarySymbolProcessor listener)
    {
        mListener = listener;
    }

    public void removeListener(Listener<Boolean> listener)
    {
        mListener = null;
    }
}
