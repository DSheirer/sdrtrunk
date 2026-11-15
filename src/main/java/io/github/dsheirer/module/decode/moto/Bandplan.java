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

package io.github.dsheirer.module.decode.moto;

/**
 * Calculates frequencies from channel numbers for Motorola Type II trunking systems.
 * Reference: OP25 tk_smartnet.py bandplan calculations.
 */
public class Bandplan
{
    private final BandplanType mBandplanType;
    
    // OBT (Other Band Trunking) custom parameters
    private final double mObtBaseFrequency;
    private final double mObtSpacing;
    private final int mObtOffset;
    
    // Standard bandplan constants
    private static final double BASE_800_DOMESTIC = 851.0125;
    private static final double BASE_800_REBAND_TRANSITION = 851.0250;
    private static final double BASE_800_SPLINTER = 851.0000;
    private static final double BASE_800_SPLINTER_UPPER = 866.0125;
    private static final double BASE_900 = 935.0125;
    
    private static final double SPACING_800 = 0.025;
    private static final double SPACING_900 = 0.0125;
    
    private static final double TX_OFFSET_800 = -45.0;
    private static final double TX_OFFSET_900 = -39.0;
    
    // Channel boundaries
    private static final int CHAN_800_REBAND_BOUNDARY = 0x1B7;
    private static final int CHAN_800_REBAND_TRANSITION_END = 0x22F;
    private static final int CHAN_800_DOMESTIC_MAX = 0x2CF;
    private static final int CHAN_800_SPLINTER_BOUNDARY = 0x257;
    
    // Upper channel ranges (all 800 MHz plans)
    private static final int CHAN_800_UPPER_1_START = 0x2D0;
    private static final int CHAN_800_UPPER_1_END = 0x2F7;
    private static final int CHAN_800_UPPER_2_START = 0x32F;
    private static final int CHAN_800_UPPER_2_END = 0x33F;
    private static final int CHAN_800_UPPER_3_START = 0x3C1;
    private static final int CHAN_800_UPPER_3_END = 0x3FE;
    private static final int CHAN_800_UPPER_SPECIAL = 0x3BE;
    
    private static final double BASE_800_UPPER_1 = 866.0000;
    private static final double BASE_800_UPPER_2 = 867.0000;
    private static final double BASE_800_UPPER_3 = 867.4250;
    private static final double BASE_800_UPPER_SPECIAL = 868.9750;
    
    /**
     * Constructor for standard bandplans (not OBT).
     */
    public Bandplan(BandplanType bandplanType)
    {
        this(bandplanType, 0.0, 0.0, 0);
    }
    
    /**
     * Constructor for OBT (Other Band Trunking) with custom parameters.
     */
    public Bandplan(BandplanType bandplanType, double obtBaseFrequency, double obtSpacing, int obtOffset)
    {
        mBandplanType = bandplanType;
        mObtBaseFrequency = obtBaseFrequency;
        mObtSpacing = obtSpacing;
        mObtOffset = obtOffset;
    }
    
    /**
     * Calculate downlink (receive) frequency from channel number.
     * @param channel Channel number (10-bit value, 0-1023)
     * @return Frequency in MHz, or -1.0 if channel is invalid
     */
    public double getDownlinkFrequency(int channel)
    {
        switch(mBandplanType)
        {
            case EIGHT_HUNDRED_REBANDED:
                return get800RebandedFrequency(channel);
            case EIGHT_HUNDRED_DOMESTIC:
                return get800DomesticFrequency(channel);
            case EIGHT_HUNDRED_DOMESTIC_SPLINTER:
                return get800SplinterFrequency(channel);
            case EIGHT_HUNDRED_INTERNATIONAL:
                return get800InternationalFrequency(channel);
            case EIGHT_HUNDRED_INTERNATIONAL_SPLINTER:
                return get800InternationalSplinterFrequency(channel);
            case NINE_HUNDRED:
                return get900Frequency(channel);
            case OBT:
                return getObtFrequency(channel);
            default:
                return -1.0;
        }
    }
    
    /**
     * Calculate uplink (transmit) frequency from channel number.
     * @param channel Channel number
     * @return Frequency in MHz, or -1.0 if channel is invalid
     */
    public double getUplinkFrequency(int channel)
    {
        double downlink = getDownlinkFrequency(channel);
        if(downlink < 0)
        {
            return -1.0;
        }
        
        double txOffset = getTxOffset(downlink);
        return downlink + txOffset;
    }
    
    /**
     * Check if a channel number is valid for this bandplan.
     */
    public boolean isChannelValid(int channel)
    {
        return getDownlinkFrequency(channel) > 0;
    }
    
    private double get800RebandedFrequency(int channel)
    {
        // Rebanded: channels 0x000-0x1B7 use standard formula
        if(channel <= CHAN_800_REBAND_BOUNDARY)
        {
            return BASE_800_DOMESTIC + (SPACING_800 * channel);
        }
        // Channels 0x1B8-0x22F use rebanded formula
        else if(channel <= CHAN_800_REBAND_TRANSITION_END)
        {
            return BASE_800_REBAND_TRANSITION + (SPACING_800 * (channel - CHAN_800_REBAND_BOUNDARY - 1));
        }
        // Upper channels use same formula as domestic
        else
        {
            return get800UpperFrequency(channel);
        }
    }
    
    private double get800DomesticFrequency(int channel)
    {
        if(channel <= CHAN_800_DOMESTIC_MAX)
        {
            return BASE_800_DOMESTIC + (SPACING_800 * channel);
        }
        else
        {
            return get800UpperFrequency(channel);
        }
    }
    
    private double get800SplinterFrequency(int channel)
    {
        if(channel <= CHAN_800_SPLINTER_BOUNDARY)
        {
            return BASE_800_SPLINTER + (SPACING_800 * channel);
        }
        else if(channel <= CHAN_800_DOMESTIC_MAX)
        {
            return BASE_800_SPLINTER_UPPER + (SPACING_800 * (channel - CHAN_800_SPLINTER_BOUNDARY - 1));
        }
        else
        {
            return get800UpperFrequency(channel);
        }
    }
    
    private double get800InternationalFrequency(int channel)
    {
        // International uses same formula as domestic for lower channels
        // but different upper channel mapping (not fully documented)
        // For now, use domestic formula
        return get800DomesticFrequency(channel);
    }
    
    private double get800InternationalSplinterFrequency(int channel)
    {
        // International splinter uses same formula as splinter for lower channels
        return get800SplinterFrequency(channel);
    }
    
    private double get800UpperFrequency(int channel)
    {
        // Upper channel ranges (same for all 800 MHz plans)
        if(channel >= CHAN_800_UPPER_1_START && channel <= CHAN_800_UPPER_1_END)
        {
            return BASE_800_UPPER_1 + (SPACING_800 * (channel - CHAN_800_UPPER_1_START));
        }
        else if(channel >= CHAN_800_UPPER_2_START && channel <= CHAN_800_UPPER_2_END)
        {
            return BASE_800_UPPER_2 + (SPACING_800 * (channel - CHAN_800_UPPER_2_START));
        }
        else if(channel >= CHAN_800_UPPER_3_START && channel <= CHAN_800_UPPER_3_END)
        {
            return BASE_800_UPPER_3 + (SPACING_800 * (channel - CHAN_800_UPPER_3_START));
        }
        else if(channel == CHAN_800_UPPER_SPECIAL)
        {
            return BASE_800_UPPER_SPECIAL;
        }
        
        return -1.0;
    }
    
    private double get900Frequency(int channel)
    {
        // 900 MHz: simple linear formula
        return BASE_900 + (SPACING_900 * channel);
    }
    
    private double getObtFrequency(int channel)
    {
        // OBT: user-configurable base/spacing/offset
        if(mObtSpacing <= 0)
        {
            return -1.0;
        }
        
        return mObtBaseFrequency + (mObtSpacing * (channel - mObtOffset));
    }
    
    private double getTxOffset(double downlinkFrequency)
    {
        // Auto-compute TX offset based on frequency range
        if(downlinkFrequency >= 851.0 && downlinkFrequency <= 869.0)
        {
            return TX_OFFSET_800;
        }
        else if(downlinkFrequency >= 935.0 && downlinkFrequency <= 941.0)
        {
            return TX_OFFSET_900;
        }
        else if(downlinkFrequency >= 136.0 && downlinkFrequency <= 174.0)
        {
            // VHF: simplex (no offset)
            return 0.0;
        }
        else if(downlinkFrequency >= 380.0 && downlinkFrequency <= 406.0)
        {
            // 380-406 MHz: +10 MHz
            return 10.0;
        }
        else if(downlinkFrequency >= 406.0 && downlinkFrequency <= 420.0)
        {
            // 406-420 MHz: +9 MHz
            return 9.0;
        }
        else if(downlinkFrequency >= 450.0 && downlinkFrequency <= 470.0)
        {
            // 450-470 MHz: +5 MHz
            return 5.0;
        }
        else if(downlinkFrequency >= 470.0 && downlinkFrequency <= 512.0)
        {
            // 470-512 MHz: +3 MHz
            return 3.0;
        }
        
        // Default: no offset
        return 0.0;
    }
    
    public BandplanType getBandplanType()
    {
        return mBandplanType;
    }
}
