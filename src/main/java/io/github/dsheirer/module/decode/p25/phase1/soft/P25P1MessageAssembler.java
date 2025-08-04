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

package io.github.dsheirer.module.decode.p25.phase1.soft;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
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

        //If the DUID has a trailing status bit, set the message length to 2-bits longer so that we consume the status
        //bit.  This causes the message to have the first 2-bits of the next sync pattern to be appended to the end of
        //the message, but that won't impact anything.
        if(duid.hasTrailingStatusDibit())
        {
            length += 2;
        }

        mMessage = new CorrectedBinaryMessage(length);
    }

    /**
     * Reconfigure this assembler to continue assembling subsequent message blocks.  For example, after receiving
     * PDU block 1, reconfigure to assemble PDU block 2, etc.
     * @param duid for the subsequent message block to assemble.
     * @param messageLength to assemble
     */
    public void reconfigure(P25P1DataUnitID duid, int messageLength)
    {
        mDataUnitID = duid;
        mMessage = new CorrectedBinaryMessage(messageLength);
    }

    /**
     * Marks this assembler for completion by either retaining the existing data unit ID when there was a valid NID
     * decoded, or updating the data unit ID based on the quantity of dibits assembled thus far.
     * @param previous data unit ID for the message prior to the currently assembled message
     * @param next data unit ID for the message that will be assembled next
     */
    public void forceCompletion(P25P1DataUnitID previous, P25P1DataUnitID next)
    {
        System.out.println("FORCING COMPLETION - PREV:" + previous + " CURRENT:" + mDataUnitID + " NEXT:" + next + " BITS:" + getMessage().currentSize());

        //Debug ... for now
        if(mDataUnitID == P25P1DataUnitID.PLACEHOLDER)
        {
            int length = getMessage().currentSize();

            switch(next)
            {
                case LOGICAL_LINK_DATA_UNIT_1:
                    if(length <= 770)
                    {
                        System.out.println("** FORCING COMPLETION TO HDU");
                        mDataUnitID = P25P1DataUnitID.HEADER_DATA_UNIT;
                    }
                    else if(length >= 1566) //Should be 1568
                    {
                        System.out.println("** FORCING COMPLETION TO LDU2");
                        mDataUnitID = P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_2;
                    }
                    break;
                case LOGICAL_LINK_DATA_UNIT_2:
                    if(length >= 1566) //Should be 1568
                    {
                        System.out.println("** FORCING COMPLETION TO LDU1");
                        mDataUnitID = P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1;
                    }
                    break;
            }

            if(mDataUnitID == P25P1DataUnitID.PLACEHOLDER)
            {
                switch(previous)
                {
                    case LOGICAL_LINK_DATA_UNIT_1:
                        if(length >= 1566) //Should be 1568
                        {
                            System.out.println("** FORCING COMPLETION TO LDU2");
                            mDataUnitID = P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_2;
                        }
                        break;
                    case LOGICAL_LINK_DATA_UNIT_2:
                        if(length >= 1566) //Should be 1568
                        {
                            System.out.println("** FORCING COMPLETION TO LDU1");
                            mDataUnitID = P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1;
                        }
                        break;
                    default:
                        System.out.println("** FORCING COMPLETION TO (DEFAULT) TDU");
                        mDataUnitID = P25P1DataUnitID.TERMINATOR_DATA_UNIT;
                        break;
                }
            }
        }

        if(mDataUnitID == P25P1DataUnitID.PLACEHOLDER)
        {
            System.out.println("** FORCING COMPLETION TO (DEFAULT) TDU - CATCH ALL");
            mDataUnitID = P25P1DataUnitID.TERMINATOR_DATA_UNIT;
        }

//        if(mDataUnitID == P25P1DataUnitID.PLACEHOLDER)
//        {
//            int bits = getMessage().currentSize();
//
//            if(bits <= 28)
//            {
//                System.out.println("  ~~ Forcing completion of assembler - DUID:"  + mDataUnitID.name() + " BIT Count: " + bits + " As TERMINATOR  ...oooOOO ~~~ OOOooo...");
//                setDataUnitID(P25P1DataUnitID.TERMINATOR_DATA_UNIT);
//            }
//            else if(bits <= 288) //144 = Terminator, 288 = Terminator x 2
//            {
//                //System.out.println("  ~~ Forcing completion of assembler - DUID:"  + mDataUnitID.name() + " BIT Count: " + bits + " As TERMINATOR  ...oooOOO ~~~ OOOooo... PLUS SKIPPED TERMINATOR");
//                setDataUnitID(P25P1DataUnitID.TERMINATOR_DATA_UNIT);
//            }
//            else if(bits == 360) // And previous is TSBK?
//            {
//                //System.out.println("  ~~ Forcing completion of assembler - DUID:"  + mDataUnitID.name() + " BIT Count: " + bits + " As TSBK1");
//                setDataUnitID(P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_1);
//            }
//            else if(bits <= 434)
//            {
//                //System.out.println("  ~~ Forcing completion of assembler - DUID:"  + mDataUnitID.name() + " BIT Count: " + bits + " As TDULC");
//                setDataUnitID(P25P1DataUnitID.TERMINATOR_DATA_UNIT_LINK_CONTROL);
//            }
//            else if(bits == 576) // And previous is TSBK?
//            {
//                //System.out.println("  ~~ Forcing completion of assembler - DUID:"  + mDataUnitID.name() + " BIT Count: " + bits + " As TSBK2");
//                setDataUnitID(P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_2);
//            }
//            else if(bits == 720) // And previous is TSBK?
//            {
//                //System.out.println("  ~~ Forcing completion of assembler - DUID:"  + mDataUnitID.name() + " BIT Count: " + bits + " As TSBK3");
//                setDataUnitID(P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_3);
//            }
//            else if(bits <= 792) //678 + 114 (sync + nid + 1x status)
//            {
//                //System.out.println("  ~~ Forcing completion of assembler - DUID:"  + mDataUnitID.name() + " BIT Count: " + bits + " As HEADER  @@@ ### <<< --------------------- **START**");
//                setDataUnitID(P25P1DataUnitID.HEADER_DATA_UNIT);
//            }
//            else if(bits <= 1728)
//            {
//                if(previous == P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1)
//                {
//                    //System.out.println("  ~~ Forcing completion of assembler - DUID:"  + mDataUnitID.name() + " BIT Count: " + bits + " As LDU 2  <<--------");
//                    setDataUnitID(P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_2);
//                }
//                else
//                {
//                    //System.out.println("  ~~ Forcing completion of assembler - DUID:"  + mDataUnitID.name() + " BIT Count: " + bits + " As LDU 1  <<--------");
//                    setDataUnitID(P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1);
//                }
//            }
//            else
//            {
//                //Dump it as a TDU
//                setDataUnitID(P25P1DataUnitID.TERMINATOR_DATA_UNIT);
//                //System.out.println("  ~~ Forcing completion of assembler - DUID:"  + mDataUnitID.name() + " BIT Count: " + bits + " As UNKNOWN TDU  ... ;;; @@@ ????????????? @@@ ;;; ...");
//            }
//        }

//        else if(mDataUnitID == P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_3)
//        {
//            if(mBitsProcessedCount < 248)
//            {
//                mDataUnitID = P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_1;
//            }
//            else if(mBitsProcessedCount < 464)
//            {
//                mDataUnitID = P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_2;
//            }
//        }
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
