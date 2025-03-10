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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.alias.P25TalkerAliasIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.TimeslotMessage;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.protocol.Protocol;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola completely assembled talker alias.  Note: this is not a true link control word.  It is reassembled from a
 * header and data blocks.
 */
public class MotorolaTalkerAliasComplete extends TimeslotMessage implements IMessage
{
    private static final byte[] LOOKUP_TABLE =
    {
         -14,   46,  102, -112,  116, -118,  111,  120,  -69,   83,    3,   17,  104,  -51,   68,   23,
          40,   95,   30, -124,  117,  121,  110, -101,   44,  -66,   98,   45,  -15,  124,  -72, -125,
         -39,   78,  109,    2,   97,   61,  -88,    6,  -71,   -8, -100,   55,   58,   35,  -63,   80,
         -19,  -97,  -81,   59,  -67, -126,  -70,  -96,  -33,  -62,   71,   34,  -16,  -18,  -95,   -2,
         -94,   16,   91,   72,   87,  -93,    5,   96,  123,   13,   -7,  108,  -77,   86,   76,  -68,
          41,  -92,   15,  -20,  -74,  -91,  -90,   60,  127,  107,  -76,   33,  -83,  -82,  -60,  -56,
         -59,   93,  -34,  -32,   29,   25,   75,  -58,   12,   63,   90,  -57,  -31,   89,   85,   84,
          74,   67,   66,  -30,  -29,   -6,    0,  -28,  -27,   24,   65,   11,   10,  -26,   -4,   -3,
         -46,  -10,  -44,   43,   99,   73, -108,   94,  -89,   92,  112,  105,   -9,    8,  -79,  125,
          56,  -49,  -52,  -40,   81, -113,  -43, -109,  106,  -13,  -17,  126,   -5,  100,  -12,   53,
          39,    7,   49,   20, -121, -104,  118,   52,  -54, -110,   51,   27,   79, -116,    9,   64,
          50,   54,  119,   18,  -45,  -61,    1,  -85,  114, -127, -107,  -55,  -64,  -23,  101,   82,
          36,   48,   28,  -37, -120,  -24, -105,  -99,   88,   38,    4,   57,  -84,   42,  -98,  -86,
          37,  -41,  -50,  -21, -106,  -11,   14, -115,  -36,  -87,   47,  -35,   31,  -22, -111,  -73,
         -42, -119, -117,  -47,  -80, -103,   19,  122,  -25, -102,  -75, -122,   -1,   70, -123,  -78,
         115,  -38,  -65,  -48,  113,  -53,   77, -128,   21,  103,   22,   26,   32, -114,   69,   62
    };

    private static final IntField SUID_WACN = IntField.length20(OCTET_0_BIT_0);
    private static final IntField SUID_SYSTEM = IntField.length12(OCTET_2_BIT_16 + 4);
    private static final IntField SUID_ID = IntField.length24(OCTET_4_BIT_32);
    private static final IntField CHUNK = IntField.length16(0);
    private static final int ENCODED_ALIAS_START = OCTET_7_BIT_56;
    private static final int CHUNK_SIZE = 16;

    private APCO25Talkgroup mTalkgroup;
    private APCO25FullyQualifiedRadioIdentifier mRadio;
    private P25TalkerAliasIdentifier mAlias;
    private List<Identifier> mIdentifiers;
    private int mSequence;
    private Protocol mProtocol;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message assembled from the data blocks
     * @param talkgroup from the header
     * @param dataBlockCount from the header
     * @param sequence number for the alias
     * @param timeslot for the message
     * @param timestamp of the most recent header or data block
     * @param protocol for the message
     */
    public MotorolaTalkerAliasComplete(CorrectedBinaryMessage message, APCO25Talkgroup talkgroup, int sequence,
                                       int timeslot, long timestamp, Protocol protocol)
    {
        super(message, timeslot, timestamp);
        mTalkgroup = talkgroup;
        mSequence = sequence;
        mProtocol = protocol;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(getTimeslot() > TIMESLOT_0)
        {
            sb.append("TS").append(getTimeslot()).append(" ");
        }

        if(!isValid())
        {
            sb.append("(CRC FAILED) ");
        }

        sb.append("MOTOROLA TALKER ALIAS COMPLETE");
        sb.append(" RADIO:").append(getRadio());
        sb.append(" TG:").append(getTalkgroup());
        sb.append(" ALIAS:").append(getAlias());
        sb.append(" SEQUENCE:").append(mSequence);
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Protocol - P25 Phase 1 or Phase 2
     */
    @Override
    public Protocol getProtocol()
    {
        return mProtocol;
    }

    /**
     * Sequence number for the alias.
     */
    public int getSequence()
    {
        return mSequence;
    }

    /**
     * Decoded alias string
     */
    public P25TalkerAliasIdentifier getAlias()
    {
        if(mAlias == null)
        {
            byte[] encoded = getEncodedAlias().toByteArray();

            // Note: CRC check is performed by assembler.
            //   Data:  wwwww sss iiiiii aaaa...aaaa cccc
            //
            //   - w = WACN
            //   - s = system
            //   - i = id
            //   - a = encoded alias
            //   - c = CRC-16/GSM of the previous bytes
            // Get number of bytes and characters excluding checksum
            char bytes = (char) (encoded.length - 2);
            char chars = (char) (bytes / 2);

            // Create array for decoded computed bytes (big-endian)
            byte[] decoded = new byte[encoded.length];

            // Set up the long-running accumulator
            char accumulator = bytes;
            char byteIndex = 0;

            // Calculate each byte sequentially
            do
            {
                // Multiplication step 1
                char accumMult = (char) (((accumulator + 65536) % 65536) * 293 + 0x72E9);

                // Lookup table step
                byte lut = LOOKUP_TABLE[encoded[byteIndex] + 128];
                byte mult1 = (byte) (lut - (byte) (accumMult >> 8));

                // Incrementing step
                byte mult2 = 1;
                byte shortstop = (byte) (accumMult | 0x1);
                byte increment = (byte) (shortstop << 1);

                while(mult2 != -1 && shortstop != 1)
                {
                    shortstop = (byte) (shortstop + increment);
                    mult2 += 2;
                }

                // Multiplication step 2
                decoded[byteIndex] = (byte) (mult1 * mult2);

                // Update the accumulator
                accumulator += (char) (((encoded[byteIndex] + 256) % 256) + 1);
                byteIndex += 1;
            }
            while(byteIndex < bytes);

            // Copy decoded bytes (as chars) to our alias string
            String alias = "";
            for(char i = 0; i < chars; i++)
            {
                alias += (char) ((decoded[i * 2] << 8) | decoded[i * 2 + 1]);
            }

            mAlias = P25TalkerAliasIdentifier.create(alias);
        }

        return mAlias;
    }

    /**
     * Calculates the 16-bit value at the specified chunk number.
     * @param chunk to get
     * @return int value at the chunk
     */
    private int getChunkValue(int chunk)
    {
        int lastIndex = ENCODED_ALIAS_START + (chunk * CHUNK_SIZE) - 1;

        if(getMessage().size() >= lastIndex)
        {
            return getInt(CHUNK, ENCODED_ALIAS_START + ((chunk - 1) * CHUNK_SIZE));
        }

        return 0;
    }

    /**
     * Extracts the encoded alias payload.
     * @return encoded alias binary message
     */
    private BinaryMessage getEncodedAlias()
    {
        int length = 1;

        for(int x = 16; x > 1; x--)
        {
            if(getChunkValue(x) > 0)
            {
                length = x;
                break;
            }
        }

        return getMessage().getSubMessage(ENCODED_ALIAS_START, ENCODED_ALIAS_START + (length * 16));
    }

    /**
     * Talkgroup
     */
    public APCO25Talkgroup getTalkgroup()
    {
        return mTalkgroup;
    }

    /**
     * Radio that is being aliased.
     */
    public APCO25FullyQualifiedRadioIdentifier getRadio()
    {
        if(mRadio == null)
        {
            int wacn = getInt(SUID_WACN);
            int system = getInt(SUID_SYSTEM);
            int id = getInt(SUID_ID);

            mRadio = APCO25FullyQualifiedRadioIdentifier.createFrom(id, wacn, system, id);
        }

        return mRadio;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTalkgroup());
            mIdentifiers.add(getRadio());

            //Only add the alias if it passes the CRC check.
            if(isValid())
            {
                mIdentifiers.add(getAlias());
            }
        }

        return mIdentifiers;
    }
}
