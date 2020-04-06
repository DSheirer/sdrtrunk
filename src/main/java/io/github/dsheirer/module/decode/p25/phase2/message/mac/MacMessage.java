/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase2.message.mac;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.Voice4VOffset;
import io.github.dsheirer.module.decode.p25.phase2.message.P25P2Message;

import java.util.List;

/**
 * Encoded MAC Information (EMI) Message base class
 */
public class MacMessage extends P25P2Message
{
    private static int[] PDU_TYPE = {0, 1, 2};
    private static int[] OFFSET_TO_NEXT_VOICE_4V_START = {3, 4, 5};
    private static int[] RESERVED = {6, 7};

    private int mChannelNumber;
    private DataUnitID mDataUnitID;
    private CorrectedBinaryMessage mMessage;
    private MacStructure mMacStructure;

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
        super(timestamp);
        mChannelNumber = timeslot;
        mDataUnitID = dataUnitID;
        mMessage = message;
        mMacStructure = macStructure;
    }

    /**
     * Number of bit errors that were corrected
     */
    public int getBitErrorCount()
    {
        return getMessage().getCorrectedBitCount();
    }

    /**
     * Timeslot / Channel number for this message
     */
    public int getTimeslot()
    {
        return mChannelNumber;
    }

    /**
     * Data Unit ID or timeslot type for this message
     */
    public DataUnitID getDataUnitID()
    {
        return mDataUnitID;
    }

    /**
     * Underlying binary message as transmitted and error-correctede
     */
    protected CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * MAC structure parser/payload for this message
     */
    public MacStructure getMacStructure()
    {
        return mMacStructure;
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
        return getMacStructure().getIdentifiers();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TS").append(getTimeslot());
        sb.append(" ").append(getDataUnitID());

        if(isValid())
        {
            sb.append(" ").append(getMacPduType().toString());
            sb.append(" ").append(getMacStructure().toString());
        }
        else
        {
            sb.append(" INVALID/CRC ERROR");
        }

        return sb.toString();
    }
}
