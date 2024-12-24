/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.dmr.channel.DMRLsn;
import io.github.dsheirer.module.decode.dmr.channel.DmrRestLsn;
import io.github.dsheirer.module.decode.dmr.channel.ITimeslotFrequencyReceiver;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Capacity+ Site Status CSBKO=62 Message
 */
public class CapacityPlusSiteStatus extends CSBKMessage implements ITimeslotFrequencyReceiver
{
    private static final int[] BYTE = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] TWO_BYTES = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] SEGMENT_INDICATOR = new int[]{16, 17};
    private static final int[] REST_LSN = new int[]{19, 20, 21, 22, 23};
    private static final int[] LSN_VOICE_BITMAP = new int[]{24, 25, 26, 27, 28, 29, 30, 31};
    private static final int LSN_1_8_BITMAP_START = 24;

    private Map<Integer,IntegerIdentifier> mActiveIdentifierMap;
    private Map<Integer,DMRLsn> mActiveLsnMap;
    private DmrRestLsn mRestChannel;
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

        sb.append(" CSBK CAP+ SITE STATUS ").append(getRestChannel());
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
     * Extracts the call activity from the first or first/last fragment.
     * @return description of call activity.
     */
    private String getActivityFragments()
    {
        StringBuilder sb = new StringBuilder();

        if(getActiveIdentifierMap().isEmpty())
        {
            sb.append("IDLE LSN: 1-16");
        }
        else
        {
            sb.append("ACTIVE LSN");

            for(int x = 1; x < 17; x++)
            {
                sb.append(" ").append(x);

                if(mActiveIdentifierMap.containsKey(x))
                {
                    Identifier id = mActiveIdentifierMap.get(x);

                    if(id instanceof TalkgroupIdentifier talkgroup)
                    {
                        if(talkgroup.getValue() > 0)
                        {
                            sb.append(":(T)").append(talkgroup.getValue()); //Active talkgroup ID for channel.
                        }
                        else
                        {
                            sb.append(":(T)A"); //Talkgroup active on channel but identifier is in a continuation message
                        }
                    }
                    else if(id instanceof RadioIdentifier radio)
                    {
                        if(radio.getValue() > 0)
                        {
                            sb.append(":(R)").append(radio.getValue()); //Active radio ID for channel
                        }
                        else
                        {
                            sb.append(":(R)A"); //Radio active on channel but identifier is in a continuation message
                        }
                    }
                }
                else
                {
                    sb.append(":"); //No radio or talkgroup active on channel
                }
            }
        }

        return sb.toString();
    }

    /**
     * Indicates if this message has voice talkgroup activity.
     *
     * @return true if voice activity.
     */
    public boolean hasVoiceTalkgroups()
    {
        return getSegmentIndicator().isFirst() && getMessage().getInt(LSN_VOICE_BITMAP) > 0;
    }

    /**
     * Map of active LSNs.
     * @return map of active LSNs
     */
    public Map<Integer,DMRLsn> getActiveLsnMap()
    {
        if(mActiveLsnMap == null)
        {
            getActiveIdentifierMap();
        }

        return mActiveLsnMap;
    }

    /**
     * Map of active voice talkgroups and DMR logical slots where they are active.
     */
    public Map<Integer,IntegerIdentifier> getActiveIdentifierMap()
    {
        if(mActiveIdentifierMap == null)
        {
            mActiveIdentifierMap = new HashMap<>();
            mActiveLsnMap = new HashMap<>();

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
                            mActiveLsnMap.put(lsn, new DMRLsn(lsn));
                            mActiveIdentifierMap.put(lsn, DMRTalkgroup.create(getMessage().getInt(BYTE, pointer)));
                            pointer += 8;
                        }
                        else
                        {
                            //Create a placeholder for the talkgroup since we don't know the value
                            mActiveLsnMap.put(lsn, new DMRLsn(lsn));
                            mActiveIdentifierMap.put(lsn, DMRTalkgroup.create(-1));
                        }
                    }
                }
            }
            else
            {
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
                            mActiveLsnMap.put(lsn, new DMRLsn(lsn));
                            mActiveIdentifierMap.put(lsn, DMRTalkgroup.create(getMessage().getInt(BYTE, pointer)));
                            pointer += 8;
                        }
                        else
                        {
                            //Create a placeholder for the talkgroup since we don't know the value
                            mActiveLsnMap.put(lsn, new DMRLsn(lsn));
                            mActiveIdentifierMap.put(lsn, DMRTalkgroup.create(-1));
                        }
                    }
                }
            }
            else
            {
                pointer += 8;
            }

            //Process Radio IDs - first bit in radio options byte is set to indicate more activity
            if(pointer <= 72 && getMessage().get(pointer))
            {
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
                                mActiveLsnMap.put(lsn, new DMRLsn(lsn));
                                mActiveIdentifierMap.put(lsn, DMRRadio.createTo(getMessage().getInt(TWO_BYTES, pointer)));
                                pointer += 16;
                            }
                            else
                            {
                                //Create a placeholder for the radio since we don't know the value
                                mActiveLsnMap.put(lsn, new DMRLsn(lsn));
                                mActiveIdentifierMap.put(lsn, DMRRadio.createTo(-1));
                            }
                        }
                    }
                }
            }
        }

        return mActiveIdentifierMap;
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
    public DmrRestLsn getRestChannel()
    {
        if(mRestChannel == null)
        {
            mRestChannel = new DmrRestLsn(getRestLSN());
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
     * Logical slot numbers that require slot to frequency mappings.
     */
    @Override
    public int[] getLogicalChannelNumbers()
    {
        Set<Integer> lcnSet = new HashSet<>();
        lcnSet.add(getRestChannel().getChannelNumber());

        for(DMRLsn lsn: getActiveLsnMap().values())
        {
            lcnSet.add(lsn.getChannelNumber());
        }

        return lcnSet.stream().mapToInt(v -> v).toArray();
    }

    /**
     * Applies logical slot number to frequency mapping.
     *
     * @param timeslotFrequencies that match the logical timeslots
     */
    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        getRestChannel().apply(timeslotFrequencies);
        for(DMRLsn lsn: getActiveLsnMap().values())
        {
            lsn.apply(timeslotFrequencies);
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
