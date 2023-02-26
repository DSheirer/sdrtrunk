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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.ahoy;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.identifier.DmrTier3Radio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.type.ServiceKind;

/**
 * DMR Tier III - Ahoy!
 */
public abstract class Ahoy extends CSBKMessage
{
    private static final int[] SERVICE_OPTIONS_MIRROR = new int[]{16, 17, 18, 19, 20, 21, 22};
    private static final int ENCRYPTED_SERVICE_OPTION_FLAG = 17;
    protected static final int SERVICE_KIND_FLAG = 23;
    private static final int AMBIENT_LISTENING_SERVICE_FLAG = 24;
    private static final int TALKGROUP_FLAG = 25;
    protected static final int[] APPENDED_BLOCKS_OR_STATUS = new int[]{26, 27};
    private static final int[] SERVICE_KIND = new int[]{28, 29, 30, 31};
    protected static final int[] TARGET_ADDRESS = new int[]{32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46,
        47, 48, 49, 50, 51, 52, 53, 54, 55};
    protected static final int[] MULTI_PURPOSE_FIELD = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69,
        70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    private IntegerIdentifier mTargetAddress;

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
    public Ahoy(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    /**
     * Utility method to lookup service kind for an AHOY message
     */
    public static ServiceKind getServiceKind(CorrectedBinaryMessage message)
    {
        return ServiceKind.fromValue(message.getInt(SERVICE_KIND));
    }

    /**
     * Value of the multi-purpose field.
     */
    public int getMultiPurposeFieldValue()
    {
        return getMessage().getInt(MULTI_PURPOSE_FIELD);
    }

    /**
     * Service Kind for this message
     */
    public ServiceKind getServiceKind()
    {
        return getServiceKind(getMessage());
    }

    /**
     * Flag that has meaning depending on the Service Kind value
     */
    public boolean isServiceKindFlag()
    {
        return getMessage().get(SERVICE_KIND_FLAG);
    }

    /**
     * Indicates if the calling party requests Ambient Listening Service (ALS).
     * @return
     */
    public boolean isAmbientListeningServiceRequest()
    {
        return getMessage().get(AMBIENT_LISTENING_SERVICE_FLAG);
    }

    /**
     * Target Talkgroup or Radio Identifier
     */
    public IntegerIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            if(isTalkgroupTarget())
            {
                mTargetAddress = DMRTalkgroup.create(getMessage().getInt(TARGET_ADDRESS));
            }
            else
            {
                mTargetAddress = DmrTier3Radio.createTo(getMessage().getInt(TARGET_ADDRESS));
            }

        }

        return mTargetAddress;
    }

    /**
     * Indicates if the target address is a talkgroup (true) or radio (false).
     */
    public boolean isTalkgroupTarget()
    {
        return getMessage().get(TALKGROUP_FLAG);
    }
}
