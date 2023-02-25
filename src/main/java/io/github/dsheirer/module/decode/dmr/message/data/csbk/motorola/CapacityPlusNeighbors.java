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
import io.github.dsheirer.module.decode.dmr.identifier.DMRSite;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.type.LCSS;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Capacity Plus - Neighbor Sites
 */
public class CapacityPlusNeighbors extends CSBKMessage implements ITimeslotFrequencyReceiver
{
    private static final int[] LC_START_STOP = new int[]{16, 17};
    private static final int TIMESLOT = 18;
    private static final int[] REST_REPEATER = new int[]{19, 20, 21, 22};
    private static final int[] REST_TIMESLOT = new int[]{23};
    private static final int ASYNC = 24;
    private static final int[] SITE = new int[]{25, 26, 27, 28};
    private static final int[] NEIGHBOR_COUNT = new int[]{29, 30, 31};
    private static final int[] NEIGHBOR_1_SITE = new int[]{32, 33, 34, 35};
    private static final int[] NEIGHBOR_1_REST = new int[]{36, 37, 38, 39};
    private static final int[] NEIGHBOR_2_SITE = new int[]{40, 41, 42, 43};
    private static final int[] NEIGHBOR_2_REST = new int[]{44, 45, 46, 47};
    private static final int[] NEIGHBOR_3_SITE = new int[]{48, 49, 50, 51};
    private static final int[] NEIGHBOR_3_REST = new int[]{52, 53, 54, 55};
    private static final int[] NEIGHBOR_4_SITE = new int[]{56, 57, 58, 59};
    private static final int[] NEIGHBOR_4_REST = new int[]{60, 61, 62, 63};
    private static final int[] NEIGHBOR_5_SITE = new int[]{64, 65, 66, 67};
    private static final int[] NEIGHBOR_5_REST = new int[]{68, 69, 70, 71};
    private static final int[] NEIGHBOR_6_SITE = new int[]{72, 73, 74, 75};
    private static final int[] NEIGHBOR_6_REST = new int[]{76, 77, 78, 79};


    private DMRLogicalChannel mRestChannel;
    private DMRSite mSite;
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
    public CapacityPlusNeighbors(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" CSBK CAP+ SITE:").append(getSite());
        sb.append(" REST ").append(getRestChannel());
        sb.append(" FL:").append(getLCSS());

        if(hasNeighbor(1))
        {
            sb.append(" NEIGHBORS SITE:").append(getNeighbor1Site());
            sb.append("-REST:").append(getNeighbor1Rest());

            if(hasNeighbor(2))
            {
                sb.append(" SITE:").append(getNeighbor2Site());
                sb.append("-REST:").append(getNeighbor2Rest());

                if(hasNeighbor(3))
                {
                    sb.append(" SITE:").append(getNeighbor3Site());
                    sb.append("-REST:").append(getNeighbor3Rest());

                    if(hasNeighbor(4))
                    {
                        sb.append(" SITE:").append(getNeighbor4Site());
                        sb.append("-REST:").append(getNeighbor4Rest());

                        if(hasNeighbor(5))
                        {
                            sb.append(" SITE:").append(getNeighbor5Site());
                            sb.append("-REST:").append(getNeighbor5Rest());

                            if(hasNeighbor(6))
                            {
                                sb.append(" SITE:").append(getNeighbor6Site());
                                sb.append("-REST:").append(getNeighbor6Rest());
                            }
                        }
                    }
                }
            }
        }

        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    public boolean hasNeighbor(int neighbor)
    {
        return neighbor <= getNeighborCount();
    }

    /**
     * Number of neighbors reported
     */
    public int getNeighborCount()
    {
        return getMessage().getInt(NEIGHBOR_COUNT);
    }


    /**
     * Neighbor 1 Site
     */
    public int getNeighbor1Site()
    {
        return getMessage().getInt(NEIGHBOR_1_SITE);
    }

    /**
     * Neighbor 1 Current Rest Channel
     */
    public int getNeighbor1Rest()
    {
        return getMessage().getInt(NEIGHBOR_1_REST);
    }

    /**
     * Neighbor 2 Site
     */
    public int getNeighbor2Site()
    {
        return getMessage().getInt(NEIGHBOR_2_SITE);
    }

    /**
     * Neighbor 2 Current Rest Channel
     */
    public int getNeighbor2Rest()
    {
        return getMessage().getInt(NEIGHBOR_2_REST);
    }

    /**
     * Neighbor 3 Site
     */
    public int getNeighbor3Site()
    {
        return getMessage().getInt(NEIGHBOR_3_SITE);
    }

    /**
     * Neighbor 3 Current Rest Channel
     */
    public int getNeighbor3Rest()
    {
        return getMessage().getInt(NEIGHBOR_3_REST);
    }

    /**
     * Neighbor 4 Site
     */
    public int getNeighbor4Site()
    {
        return getMessage().getInt(NEIGHBOR_4_SITE);
    }

    /**
     * Neighbor 4 Current Rest Channel
     */
    public int getNeighbor4Rest()
    {
        return getMessage().getInt(NEIGHBOR_4_REST);
    }

    /**
     * Neighbor 5 Site
     */
    public int getNeighbor5Site()
    {
        return getMessage().getInt(NEIGHBOR_5_SITE);
    }

    /**
     * Neighbor 5 Current Rest Channel
     */
    public int getNeighbor5Rest()
    {
        return getMessage().getInt(NEIGHBOR_5_REST);
    }

    /**
     * Neighbor 6 Site
     */
    public int getNeighbor6Site()
    {
        return getMessage().getInt(NEIGHBOR_6_SITE);
    }

    /**
     * Neighbor 6 Current Rest Channel
     */
    public int getNeighbor6Rest()
    {
        return getMessage().getInt(NEIGHBOR_6_REST);
    }

    /**
     * This site number
     *
     * @return
     */
    public DMRSite getSite()
    {
        if(mSite == null)
        {
            mSite = DMRSite.create(getMessage().getInt(SITE));
        }

        return mSite;
    }

    /**
     * Indicates if the site uses period beacons or asynchronous beacons on the rest channel.
     */
    public String getBeacon()
    {
        return getMessage().get(ASYNC) ? "ASYNC" : "PERIODIC";
    }

    /**
     * Message Fragment indicator
     */
    public LCSS getLCSS()
    {
        return LCSS.fromValue(getMessage().getInt(LC_START_STOP));
    }

    /**
     * Rest repeater
     */
    public int getRestRepeater()
    {
        return getMessage().getInt(REST_REPEATER) + 1;
    }

    /**
     * Rest timeslot
     * @return timeslot 1 or 2
     */
    public int getRestTimeslot()
    {
        return getMessage().getInt(REST_TIMESLOT) + 1;
    }

    /**
     * DMR Channel
     */
    public DMRLogicalChannel getRestChannel()
    {
        if(mRestChannel == null)
        {
            mRestChannel = new DMRLogicalChannel(getRestRepeater(), getRestTimeslot());
        }

        return mRestChannel;
    }

    @Override
    public int[] getLogicalTimeslotNumbers()
    {
        return getRestChannel().getLSNArray();
    }

    /**
     * Assigns a timeslot frequency map for the DMR channel
     *
     * @param timeslotFrequencies that match the logical timeslots
     */
    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        for(TimeslotFrequency timeslotFrequency : timeslotFrequencies)
        {
            if(timeslotFrequency.getNumber() == getRestChannel().getLogicalSlotNumber())
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
