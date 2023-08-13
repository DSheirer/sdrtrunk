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

package io.github.dsheirer.module.decode.dmr.channel;

import java.util.List;

/**
 * DMR Logical Slot Number (LSN) channel.
 *
 * Note: LSN 1-16 are represented in memory using the following channel and timeslot values:
 *
 * LSN: CHANNEL/TIMESLOT
 *  1:  1/1
 *  2:  1/2
 *  3:  2/1
 *  4:  2/2
 *  5:  3/1
 *  6:  3/2
 *  7:  4/1
 *  8:  4/2
 *  9:  5/1
 * 10:  5/2
 * 11:  6/1
 * 12:  6/2
 * 13:  7/1
 * 14:  7/2
 * 15:  8/1
 * 16:  8/2
 */
public class DMRLsn extends DMRChannel implements ITimeslotFrequencyReceiver
{
    private TimeslotFrequency mTimeslotFrequency;

    /**
     * Constructs an instance
     *
     * @param lsn in range 1 - 16
     */
    public DMRLsn(int lsn)
    {
        super(((lsn - 1) / 2) + 1, ((lsn - 1) % 2) + 1);
    }

    @Override
    public String toString()
    {
        return "LSN:" + getLsn();
    }

    /**
     * Logical Slot Number
     * @return lsn
     */
    public int getLsn()
    {
        return ((getChannel() - 1) * 2) + getTimeslot();
    }

    @Override
    public long getDownlinkFrequency()
    {
        if(mTimeslotFrequency != null)
        {
            return mTimeslotFrequency.getDownlinkFrequency();
        }

        return 0;
    }

    @Override
    public long getUplinkFrequency()
    {
        if(mTimeslotFrequency != null)
        {
            return mTimeslotFrequency.getUplinkFrequency();
        }

        return 0;
    }

    @Override
    public int[] getLogicalSlotNumbers()
    {
        return new int[]{getLsn()};
    }

    /**
     * Sets the lsn to frequency mapper value.
     * @param timeslotFrequency to set
     */
    public void setTimeslotFrequency(TimeslotFrequency timeslotFrequency)
    {
        mTimeslotFrequency = timeslotFrequency;
    }

    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        for(TimeslotFrequency timeslotFrequency: timeslotFrequencies)
        {
            if(timeslotFrequency.getNumber() == getLsn())
            {
                setTimeslotFrequency(timeslotFrequency);
                return;
            }
        }
    }

    public static void main(String[] args)
    {
        for(int x = 1; x <= 16; x++)
        {
            DMRLsn lsn = new DmrRestLsn(x);
            System.out.println(lsn);
        }
    }
}
