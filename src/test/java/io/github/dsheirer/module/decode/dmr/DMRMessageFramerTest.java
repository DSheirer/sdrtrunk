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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.dmr.bptc.BPTC_196_96;
import io.github.dsheirer.module.decode.dmr.message.data.IDLEMessage;
import io.github.dsheirer.module.decode.dmr.message.type.DataType;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility.addLowReliabilityError;
import static io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility.createBurst;
import static io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility.createInterleavedBPTC;
import static io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility.frame;
import static io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility.getBPTC;
import static io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility.getPayloadDibits;
import static io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility.toSoftSymbols;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the DMR message framer C4FM symbol stream input, where the framer maintains the burst delay lines
 * for the dibits and soft symbols and the sync detection is signalled on the final dibit of the sync pattern.
 */
public class DMRMessageFramerTest
{
    private static final int COLOR_CODE = 5;
    //Soft symbol distance from the +/-3 and +/-1 decision boundary (PI/2) for low reliability symbol errors.
    private static final float LOW_RELIABILITY_MARGIN = 0.05f;

    /**
     * Finds the IDLE message dispatched by the framer
     */
    private static IDLEMessage getIdleMessage(List<IMessage> messages)
    {
        IDLEMessage idle = null;

        for(IMessage message: messages)
        {
            if(message instanceof IDLEMessage idleMessage)
            {
                idle = idleMessage;
            }
        }

        assertNotNull(idle, "Framer did not dispatch an IDLE message: " + messages);
        return idle;
    }

    /**
     * Test: a burst fed through the C4FM input with the sync detection signalled on the final sync dibit is framed
     * with the correct alignment so that the slot type and BPTC payload decode without errors.
     */
    @Test
    void testBurstAlignment()
    {
        Random random = new Random(1);
        CorrectedBinaryMessage bptc = createInterleavedBPTC(random);
        CorrectedBinaryMessage expected = BPTC_196_96.extract(bptc);
        assertEquals(0, expected.getCorrectedBitCount());

        IDLEMessage idle = getIdleMessage(frame(toSoftSymbols(createBurst(bptc, COLOR_CODE, DataType.SLOT_IDLE))));
        assertTrue(idle.isValid());
        assertEquals(COLOR_CODE, idle.getSlotType().getColorCode());
        assertEquals(DataType.SLOT_IDLE, idle.getSlotType().getDataType());
        assertEquals(expected.toString(), idle.getMessage().toString());
        assertEquals(0, idle.getMessage().getCorrectedBitCount());
    }

    /**
     * Test: low reliability symbol errors in the BPTC payload that exceed the hard decision decoder capability are
     * corrected by soft decision decoding, which requires the soft symbols to remain aligned with the dibits through
     * the framer delay lines.
     *
     * Note: for IDLE messages (no CRC), soft decision decoding is only applied when the hard decision decoder fails,
     * so trials where the hard decision decoder reports success (correctly, or with a miscorrection) don't exercise
     * the soft symbols and are not evaluated.
     */
    @Test
    void testSoftSymbolAlignment()
    {
        List<Integer> payloadDibits = getPayloadDibits();
        int hardFailures = 0;

        for(int trial = 0; trial < 40; trial++)
        {
            Random random = new Random(100 + trial);
            CorrectedBinaryMessage bptc = createInterleavedBPTC(random);
            CorrectedBinaryMessage expected = BPTC_196_96.extract(bptc);
            boolean[] burst = createBurst(bptc, COLOR_CODE, DataType.SLOT_IDLE);
            float[] softSymbols = toSoftSymbols(burst);

            //Add 10 low reliability payload symbol errors
            Set<Integer> errors = new HashSet<>();

            while(errors.size() < 10)
            {
                errors.add(payloadDibits.get(random.nextInt(payloadDibits.size())));
            }

            for(int error: errors)
            {
                addLowReliabilityError(softSymbols, burst, error, LOW_RELIABILITY_MARGIN);
            }

            //Only evaluate trials where the hard decision decoder fails
            if(BPTC_196_96.extract(getBPTC(burst)).getCorrectedBitCount() >= 0)
            {
                continue;
            }

            hardFailures++;
            IDLEMessage idle = getIdleMessage(frame(softSymbols));
            assertTrue(idle.isValid(), "Trial " + trial);
            assertEquals(expected.toString(), idle.getMessage().toString(), "Trial " + trial);
        }

        //Sanity check that the soft decision decoder was exercised
        assertTrue(hardFailures >= 5, "Expected at least 5 hard decision failures, but found " + hardFailures);
    }
}
