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

package io.github.dsheirer.module.decode.p25.phase2.message.mac;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Nac;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.Voice4VOffset;
import io.github.dsheirer.module.decode.p25.phase2.message.P25P2Message;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructure;
import java.util.ArrayList;
import java.util.List;

/**
 * Encoded MAC Information (EMI) Message base class
 */
public class MacMessage extends P25P2Message
{
    private static final IntField PDU_TYPE = IntField.range(0, 2);
    private static final IntField OFFSET_TO_NEXT_VOICE_4V_START = IntField.range(3, 5);
    private static final IntField RESERVED = IntField.range(6, 7);
    private DataUnitID mDataUnitID;
    private MacStructure mMacStructure;
    private Identifier mNAC;

    /**
     * Constructs the message
     *
     * @param timeslot for this message
     * @param message containing underlying transmitted bits
     * @param timestamp of the final bit of the message
     * @param macStructure for the payload
     */
    public MacMessage(int timeslot, DataUnitID dataUnitID, CorrectedBinaryMessage message,
                      long timestamp, MacStructure macStructure)
    {
        super(message, 0, timeslot, timestamp);
        mDataUnitID = dataUnitID;
        mMacStructure = macStructure;
    }

    /**
     * Assigns the NAC value that is optionally decoded from the LCCH mac structure.
     * @param nac value to assign
     */
    public void setNAC(int nac)
    {
        mNAC = APCO25Nac.create(nac);
    }

    /**
     * NAC value when available.
     * @return NAC value or zero if the value is unavailable (@code hasNAC()).
     */
    public Identifier getNAC()
    {
        return mNAC;
    }

    /**
     * Indicates if this mac message has a non-zero NAC value.
     * @return true if available.
     */
    public boolean hasNAC()
    {
        return mNAC != null;
    }

    /**
     * Number of bit errors that were corrected
     */
    public int getBitErrorCount()
    {
        return getMessage().getCorrectedBitCount();
    }

    /**
     * Data Unit ID or timeslot type for this message
     */
    public DataUnitID getDataUnitID()
    {
        return mDataUnitID;
    }

    /**
     * MAC structure parser/payload for this message
     */
    public MacStructure getMacStructure()
    {
        return mMacStructure;
    }

    /**
     * Assigns a new mac structure to this mac message.
     * @param macStructure to assign.
     */
    public void setMacStructure(MacStructure macStructure)
    {
        mMacStructure = macStructure;
    }

    /**
     * MAC opcode identifies the type of MAC PDU for this message
     */
    public MacPduType getMacPduType()
    {
        return getMacPduTypeFromMessage(getMessage());
    }

    /**
     * Lookup the MAC opcode from the message
     * @param message containing a mac opcode
     * @return opcode
     */
    public static MacPduType getMacPduTypeFromMessage(CorrectedBinaryMessage message)
    {
        return MacPduType.fromValue(message.getInt(PDU_TYPE));
    }

    /**
     * Offset to the next Voice 4V start sequence
     */
    public Voice4VOffset getOffsetToNextVoice4VStart()
    {
        return Voice4VOffset.fromValue(getMessage().getInt(OFFSET_TO_NEXT_VOICE_4V_START));
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.addAll(getMacStructure().getIdentifiers());
        if(hasNAC())
        {
            identifiers.add(getNAC());
        }

        return identifiers;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TS").append(getTimeslot());
        sb.append(" ").append(getDataUnitID());

        if(!isValid())
        {
            sb.append(" [CRC ERROR]");
        }

        if(hasNAC())
        {
            sb.append(" NAC:").append(getNAC());
        }

        sb.append(" ").append(getMacPduType().toString());
        sb.append(" ").append(getMacStructure().toString());

        return sb.toString();
    }
}
