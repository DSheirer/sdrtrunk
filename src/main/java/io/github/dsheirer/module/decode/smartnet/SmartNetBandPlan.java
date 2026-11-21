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

package io.github.dsheirer.module.decode.smartnet;

/**
 * Motorola Type II 800 MHz channel numbers to receive frequencies.  Mapping follows
 * trunk-recorder's SmartnetParser.get_freq() (GPL).  Massachusetts State Police uses the rebanded plan
 * (857.0375 MHz = channel 241).
 */
public enum SmartNetBandPlan
{
    BAND_800_REBANDED("800 MHz rebanded"),
    BAND_800_STANDARD("800 MHz standard (pre-rebanding)"),
    BAND_800_SPLINTER("800 MHz splinter");

    private final String mLabel;

    SmartNetBandPlan(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Receive (outbound) frequency in Hz for a channel number, or 0 when the command is not a channel.
     */
    public long getFrequency(int channel)
    {
        double mhz = 0;

        switch(this)
        {
            case BAND_800_REBANDED ->
            {
                if(channel >= 0 && channel <= 0x1B7)
                {
                    mhz = 851.0125 + 0.025 * channel;
                }
                else if(channel >= 0x1B8 && channel <= 0x22F)
                {
                    mhz = 851.0250 + 0.025 * (channel - 0x1B8);
                }
            }
            case BAND_800_SPLINTER ->
            {
                if(channel >= 0 && channel <= 0x257)
                {
                    mhz = 851.0000 + 0.025 * channel;
                }
                else if(channel >= 0x258 && channel <= 0x2CF)
                {
                    mhz = 866.0125 + 0.025 * (channel - 0x258);
                }
            }
            case BAND_800_STANDARD ->
            {
                if(channel >= 0 && channel <= 0x2CF)
                {
                    mhz = 851.0125 + 0.025 * channel;
                }
            }
        }

        if(channel >= 0x2D0 && channel <= 0x2F7)
        {
            mhz = 866.0000 + 0.025 * (channel - 0x2D0);
        }
        else if(channel >= 0x32F && channel <= 0x33F)
        {
            mhz = 867.0000 + 0.025 * (channel - 0x32F);
        }
        else if(channel >= 0x3C1 && channel <= 0x3FE)
        {
            mhz = 867.4250 + 0.025 * (channel - 0x3C1);
        }
        else if(channel == 0x3BE)
        {
            mhz = 868.9750;
        }

        return Math.round(mhz * 1_000_000.0);
    }

    /**
     * Indicates the command is a channel number in this band plan.
     */
    public boolean isChannel(int command)
    {
        return getFrequency(command) > 0;
    }
}
