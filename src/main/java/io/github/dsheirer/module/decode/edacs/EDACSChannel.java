/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.edacs;

import io.github.dsheirer.channel.IBandChannelDescriptor;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.protocol.Protocol;
import java.util.Objects;

/**
 * A logical channel number (LCN) and its frequency from the configured LCN list.
 */
public class EDACSChannel implements IBandChannelDescriptor
{
    private final int mLCN;
    private final long mFrequency;

    public EDACSChannel(int lcn, long frequency)
    {
        mLCN = lcn;
        mFrequency = frequency;
    }

    public int getLCN()
    {
        return mLCN;
    }

    @Override
    public long getDownlinkFrequency()
    {
        return mFrequency;
    }

    @Override
    public long getUplinkFrequency()
    {
        return 0;
    }

    @Override
    public int[] getFrequencyBandIdentifiers()
    {
        return new int[0];
    }

    @Override
    public void setFrequencyBand(IFrequencyBand bandIdentifier)
    {
        //no-op: the LCN list gives the frequency
    }

    @Override
    public boolean isTDMAChannel()
    {
        return false;
    }

    @Override
    public int getTimeslotCount()
    {
        return 1;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.EDACS;
    }

    @Override
    public String toString()
    {
        return "LCN " + mLCN + (mFrequency > 0 ? " (" + String.format("%.4f", mFrequency / 1e6) + ")" : "");
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof EDACSChannel other && other.mLCN == mLCN && other.mFrequency == mFrequency;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mLCN, mFrequency);
    }
}
