/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.edac.CRCDMR;
import io.github.dsheirer.edac.Golay24;
import io.github.dsheirer.edac.Hamming13;
import io.github.dsheirer.edac.Hamming15;
import io.github.dsheirer.edac.soft.SoftBlockCode;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.dmr.bptc.BPTC_196_96;
import io.github.dsheirer.module.decode.dmr.message.type.DataType;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility for creating DMR data bursts and framing them for unit tests.
 */
public class DMRBurstTestUtility
{
    public static final int BURST_DIBITS = 144;
    public static final int DELAY_LINE_DIBITS = 90; //CACH(12) + MESSAGE_PREFIX(54) + SYNC(24)
    public static final DMRSyncPattern SYNC_PATTERN = DMRSyncPattern.BASE_STATION_DATA;
    public static final int CSBK_CRC_MASK = 0xA5A5;
    private static final SoftBlockCode ROW_CODE = SoftBlockCode.fromCodewordTest(15,
            word -> Hamming15.getSyndrome(toMessage(word, 15), 0) == 0);
    private static final SoftBlockCode COLUMN_CODE = SoftBlockCode.fromCodewordTest(13,
            word -> Hamming13.getSyndrome(toMessage(word, 13), new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}) == 0);

    private static BinaryMessage toMessage(int word, int length)
    {
        BinaryMessage message = new BinaryMessage(length);

        for(int i = 0; i < length; i++)
        {
            if((word & (1 << i)) != 0)
            {
                message.set(i);
            }
        }

        return message;
    }

    /**
     * Finds the systematic codeword whose information bits (the low bits) match the information value.
     */
    private static int encode(SoftBlockCode code, int information, int informationLength)
    {
        int mask = (1 << informationLength) - 1;

        for(int codeword: code.getCodewords())
        {
            if((codeword & mask) == information)
            {
                return codeword;
            }
        }

        throw new IllegalStateException("No codeword for information value " + information);
    }

    /**
     * Creates a random valid BPTC(196,96) codeword in transmitted (interleaved) bit order.
     */
    public static CorrectedBinaryMessage createInterleavedBPTC(Random random)
    {
        CorrectedBinaryMessage deinterleaved = new CorrectedBinaryMessage(196);

        for(int row = 0; row < 9; row++)
        {
            int information = random.nextInt(1 << 11);

            for(int column = 0; column < 11; column++)
            {
                if((information & (1 << column)) != 0)
                {
                    deinterleaved.set(BPTC_196_96.getIndex(column, row));
                }
            }
        }

        return encodeAndInterleave(deinterleaved);
    }

    /**
     * Creates a valid BPTC(196,96) codeword in transmitted (interleaved) bit order that carries the 96-bit payload,
     * with the reserved bits set to zero.
     * @param payload of 96 bits
     */
    public static CorrectedBinaryMessage createInterleavedBPTC(BinaryMessage payload)
    {
        CorrectedBinaryMessage deinterleaved = new CorrectedBinaryMessage(196);

        //Payload bits occupy the row information columns in order, starting at index 4 (see BPTC_196_96.extract())
        int payloadIndex = 0;

        for(int index = 4; index < BPTC_196_96.MAX_ORIGINAL_INDEX && payloadIndex < 96; index++)
        {
            if((index % BPTC_196_96.COLUMN_COUNT) >= 1 && (index % BPTC_196_96.COLUMN_COUNT) <= 11)
            {
                if(payload.get(payloadIndex))
                {
                    deinterleaved.set(index);
                }

                payloadIndex++;
            }
        }

        return encodeAndInterleave(deinterleaved);
    }

    /**
     * Encodes the row and column parity for the information bits (rows 0-8, columns 0-10) of the de-interleaved
     * message and interleaves the result.
     */
    private static CorrectedBinaryMessage encodeAndInterleave(CorrectedBinaryMessage deinterleaved)
    {
        //Information rows 0-8, each encoded with the row code
        for(int row = 0; row < 9; row++)
        {
            int information = 0;

            for(int column = 0; column < 11; column++)
            {
                if(deinterleaved.get(BPTC_196_96.getIndex(column, row)))
                {
                    information |= (1 << column);
                }
            }

            int rowWord = encode(ROW_CODE, information, 11);

            for(int column = 11; column < 15; column++)
            {
                if((rowWord & (1 << column)) != 0)
                {
                    deinterleaved.set(BPTC_196_96.getIndex(column, row));
                }
            }
        }

        //Parity rows 9-12 from the column code
        for(int column = 0; column < 15; column++)
        {
            int information = 0;

            for(int row = 0; row < 9; row++)
            {
                if(deinterleaved.get(BPTC_196_96.COLUMN_INDEXES[column][row]))
                {
                    information |= (1 << row);
                }
            }

            int columnWord = encode(COLUMN_CODE, information, 9);

            for(int row = 9; row < 13; row++)
            {
                if((columnWord & (1 << row)) != 0)
                {
                    deinterleaved.set(BPTC_196_96.COLUMN_INDEXES[column][row]);
                }
            }
        }

        //De-interleave maps deinterleaved[x] = interleaved[DEINTERLEAVE[x]], so interleave is the inverse
        CorrectedBinaryMessage interleaved = new CorrectedBinaryMessage(196);

        for(int x = 0; x < 196; x++)
        {
            if(deinterleaved.get(x))
            {
                interleaved.set(BPTC_196_96.BPTC_DEINTERLEAVE[x]);
            }
        }

        return interleaved;
    }

    /**
     * Creates a random 96-bit CSBK payload with a valid CRC-CCITT (80 message bits and 16 CRC bits).
     */
    public static BinaryMessage createCSBKPayload(Random random)
    {
        BinaryMessage payload = new BinaryMessage(96);
        int crc = CSBK_CRC_MASK;

        for(int x = 0; x < 80; x++)
        {
            if(random.nextBoolean())
            {
                payload.set(x);
                crc ^= CRCDMR.CCITT_80_CHECKSUMS[x];
            }
        }

        for(int x = 0; x < 16; x++)
        {
            if((crc & (1 << (15 - x))) != 0)
            {
                payload.set(80 + x);
            }
        }

        return payload;
    }

    /**
     * Creates the 20 transmitted slot type bits: Golay(24,12) shortened by 4 leading zero bits, where the 24-bit
     * message has the color code at bits 4-7, the data type at 8-11, the 11-bit checksum at 12-22 and even parity at 23.
     */
    public static boolean[] createSlotType(int colorCode, DataType dataType)
    {
        boolean[] golay = new boolean[24];
        int information = (colorCode << 4) | dataType.getValue();
        int checksum = 0;

        for(int bit = 0; bit < 8; bit++)
        {
            if((information & (1 << (7 - bit))) != 0)
            {
                golay[4 + bit] = true;
                checksum ^= Golay24.CHECKSUMS[4 + bit];
            }
        }

        int ones = Integer.bitCount(information);

        for(int bit = 0; bit < 11; bit++)
        {
            if((checksum & (1 << (10 - bit))) != 0)
            {
                golay[12 + bit] = true;
                ones++;
            }
        }

        golay[23] = ones % 2 != 0;

        boolean[] transmitted = new boolean[20];
        System.arraycopy(golay, 4, transmitted, 0, 20);
        return transmitted;
    }

    /**
     * Creates a 288-bit DMR base station data burst: CACH (0-23), BPTC payload part 1 (24-121), slot type part 1
     * (122-131), sync (132-179), slot type part 2 (180-189) and BPTC payload part 2 (190-287).
     */
    public static boolean[] createBurst(CorrectedBinaryMessage bptc, int colorCode, DataType dataType)
    {
        boolean[] burst = new boolean[288];
        boolean[] slotType = createSlotType(colorCode, dataType);

        for(int x = 0; x < 98; x++)
        {
            burst[24 + x] = bptc.get(x);
            burst[190 + x] = bptc.get(98 + x);
        }

        for(int x = 0; x < 10; x++)
        {
            burst[122 + x] = slotType[x];
            burst[180 + x] = slotType[10 + x];
        }

        Dibit[] sync = SYNC_PATTERN.toDibits();

        for(int x = 0; x < sync.length; x++)
        {
            burst[132 + 2 * x] = sync[x].getBit1();
            burst[133 + 2 * x] = sync[x].getBit2();
        }

        return burst;
    }

    /**
     * Converts the burst bits into ideal soft symbols.
     */
    public static float[] toSoftSymbols(boolean[] burst)
    {
        float[] softSymbols = new float[BURST_DIBITS];

        for(int x = 0; x < BURST_DIBITS; x++)
        {
            int value = (burst[2 * x] ? 2 : 0) + (burst[2 * x + 1] ? 1 : 0);
            softSymbols[x] = Dibit.fromValue(value).getIdealPhase();
        }

        return softSymbols;
    }

    /**
     * BPTC payload dibit indexes in the burst, excluding the CACH, slot type and sync dibits.
     */
    public static List<Integer> getPayloadDibits()
    {
        List<Integer> payloadDibits = new ArrayList<>();

        for(int x = 12; x < 61; x++)
        {
            payloadDibits.add(x);
        }

        for(int x = 95; x < BURST_DIBITS; x++)
        {
            payloadDibits.add(x);
        }

        return payloadDibits;
    }

    /**
     * Moves the soft symbol just across the outer/inner (PI/2) decision boundary, flipping the low reliability
     * magnitude bit of the symbol decision.
     * @param softSymbols to modify
     * @param burst bits to modify to match the new symbol decision
     * @param dibit index of the symbol to modify
     * @param margin distance from the decision boundary
     */
    public static void addLowReliabilityError(float[] softSymbols, boolean[] burst, int dibit, float margin)
    {
        float sign = Math.signum(softSymbols[dibit]);
        boolean outer = Math.abs(softSymbols[dibit]) > Math.PI / 2;
        softSymbols[dibit] = sign * ((float)(Math.PI / 2) + (outer ? -margin : margin));
        burst[2 * dibit + 1] = !burst[2 * dibit + 1];
    }

    /**
     * Extracts the transmitted BPTC(196,96) bits from the burst.
     */
    public static CorrectedBinaryMessage getBPTC(boolean[] burst)
    {
        CorrectedBinaryMessage bptc = new CorrectedBinaryMessage(196);

        for(int x = 0; x < 98; x++)
        {
            bptc.set(x, burst[24 + x]);
            bptc.set(98 + x, burst[190 + x]);
        }

        return bptc;
    }

    /**
     * Feeds the burst soft symbols to the framer via the C4FM input, signals the sync detection on the final sync
     * dibit, and then flushes the burst through the delay line with filler symbols.
     * @return messages dispatched by the framer.
     */
    public static List<IMessage> frame(float[] softSymbols)
    {
        List<IMessage> messages = new ArrayList<>();
        DMRMessageFramer framer = new DMRMessageFramer(new DMRCrcMaskManager(false));
        framer.setListener(messages::add);
        framer.start();

        //The sync pattern ends at dibit 89 (CACH 12 + prefix 54 + sync 24)
        for(int x = 0; x < DELAY_LINE_DIBITS; x++)
        {
            framer.process(Dibit.fromSample(softSymbols[x]), softSymbols[x]);
        }

        framer.setSyncPattern(SYNC_PATTERN);
        framer.syncDetected();

        //Remaining burst dibits, followed by filler dibits to eject the full burst from the delay line
        for(int x = DELAY_LINE_DIBITS; x < BURST_DIBITS + DELAY_LINE_DIBITS; x++)
        {
            float softSymbol = x < BURST_DIBITS ? softSymbols[x] : Dibit.D00_PLUS_1.getIdealPhase();
            framer.process(Dibit.fromSample(softSymbol), softSymbol);
        }

        return messages;
    }
}
