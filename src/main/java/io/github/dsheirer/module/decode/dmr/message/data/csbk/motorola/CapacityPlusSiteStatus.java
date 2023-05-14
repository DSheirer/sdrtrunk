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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.channel.DMRLogicalChannel;
import io.github.dsheirer.module.decode.dmr.channel.ITimeslotFrequencyReceiver;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Capacity+ Site Status CSBKO=62 Message
 */
public class CapacityPlusSiteStatus extends CSBKMessage implements ITimeslotFrequencyReceiver
{
    private static final int[] BYTE = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] TWO_BYTES = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] SEGMENT_INDICATOR = new int[]{16, 17};
    private static final int TIMESLOT = 18;
    private static final int RESERVED = 19;
    private static final int[] REST_LSN = new int[]{20, 21, 22, 23};
    private static final int[] LSN_VOICE_BITMAP = new int[]{24, 25, 26, 27, 28, 29, 30, 31};
    private static final int LSN_1_8_BITMAP_START = 24;
    private static final int[][] VOICE_TALKGROUPS = new int[][]{{32, 33, 34, 35, 36, 37, 38, 39},
            {40, 41, 42, 43, 44, 45, 46, 47}, {48, 49, 50, 51, 52, 53, 54, 55}, {56, 57, 58, 59, 60, 61, 62, 63},
            {64, 65, 66, 67, 68, 69, 70, 71}, {72, 73, 74, 75, 76, 77, 78, 79}};

    private DMRLogicalChannel mRestChannel;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance
     *
     * @param syncPattern for the CSBK
     * @param message bits
     * @param cach for the DMR burst
     * @param slotType for this message
     * @param timestamp
     * @param timeslot
     */
    public CapacityPlusSiteStatus(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        sb.append("CC:").append(getSlotType().getColorCode());

        if(hasRAS())
        {
            sb.append(" RAS:").append(getBPTCReservedBits());
        }

        sb.append(" CSBK CAP+ SITE STATUS REST ").append(getRestChannel());
        sb.append(" ").append(getSegmentIndicator());
        sb.append(" ");

        if(getSegmentIndicator().isFirst())
        {
            sb.append(getActivityFragments());
        }
        else
        {
            sb.append("CONTINUATION BLOCK");
        }

        sb.append(" MSG:").append(getMessage().toHexString());

        return sb.toString();
    }

    /**
     * Indicates if this message has voice talkgroup activity.
     *
     * @return true if voice activity.
     */
    private boolean hasVoiceTalkgroups()
    {
        return getSegmentIndicator().isFirst() && getMessage().getInt(LSN_VOICE_BITMAP) > 0;
    }

    /**
     * Extracts the call activity from the first or first/last fragment.
     * @return description of call activity.
     */
    private String getActivityFragments()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("VOICE LSN ");
        int pointer = LSN_1_8_BITMAP_START;

        //Process voice LSNs 1-8
        if(getMessage().getInt(BYTE, pointer) > 0)
        {
            int lowLsnBitmap = pointer;
            pointer += 8;

            for(int x = lowLsnBitmap; x < (lowLsnBitmap + 8); x++)
            {
                int lsn = x - lowLsnBitmap + 1;

                if(getMessage().get(x))
                {
                    if(pointer <= 72)
                    {
                        sb.append(lsn).append(":").append(getMessage().getInt(BYTE, pointer)).append(" ");
                        pointer += 8;
                    }
                    else
                    {
                        sb.append(lsn).append(":A ");
                    }
                }
                else
                {
                    sb.append(lsn).append(":* ");
                }
            }
        }
        else
        {
            sb.append("1-8:* ");
            pointer += 8;
        }

        //Process voice LSNs 9 - 16
        if(pointer <= 72 && getMessage().getInt(BYTE, pointer) > 0)
        {
            int highLsnBitmap = pointer;

            pointer += 8;

            for(int x = highLsnBitmap; x < (highLsnBitmap + 8); x++)
            {
                int lsn = x - highLsnBitmap + 1 + 8;

                if(getMessage().get(x))
                {
                    if(pointer <= 72)
                    {
                        sb.append(lsn).append(":").append(getMessage().getInt(BYTE, pointer)).append(" ");
                        pointer += 8;
                    }
                    else
                    {
                        sb.append(lsn).append(":A ");
                    }
                }
                else
                {
                    sb.append(lsn).append(":* ");
                }
            }
        }
        else
        {
            sb.append("9-16:* ");
            pointer += 8;
        }

        //Process Data and Private Radio IDs - first bit in radio options byte is set to indicate more activity
        if(pointer <= 72 && getMessage().get(pointer))
        {
            sb.append("DATA LSN ");
            pointer += 8;

            //If we have the data revert channel bitmap ...
            if(pointer <= 72 && getMessage().getInt(BYTE, pointer) > 0)
            {
                int lowDataLsnBitmap = pointer;

                pointer += 8;

                for(int x = lowDataLsnBitmap; x < (lowDataLsnBitmap + 8); x++)
                {
                    int lsn = x - lowDataLsnBitmap + 1;

                    if(getMessage().get(x))
                    {
                        if(pointer <= 64)
                        {
                            sb.append(lsn).append(":").append(getMessage().getInt(TWO_BYTES, pointer)).append(" ");
                            pointer += 16;
                        }
                        else
                        {
                            sb.append(lsn).append(":A ");
                        }
                    }
                    else
                    {
                        sb.append(lsn).append(":* ");
                    }
                }
            }
            else
            {
                sb.append("1-8:- ");
            }
        }

        return sb.toString();
    }


    /**
     * Segment indicator for system status message values that are fragmented across multiple system status
     * messages.
     */
    public SegmentIndicator getSegmentIndicator()
    {
        return SegmentIndicator.fromValue(getMessage().getInt(SEGMENT_INDICATOR));
    }

    /**
     * Current rest channel for this site.
     */
    public DMRLogicalChannel getRestChannel()
    {
        if(mRestChannel == null)
        {
            mRestChannel = new DMRLogicalChannel(getRestRepeater(), getRestTimeslot());
        }

        return mRestChannel;
    }

    /**
     * Rest LSN
     *
     * @return Logical Slot Number 1-16
     */
    public int getRestLSN()
    {
        return getMessage().getInt(REST_LSN);
    }

    /**
     * Rest Channel Repeater
     */
    public int getRestRepeater()
    {
        return (int) Math.ceil(getRestLSN() / 2.0);
    }

    /**
     * Rest Channel Timeslot
     *
     * @return 1 or 2
     */
    public int getRestTimeslot()
    {
        return (getRestLSN() % 2 == 0) ? 2 : 1;
    }

    /**
     * Logical slot numbers that require slot to frequency mappings.
     */
    @Override
    public int[] getLogicalTimeslotNumbers()
    {
        return getRestChannel().getLSNArray();
    }

    /**
     * Applies logical slot number to frequency mapping.
     *
     * @param timeslotFrequencies that match the logical timeslots
     */
    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        for(TimeslotFrequency timeslotFrequency : timeslotFrequencies)
        {
            if(getRestChannel().getLogicalSlotNumber() == timeslotFrequency.getNumber())
            {
                getRestChannel().setTimeslotFrequency(timeslotFrequency);
            }
        }
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getRestChannel());
        }

        return mIdentifiers;
    }
}
