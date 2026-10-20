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

package io.github.dsheirer.module.decode.p25.phase2.message;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.edac.ReedSolomon63TestEncoder;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.message.isch.ISCHDecoder;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.AbstractSignalingTimeslot;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.ScramblingSequence;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.Timeslot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Synthesizes 720-dibit superframe fragments — an I-ISCH plus a scrambled SACCH in timeslot A, an I-ISCH plus a
 * scrambled FACCH in timeslot B, sync-marked idle timeslots C and D — at each of the three positions in the
 * superframe, and checks that SuperFrameFragment descrambles them at the right timeslot offset both when the
 * I-ISCH is intact and when it is unreadable, where the offset must come from the vote over the timeslots' own
 * MAC PDUs.
 */
public class SuperFrameFragmentOffsetVoteTest
{
    private static final int WACN = 0x12345, SYSTEM = 0x2AB, NAC = 0x3CD;
    private static final long ISCH_CODE_WORD_OFFSET = 0x184229d461L; //ISCHDecoder's, applied to every codeword
    private static final int[] DUID_BITS = {0, 1, 74, 75, 244, 245, 318, 319};

    /**
     * A MAC message (SACCH: 168 info bits + CRC-12, FACCH: 144 + CRC-12) whose one structure is the null
     * information message: PDU type IDLE (3), opcode 0, CRC found by search against the decoder's own check.
     */
    private static CorrectedBinaryMessage macMessage(boolean sacch)
    {
        int infoBits = sacch ? 168 : 144;
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(infoBits + 12);
        message.load(0, 3, 3); //MAC_3_IDLE
        message.load(3, 5, 0);
        message.load(8, 8, 0); //TDMA_00_NULL_INFORMATION_MESSAGE

        for(int crc = 0; crc < 4096; crc++)
        {
            message.load(infoBits, 12, crc);

            if(sacch ? CRCP25.crc12_SACCH(message) : CRCP25.crc12_FACCH(message))
            {
                return message;
            }
        }

        throw new IllegalStateException("no CRC-12 value satisfies the checker");
    }

    /**
     * Lays a SACCH or FACCH message out as a 320-bit unscrambled timeslot: the DUID in its eight scattered bits,
     * the RS(63,35) codeword's transmitted symbols in the timeslot's information and parity fields.  Field
     * positions mirror SacchTimeslot / FacchTimeslot.
     */
    private static CorrectedBinaryMessage timeslot(boolean sacch)
    {
        CorrectedBinaryMessage mac = macMessage(sacch);
        int infoSymbols = sacch ? 30 : 26;
        int paritySymbols = sacch ? 22 : 19;
        int[] info = new int[35];

        for(int m = 0; m < infoSymbols; m++)
        {
            info[35 - infoSymbols + m] = mac.getInt(6 * m, 6 * m + 5);
        }

        int[] codeword = ReedSolomon63TestEncoder.encode(info);
        CorrectedBinaryMessage slot = new CorrectedBinaryMessage(320);
        DataUnitID duid = sacch ? DataUnitID.SCRAMBLED_SACCH : DataUnitID.SCRAMBLED_FACCH;
        int encoded = duid.getValueWithParity();

        for(int i = 0; i < 8; i++)
        {
            if((encoded & (0x80 >> i)) != 0)
            {
                slot.set(DUID_BITS[i]);
            }
        }

        //Symbol m (1-based) sits at codeword index 63 - (35 - infoSymbols) - m; parity p at 28 - p.
        int[] infoPositions = new int[infoSymbols];
        int[] parityPositions = new int[paritySymbols];

        if(sacch)
        {
            for(int m = 0; m < 30; m++)
            {
                infoPositions[m] = m < 12 ? 2 + 6 * m : 76 + 6 * (m - 12);
            }

            for(int p = 0; p < 22; p++)
            {
                parityPositions[p] = p < 10 ? 184 + 6 * p : 246 + 6 * (p - 10);
            }
        }
        else
        {
            for(int m = 0; m < 26; m++)
            {
                infoPositions[m] = m < 12 ? 2 + 6 * m : (m < 22 ? 76 + 6 * (m - 12) : (m == 22 ? -1 : 184 + 6 * (m - 23)));
            }

            for(int p = 0; p < 19; p++)
            {
                parityPositions[p] = p < 7 ? 202 + 6 * p : 246 + 6 * (p - 7);
            }
        }

        for(int m = 0; m < infoSymbols; m++)
        {
            int symbol = codeword[62 - (35 - infoSymbols) - m];

            if(infoPositions[m] == -1)
            {
                //FACCH INFO_23 straddles the sync gap: bits 136, 137, 180, 181, 182, 183
                int[] bits = {136, 137, 180, 181, 182, 183};

                for(int b = 0; b < 6; b++)
                {
                    if((symbol & (0x20 >> b)) != 0)
                    {
                        slot.set(bits[b]);
                    }
                }
            }
            else
            {
                slot.load(infoPositions[m], 6, symbol);
            }
        }

        for(int p = 0; p < paritySymbols; p++)
        {
            slot.load(parityPositions[p], 6, codeword[27 - p]);
        }

        return slot;
    }

    /** The 40-bit I-ISCH codeword for a channel (1 or 2) at a superframe fragment location (0, 1, 2). */
    private static long ischCodeword(int channel, int location)
    {
        BinaryMessage message = new BinaryMessage(9);
        message.load(2, 2, channel - 1);
        message.load(4, 2, location);
        return ISCHDecoder.getCodeWord(message) ^ ISCH_CODE_WORD_OFFSET;
    }

    /**
     * Builds the 1440-bit fragment at the given superframe location with both timeslots scrambled under the
     * sequence's segments for that location.
     */
    private static CorrectedBinaryMessage fragment(int location, ScramblingSequence sequence, boolean intactIsch)
    {
        CorrectedBinaryMessage fragment = new CorrectedBinaryMessage(1440);
        int offset = location * 4;

        if(intactIsch)
        {
            fragment.load(0, 40, ischCodeword(1, location));
            fragment.load(360, 40, ischCodeword(2, location));
        }

        CorrectedBinaryMessage[] slots = {timeslot(true), timeslot(false)};
        int[] starts = {40, 400};

        for(int index = 0; index < 2; index++)
        {
            CorrectedBinaryMessage slot = slots[index];
            BinaryMessage segment = sequence.getTimeslotSequence(offset + index);

            for(int bit = 0; bit < 320; bit++)
            {
                boolean duidBit = false;

                for(int d : DUID_BITS)
                {
                    duidBit |= d == bit;
                }

                //The DUID is transmitted in the clear; everything else is scrambled.
                boolean value = slot.get(bit) ^ (!duidBit && segment.get(bit));

                if(value)
                {
                    fragment.set(starts[index] + bit);
                }
            }
        }

        return fragment;
    }

    private static ScramblingSequence sequence(int wacn, int system, int nac)
    {
        ScramblingSequence sequence = new ScramblingSequence();
        sequence.update(wacn, system, nac);
        return sequence;
    }

    /** Number of valid MAC messages across the fragment's signalling timeslots. */
    private static int validMacMessages(SuperFrameFragment fragment)
    {
        int valid = 0;

        for(Timeslot timeslot : fragment.getTimeslots())
        {
            if(timeslot instanceof AbstractSignalingTimeslot signaling)
            {
                for(MacMessage mac : signaling.getMacMessages())
                {
                    if(mac.isValid())
                    {
                        valid++;
                    }
                }
            }
        }

        return valid;
    }

    @Test
    public void intactIschDescramblesAtEveryLocation()
    {
        ScramblingSequence sequence = sequence(WACN, SYSTEM, NAC);

        for(int location = 0; location < 3; location++)
        {
            SuperFrameFragment fragment = new SuperFrameFragment(fragment(location, sequence, true), 0, sequence);
            assertTrue(fragment.getIISCH1().isValid(), "location " + location + ": I-ISCH 1 should decode");
            assertEquals(2, validMacMessages(fragment), "location " + location + ": SACCH and FACCH MAC PDUs");
        }
    }

    @Test
    public void unreadableIschIsResolvedByTheVote()
    {
        ScramblingSequence sequence = sequence(WACN, SYSTEM, NAC);

        for(int location = 0; location < 3; location++)
        {
            SuperFrameFragment fragment = new SuperFrameFragment(fragment(location, sequence, false), 0, sequence);
            assertEquals(2, validMacMessages(fragment),
                "location " + location + ": with no I-ISCH the offset must come from the MAC PDUs");
        }
    }

    @Test
    public void wrongScramblingSequenceDecodesNothing()
    {
        //Negative control: the vote cannot manufacture PDUs from a fragment scrambled under another system.
        ScramblingSequence transmitted = sequence(WACN, SYSTEM, NAC);
        ScramblingSequence other = sequence(0x54321, 0x1BA, 0x2DC);

        for(int location = 0; location < 3; location++)
        {
            SuperFrameFragment fragment = new SuperFrameFragment(fragment(location, transmitted, true), 0, other);
            assertEquals(0, validMacMessages(fragment), "location " + location);
        }
    }
}
