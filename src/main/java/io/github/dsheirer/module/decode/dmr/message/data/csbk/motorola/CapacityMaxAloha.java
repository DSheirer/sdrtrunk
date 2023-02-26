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
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.identifier.DmrTier3Radio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.type.ServiceFunction;
import io.github.dsheirer.module.decode.dmr.message.type.SystemIdentityCode;
import io.github.dsheirer.module.decode.dmr.message.type.Version;
import java.util.ArrayList;
import java.util.List;

/**
 * Capacity Plus - Aloha Message
 */
public class CapacityMaxAloha extends CSBKMessage
{
    private static final int[] RESERVED = new int[]{16, 17};
    private static final int SITE_TIMESLOT_SYNCHRONIZATION = 18;
    private static final int[] VERSION = new int[]{19, 20, 21};
    private static final int TIMING_OFFSET = 22;
    private static final int ACTIVE_NETWORK_CONNECTION_FLAG = 23;
    private static final int[] MASK = new int[]{24, 25, 26, 27, 28};
    private static final int[] SERVICE_FUNCTION = new int[]{29, 30};
    private static final int[] N_RAND_WAIT = new int[]{31, 32, 33, 34};
    private static final int REGISTRATION_REQUIRED_FLAG = 35;
    private static final int[] BACKOFF = new int[]{36, 37, 38, 39};
    private static final int SYSTEM_IDENTITY_CODE_OFFSET = 40;
    private static final int[] RADIO = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72,
        73, 74, 75, 76, 77, 78, 79};

    private SystemIdentityCode mSystemIdentityCode;
    private RadioIdentifier mRadioIdentifier;
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
    public CapacityMaxAloha(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" CSBK CAPACITY-MAX ALOHA");

        if(hasRadioIdentifier())
        {
            sb.append(" TO:").append(getRadioIdentifier());
        }

        sb.append(" ").append(getSystemIdentityCode().getModel());
        sb.append(" NETWORK:").append(getSystemIdentityCode().getNetwork());
        sb.append(" SITE:").append(getSystemIdentityCode().getSite());
        if(hasActiveNetworkConnection())
        {
            sb.append(" NET-CONNECTED");
        }
        else
        {
            sb.append(" NET-DISCONNECTED");
        }

        sb.append(" SERVICES:").append(getServiceFunction());

        sb.append(" ETSI VER:").append(getVersion());
        sb.append(" MASK:").append(getMask());

        if(getSystemIdentityCode().getPAR().isMultipleControlChannels())
        {
            sb.append(" ").append(getSystemIdentityCode().getPAR());
        }

        sb.append(" ").append(getMessage().toHexString());

        return sb.toString();
    }

    /**
     * Services provided by the control channel.
     */
    public ServiceFunction getServiceFunction()
    {
        return ServiceFunction.fromValue(getMessage().getInt(SERVICE_FUNCTION));
    }

    /**
     * Mobile subscriber ID masking value.  See: 102 361-4 p6.1.3
     */
    public int getMask()
    {
        return getMessage().getInt(MASK);
    }

    public boolean hasActiveNetworkConnection()
    {
        return getMessage().get(ACTIVE_NETWORK_CONNECTION_FLAG);
    }

    /**
     * DMR Tier III ETSI 102 361-4 ICD Version Number supported by this system
     */
    public Version getVersion()
    {
        return Version.fromValue(getMessage().getInt(VERSION));
    }

    /**
     * Acknowledged radio identifier
     */
    public RadioIdentifier getRadioIdentifier()
    {
        if(mRadioIdentifier == null)
        {
            mRadioIdentifier = DmrTier3Radio.createTo(getMessage().getInt(RADIO));
        }

        return mRadioIdentifier;
    }

    public boolean hasRadioIdentifier()
    {
        return getMessage().getInt(RADIO) != 0;
    }

    /**
     * System Identity Code structure
     */
    public SystemIdentityCode getSystemIdentityCode()
    {
        if(mSystemIdentityCode == null)
        {
            mSystemIdentityCode = new SystemIdentityCode(getMessage(), SYSTEM_IDENTITY_CODE_OFFSET, true);
        }

        return mSystemIdentityCode;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            if(hasRadioIdentifier())
            {
                mIdentifiers.add(getRadioIdentifier());
            }
            mIdentifiers.add(getSystemIdentityCode().getNetwork());
            mIdentifiers.add(getSystemIdentityCode().getSite());
        }

        return mIdentifiers;
    }
}
