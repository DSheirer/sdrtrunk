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
package io.github.dsheirer.dsp.psk;

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;

public class QPSKStarSlicer implements IQPSKSymbolDecoder, Listener<Complex>
{
    private Broadcaster<Dibit> mBroadcaster = new Broadcaster<Dibit>();

    /**
     * Slices a Complex sample representing a phase shifted symbol according to the following constellation pattern:
     *
     * 00 +3 | 01 +1
     * ------|------
     * 10 -1 | 11 -1
     */
    public QPSKStarSlicer()
    {
    }

    public void dispose()
    {
        mBroadcaster.dispose();
        mBroadcaster = null;
    }

    public void addListener(Listener<Dibit> listener)
    {
        mBroadcaster.addListener(listener);
    }

    public void removeListener(Listener<Dibit> listener)
    {
        mBroadcaster.removeListener(listener);
    }

    @Override
    public void receive(Complex sample)
    {
        mBroadcaster.broadcast(decode(sample));
    }

    @Override
    public Dibit decode(Complex sample)
    {
        if(sample.inphase() > 0)
        {
            return sample.quadrature() > 0 ? Dibit.D00_PLUS_1 : Dibit.D10_MINUS_1;
        }
        else
        {
            return sample.quadrature() > 0 ? Dibit.D01_PLUS_3 : Dibit.D11_MINUS_3;
        }
    }

}
