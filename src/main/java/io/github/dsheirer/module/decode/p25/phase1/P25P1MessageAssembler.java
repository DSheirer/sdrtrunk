/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUHeader;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.TSBKMessageFactory;
import io.github.dsheirer.module.decode.p25.reference.PDUFormat;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P25 Phase 1 message assembler.  Processes an incoming stream of demodulated dibits/symbols and tracks the expected
 * message length against the actual received symbol count to indicate when the message is fully assembled.
 *
 * This assembler also supports fuzzy message type estimation.  When the NID can't be fully error corrected, we can make
 * the best guess for message type and then validate at the next sync interval that the quantity of elapsed symbols
 * matches the expected message length and then either accept the estimated data unit ID, or reassign a data unit ID
 * that more closely matches the quantity of elapsed symbols.
 */
public class P25P1MessageAssembler implements Listener<Dibit>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(P25P1MessageAssembler.class);
    private static final LoggingSuppressor LOGGING_SUPPRESSOR = new LoggingSuppressor(LOGGER);
    private static final int MINIMUM_LDU_BIT_LENGTH = 1500; //actual length is 1568
    private CorrectedBinaryMessage mMessage;
    private P25P1DataUnitID mDataUnitID;
    private int mNac;

    /**
     * Constructs an instance
     * @param nac for the message
     * @param duid for the message type
     *
     * Note: if the duid is PLACEHOLDER it indicates the NID did not pass error detection/correction and therefore the
     * duid is suspect and we'll close out the message assembly once the next sync is detected and then we can inspect
     * the previous/next duids to determine what is the logical duid that would have been transmitted in between, and
     * also check the dibit count to determine the correct duid.
     */
    public P25P1MessageAssembler(int nac, P25P1DataUnitID duid)
    {
        mNac = nac;
        mDataUnitID = duid;
        int length = duid.getMessageLength();

        if(length < 0)
        {
            System.out.println("Negative message length [" + length + "]  duid [" + duid + "]");
            length = 0;
        }

        mMessage = new CorrectedBinaryMessage(length);
    }

    /**
     * Reconfigure this assembler to continue assembling subsequent message blocks. The assembling message size is
     * extended to the expected message length of the new DUID.
     *
     * @param duid for the subsequent message block to assemble.
     */
    public void reconfigure(P25P1DataUnitID duid)
    {
        mDataUnitID = duid;

        if(mMessage != null)
        {
            mMessage.setSize(duid.getMessageLength());
        }
        else
        {
            mMessage = new CorrectedBinaryMessage(duid.getMessageLength());
        }
    }

    /**
     * Forces this assembler to complete assembly of the current message.  Attempts to identify
     *
     *
     * ion by either retaining the existing data unit ID when there was a valid NID
     * decoded, or updating the data unit ID based on the quantity of dibits assembled thus far.
     * @param previous data unit ID for the message prior to the currently assembled message
     * @param next data unit ID for the message that will be assembled next
     * @return number of bits that were dropped.
     */
    public int forceCompletion(P25P1DataUnitID previous, P25P1DataUnitID next)
    {
//        System.out.println("\n[_] FORCING COMPLETION - PREV:" + previous + " CURRENT:" + mDataUnitID + " NEXT:" + next + " RECEIVED BITS:" + getMessage().currentSize() + "/" + getMessage().size() + "\n");

        if(mDataUnitID == P25P1DataUnitID.PLACE_HOLDER)
        {
            int length = getMessage().currentSize();

            if(length <= 28)
            {
                mDataUnitID = P25P1DataUnitID.TERMINATOR_DATA_UNIT;
            }
            else
            {
                switch(next)
                {
                    case LOGICAL_LINK_DATA_UNIT_1:
                        if(length <= 770)
                        {
                            mDataUnitID = P25P1DataUnitID.HEADER_DATA_UNIT;
                        }
                        else if(length >= MINIMUM_LDU_BIT_LENGTH)
                        {
                            mDataUnitID = P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_2;
                        }
                        break;
                    case LOGICAL_LINK_DATA_UNIT_2:
                        if(length >= MINIMUM_LDU_BIT_LENGTH)
                        {
                            mDataUnitID = P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1;
                        }
                        break;
                    case TRUNKING_SIGNALING_BLOCK_1:
                        if(length >= 195)
                        {
                            //Determine if this is a TSBK or an AMBTC PDU
                            CorrectedBinaryMessage candidate = TSBKMessageFactory.deinterleaveViterbiAndCrc(getMessage().getSubMessage(0, 195));

                            if(candidate != null && candidate.getCorrectedBitCount() >= 0)
                            {
                                PDUFormat format = PDUHeader.getFormat(candidate);

                                if(format == PDUFormat.UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL ||
                                   format == PDUFormat.ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL)
                                {
                                    mDataUnitID = P25P1DataUnitID.PACKET_DATA_UNIT;
                                }
                                else
                                {
                                    mDataUnitID = P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_1;
                                }
                            }
                            else
                            {
                                mDataUnitID = P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_1;
                            }
                        }
                        break;
                }
            }


            //If we're still PLACE_HOLDER, meaning we didn't resolve it using the next DUID, check the previous DUID
            if(mDataUnitID == P25P1DataUnitID.PLACE_HOLDER)
            {
                switch(previous)
                {
                    case LOGICAL_LINK_DATA_UNIT_1:
                        if(length >= MINIMUM_LDU_BIT_LENGTH)
                        {
                            mDataUnitID = P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_2;
                        }
                        break;
                    case LOGICAL_LINK_DATA_UNIT_2:
                        if(length >= MINIMUM_LDU_BIT_LENGTH)
                        {
                            mDataUnitID = P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1;
                        }
                        break;
                }
            }
        }

        if(mDataUnitID == P25P1DataUnitID.PLACE_HOLDER)
        {
            mDataUnitID = P25P1DataUnitID.TERMINATOR_DATA_UNIT;
        }

        getMessage().setSize(mDataUnitID.getMessageLength());
        return getMessage().size() - getMessage().currentSize();
    }

    /**
     * The decoded NAC or zero if the NAC hasn't yet been fully decoded.
     * @return NAC
     */
    public int getNAC()
    {
        return mNac;
    }

    /**
     * Access the completed message.
     * @return message.
     */
    public CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Primary input method for demodulated dibits/symbols to be appended to the message under assembly.
     * @param dibit to add to the message.
     */
    public void receive(Dibit dibit)
    {
        if(mMessage.isFull())
        {
            LOGGING_SUPPRESSOR.error("P25P1-Full", 3, "P25 Phase 1 message under assembly is " +
                    "full - can't add additional dibits.");
        }
        else
        {
            mMessage.add(dibit.getBit1(), dibit.getBit2());
        }
    }

    /**
     * Indicates if the message is fully assembled meaning that the quantity of assembled dibits matches the data unit
     * ID message length.
     * @return true if complete.
     */
    public boolean isComplete()
    {
        return getMessage().isFull();
    }

    /**
     * Data unit ID for the message under assembly.  Note: this is the best guess data unit ID when the isValidNID()
     * method indicates false, meaning the NID was not able to be confirmed through error detection and correction.
     * @return current data unit ID.
     */
    public P25P1DataUnitID getDataUnitID()
    {
        return mDataUnitID;
    }

    /**
     * Reassigns the data unit ID to match a more likely data unit ID based on the number of symbols that have elapsed
     * between the sync detection that triggered this message assembly and the subsequent sync detection for the
     * next message.
     *
     * @param dataUnitID to assign.
     */
    public void setDataUnitID(P25P1DataUnitID dataUnitID)
    {
        mMessage.setSize(dataUnitID.getMessageLength());
        mDataUnitID = dataUnitID;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("NAC:" + mNac + " Assembled [").append(mMessage.pointer()).append("] bits of [" +
                mDataUnitID.getMessageLength() + "] for DUID:").append(mDataUnitID.name());
        return sb.toString();
    }
}
