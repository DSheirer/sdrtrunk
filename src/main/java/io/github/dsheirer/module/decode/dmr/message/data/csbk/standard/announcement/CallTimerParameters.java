/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.ArrayList;
import java.util.List;

/**
 * DMR Tier III - Call Timer Parameters
 */
public class CallTimerParameters extends Announcement
{
    //Broadcast Parameters 1: 21-34
    private static final int[] EMERGENCY_TIMER = new int[]{21, 22, 23, 24, 25, 26, 27, 28, 29};
    private static final int[] PACKET_TIMER = new int[]{30, 31, 32, 33, 34};

    //Broadcast Parameters 2: 56-79
    private static final int[] MOBILE_TO_MOBILE_TIMER = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67};
    private static final int[] MOBILE_TO_LINE_TIMER = new int[]{68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

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
    public CallTimerParameters(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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

        sb.append(" CALL TIMERS EMERG:").append(getEmergencyTimerDecoded());
        sb.append(" PACKET:").append(getPacketTimerDecoded());
        sb.append(" MS-MS:").append(getMobileToMobileTimerDecoded());
        sb.append(" MS-LINE:").append(getMobileToLineTimerDecoded());

        sb.append(" ").append(getSystemIdentityCode().getModel());
        sb.append(" NETWORK:").append(getSystemIdentityCode().getNetwork());
        sb.append(" SITE:").append(getSystemIdentityCode().getSite());

        return sb.toString();
    }

    /**
     * Emergency Timer
     * @return 0 = Internal, 1-510 = value, 511 = infinity
     */
    public int getEmergencyTimer()
    {
        return getMessage().getInt(EMERGENCY_TIMER);
    }

    /**
     * Decodes the numeric timer value for emergency calls
     */
    public String getEmergencyTimerDecoded()
    {
        int timer = getEmergencyTimer();

        if(timer == 0)
        {
            return "INTERNAL";
        }
        else if(1 <= timer && timer <= 10)
        {
            return timer + " SECS";
        }
        else if(11 <= timer && timer <= 20)
        {
            return ((timer - 8) * 5) + " SECS";
        }
        else if(21 <= timer && timer <= 28)
        {
            return ((timer - 16) * 15) + " SECS";
        }
        else if(29 <= timer && timer <= 40)
        {
            return ((timer - 22) * .5) + " MINS";
        }
        else if(41 <= timer && timer <= 51)
        {
            return (timer - 31) + " MINS";
        }
        else if(52 <= timer && timer <= 510)
        {
            return ((timer - 47) * 5) + " MINS";
        }

        return "INFINITY";
    }

    /**
     * Packet Timer
     * @return 0 = Internal, 1-30 = value, 31 = infinity
     */
    public int getPacketTimer()
    {
        return getMessage().getInt(PACKET_TIMER);
    }

    /**
     * Decodes the numeric timer value for packet calls
     */
    public String getPacketTimerDecoded()
    {
        int timer = getPacketTimer();

        if(timer == 0)
        {
            return "INTERNAL";
        }
        else if(1 <= timer && timer <= 5)
        {
            return timer + " SECS";
        }
        else if(6 <= timer && timer <= 10)
        {
            return ((timer - 4) * 5) + " SECS";
        }
        else if(11 <= timer && timer <= 12)
        {
            return ((timer - 8) * 15) + " SECS";
        }
        else if(13 <= timer && timer <= 20)
        {
            return ((timer - 10) * .5) + " MINS";
        }
        else if(21 <= timer && timer <= 25)
        {
            return (timer - 15) + " MINS";
        }
        else if(26 <= timer && timer <= 30)
        {
            return ((timer - 23) * 5) + " MINS";
        }

        return "INFINITY";
    }

    /**
     * Mobile-to-Mobile Call Timer
     * @return 0 = Internal, 1-4094 = value, 4095 = infinity
     */
    public int getMobileToMobileTimer()
    {
        return getMessage().getInt(MOBILE_TO_MOBILE_TIMER);
    }

    /**
     * Decodes the numeric timer value for mobile-2-mobile calls
     */
    public String getMobileToMobileTimerDecoded()
    {
        int timer = getMobileToMobileTimer();

        if(timer == 0)
        {
            return "INTERNAL";
        }
        else if(1 <= timer && timer <= 59)
        {
            return timer + " SECS";
        }
        else if(60 <= timer && timer <= 107)
        {
            return ((timer - 48) * 5) + " SECS";
        }
        else if(108 <= timer && timer <= 138)
        {
            return ((timer - 98) * .5) + " MINS";
        }
        else if(139 <= timer && timer <= 4094)
        {
            return (timer - 118) + " MINS";
        }

        return "INFINITY";
    }

    /**
     * Mobile-to-Line Call Timer
     * @return 0 = Internal, 1-510 = value, 511 = infinity
     */
    public int getMobileToLineTimer()
    {
        return getMessage().getInt(MOBILE_TO_LINE_TIMER);
    }

    /**
     * Decodes the numeric timer value for mobile-2-line calls
     */
    public String getMobileToLineTimerDecoded()
    {
        int timer = getMobileToLineTimer();

        if(timer == 0)
        {
            return "INTERNAL";
        }
        else if(1 <= timer && timer <= 59)
        {
            return timer + " SECS";
        }
        else if(60 <= timer && timer <= 107)
        {
            return ((timer - 48) * 5) + " SECS";
        }
        else if(108 <= timer && timer <= 138)
        {
            return ((timer - 98) * .5) + " MINS";
        }
        else if(139 <= timer && timer <= 4094)
        {
            return (timer - 118) + " MINS";
        }

        return "INFINITY";
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSystemIdentityCode().getNetwork());
            mIdentifiers.add(getSystemIdentityCode().getSite());
        }

        return mIdentifiers;
    }
}
