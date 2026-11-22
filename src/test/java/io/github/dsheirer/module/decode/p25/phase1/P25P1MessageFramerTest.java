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

package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.edac.bch.BCH_63_16_23_P25;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SyncDetector;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit tests for the P25 Phase 1 message framer recovery from a frame with a failed NID.  The failed frame assembles
 * as a place holder and the valid NID of the next frame forces completion of that place holder, which must not impact
 * assembly of the next frame.
 */
public class P25P1MessageFramerTest
{
    private static final int NAC = 0x293;
    private static final int TSBK_DUID = 7;
    private static final int OPCODE_RFSS_STATUS_BROADCAST = 58;
    private static final int PDU_FORMAT_AMBTC = 23;
    private static final int FRAME_DIBITS = 180; //Sync, NID, header/single TSBK, status dibits and null padding
    private static final int DATA_BLOCK_DIBITS = 108; //98 dibits, 3 status dibits and null padding, to next frame
    private static final int SYNC_DIBITS = 24;
    private static final int SYNC_FRAMES = 8; //Valid frames preceding the failed frame so that the NAC is tracked
    private static final int NEXT_FRAMES = 3; //Valid frames following the failed frame

    private final Random mRandom = new Random(1);

    /**
     * Test: control case with all frames detected.
     */
    @Test
    void testNoFailedFrame()
    {
        assertEquals(SYNC_FRAMES + NEXT_FRAMES, process(Collections.emptyList()));
    }

    /**
     * Test: a missed AMBTC PDU, fully assembled as a place holder, dispatches all of its data blocks without the
     * 'message under assembly is full' error and without losing the next frame, for each supported block count.
     */
    @Test
    void testPlaceHolderResolvedAsPDU()
    {
        for(int blocks = 1; blocks <= 5; blocks++)
        {
            assertEquals(SYNC_FRAMES + NEXT_FRAMES, process(getPDU(blocks, blocks)), blocks + " data blocks");
        }
    }

    /**
     * Test: a missed AMBTC PDU that's truncated by the next frame, where the header indicates more data blocks than
     * were assembled, doesn't capture the next frame as data blocks.
     */
    @Test
    void testPlaceHolderResolvedAsTruncatedPDU()
    {
        assertEquals(SYNC_FRAMES + NEXT_FRAMES, process(getPDU(3, 1)));
    }

    /**
     * Test: a missed corrupt TSBK, where the failed CRC can't indicate the last block, doesn't capture the next frame
     * as the second TSBK of the sequence.
     */
    @Test
    void testPlaceHolderResolvedAsCorruptTSBK()
    {
        List<Dibit> noise = new ArrayList<>();

        for(int x = 0; x < FRAME_DIBITS; x++)
        {
            noise.add(Dibit.values()[mRandom.nextInt(4)]);
        }

        assertEquals(SYNC_FRAMES + NEXT_FRAMES, process(noise));
    }

    /**
     * Processes valid TSBK frames, the frame that fails sync detection, and then more valid TSBK frames.
     * @param failedFrame dibits
     * @return count of valid TSBK messages dispatched by the framer.
     */
    private int process(List<Dibit> failedFrame)
    {
        int[] validCount = new int[1];
        P25P1MessageFramer framer = new P25P1MessageFramer();
        framer.setListener((IMessage message) -> {
            if(message instanceof TSBKMessage tsbk && tsbk.isValid())
            {
                validCount[0]++;
            }
        });
        framer.start();

        for(int x = 0; x < SYNC_FRAMES; x++)
        {
            process(framer, getFrame(TSBK_DUID, getTSBK(), FRAME_DIBITS), true);
        }

        process(framer, failedFrame, false);

        for(int x = 0; x < NEXT_FRAMES; x++)
        {
            process(framer, getFrame(TSBK_DUID, getTSBK(), FRAME_DIBITS), true);
        }

        //A message is dispatched on the dibit following the message, so flush the final frame
        process(framer, Collections.nCopies(FRAME_DIBITS, Dibit.D00_PLUS_1), false);

        return validCount[0];
    }

    /**
     * Processes the frame dibits, optionally indicating a sync detection after the final sync pattern dibit.
     */
    private static void process(P25P1MessageFramer framer, List<Dibit> dibits, boolean detectSync)
    {
        for(int x = 0; x < dibits.size(); x++)
        {
            framer.process(dibits.get(x));

            if(detectSync && x == SYNC_DIBITS - 1)
            {
                framer.syncDetected();
            }
        }
    }

    /**
     * Creates a single block TSBK with the last block indicator set.
     */
    private BitSet getTSBK()
    {
        BitSet tsbk = new BitSet();
        tsbk.set(0);
        P25P1DecoderLSMTest.setValue(tsbk, 2, 6, OPCODE_RFSS_STATUS_BROADCAST);

        for(int x = 16; x < 80; x++)
        {
            tsbk.set(x, mRandom.nextBoolean());
        }

        return tsbk;
    }

    /**
     * Creates an AMBTC PDU frame.
     * @param indicatedBlocks count of blocks to follow in the header
     * @param actualBlocks count of data blocks in the frame.
     */
    private List<Dibit> getPDU(int indicatedBlocks, int actualBlocks)
    {
        BitSet header = new BitSet();
        header.set(2); //Outbound
        P25P1DecoderLSMTest.setValue(header, 3, 5, PDU_FORMAT_AMBTC);
        P25P1DecoderLSMTest.setValue(header, 49, 7, indicatedBlocks);

        List<Dibit> data = getData(header);

        for(int x = 0; x < actualBlocks; x++)
        {
            data.addAll(P25P1DecoderLSMTest.encodeTSBK(getTSBK()));
        }

        return addStatusDibits(data, FRAME_DIBITS + actualBlocks * DATA_BLOCK_DIBITS);
    }

    /**
     * Creates a frame with the sync, NID, and encoded message.
     */
    private static List<Dibit> getFrame(int duid, BitSet message, int length)
    {
        return addStatusDibits(getData(duid, message), length);
    }

    private static List<Dibit> getData(BitSet pduHeader)
    {
        return getData(12, pduHeader);
    }

    private static List<Dibit> getData(int duid, BitSet message)
    {
        List<Dibit> data = new ArrayList<>();
        P25P1DecoderLSMTest.addBits(data, P25P1SyncDetector.SYNC_PATTERN, 48);
        P25P1DecoderLSMTest.addBits(data, BCH_63_16_23_P25.encode(NAC, duid) << 1, 64); //64th (parity) bit is zero
        data.addAll(P25P1DecoderLSMTest.encodeTSBK(message));
        return data;
    }

    /**
     * Inserts a status dibit at every 36th dibit and null pads the frame to the length.
     */
    private static List<Dibit> addStatusDibits(List<Dibit> data, int length)
    {
        List<Dibit> frame = new ArrayList<>();
        int pointer = 0;

        for(int x = 1; x <= length; x++)
        {
            if(x % 36 == 0)
            {
                frame.add(Dibit.D01_PLUS_3); //Outbound (repeater) status
            }
            else
            {
                frame.add(pointer < data.size() ? data.get(pointer++) : Dibit.D00_PLUS_1);
            }
        }

        return frame;
    }
}
