/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25Radio;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.Encryption;

import java.util.ArrayList;
import java.util.List;

/**
 * Radio unit monitor command - enhanced format.  Commands the target radio to initiate a group or unit-to-unit call to
 * either the source address to or the talkgroup, either clear or encrypted
 */
public class RadioUnitMonitorCommandEnhanced extends MacStructure
{
    private static final int[] TARGET_ADDRESS = {8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
        26, 27, 28, 29, 30, 31};
    private static final int[] TALKGROUP_ID = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] SOURCE_ADDRESS = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64,
        65, 66, 67, 68, 69, 70, 71};
    private static final int SM = 72; //Stealth Mode?
    private static final int TG = 73;
    private static final int[] TRANSMIT_TIME = {80, 81, 82, 83, 84, 85, 86, 87};
    private static final int[] KEY_ID = {88, 89, 90, 91, 92, 93, 94, 95};
    private static final int[] ALGORITHM_ID = {96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111};


    private List<Identifier> mIdentifiers;
    private Identifier mTargetAddress;
    private Identifier mTalkgroupId;
    private Identifier mSourceAddress;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public RadioUnitMonitorCommandEnhanced(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" CALL RADIO:").append(getSourceAddress());
        sb.append(" OR TALKGROUP:").append(getTalkgroupId());

        if(isStealthMode())
        {
            sb.append(" STEALTH MODE");
        }

        if(isTG())
        {
            sb.append(" ?TG?");
        }

        long transmitTime = getTransmitTime();

        if(transmitTime > 0)
        {
            sb.append(" TRANSMIT TIME:").append(transmitTime / 1000d).append("secs");
        }

        Encryption encryption = getEncryption();

        if(encryption != Encryption.UNENCRYPTED)
        {
            sb.append(" USE ENCRYPTION:").append(encryption);
            sb.append(" KEY:").append(getEncryptionKeyId());
        }

        return sb.toString();
    }

    /**
     * To radio address
     */
    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25Radio.createTo(getMessage().getInt(TARGET_ADDRESS, getOffset()));
        }

        return mTargetAddress;
    }

    /**
     * Talkgroup to call
     */
    public Identifier getTalkgroupId()
    {
        if(mTalkgroupId == null)
        {
            mTalkgroupId = APCO25Talkgroup.create(getMessage().getInt(TALKGROUP_ID, getOffset()));
        }

        return mTalkgroupId;
    }

    /**
     * From Radio Unit
     */
    public Identifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25Radio.createFrom(getMessage().getInt(SOURCE_ADDRESS, getOffset()));
        }

        return mSourceAddress;
    }

    /**
     * No ICD ... anyone?
     */
    public boolean isStealthMode()
    {
        return getMessage().get(SM + getOffset());
    }

    /**
     * No ICD ... anyone?
     */
    public boolean isTG()
    {
        return getMessage().get(TG + getOffset());
    }

    /**
     * No ICD ... anyone?
     * @return
     */
    public long getTransmitTime()
    {
        return getMessage().getInt(TRANSMIT_TIME, getOffset()) * 100; //No ICD ... not sure if multiplier is correct
    }

    /**
     * Encryption key id to use for the callback
     */
    public int getEncryptionKeyId()
    {
        return getMessage().getInt(KEY_ID, getOffset());
    }

    /**
     * Encryption algorithm to use for the call-back
     */
    public Encryption getEncryption()
    {
        return Encryption.fromValue(getMessage().getInt(ALGORITHM_ID, getOffset()));
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getSourceAddress());
            mIdentifiers.add(getTalkgroupId());
        }

        return mIdentifiers;
    }
}
