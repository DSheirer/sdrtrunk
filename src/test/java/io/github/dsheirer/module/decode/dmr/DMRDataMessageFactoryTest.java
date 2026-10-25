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
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.DMRMessageFactory;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.type.DataType;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility.SYNC_PATTERN;
import static io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility.addLowReliabilityError;
import static io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility.createBurst;
import static io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility.createCSBKPayload;
import static io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility.createInterleavedBPTC;
import static io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility.frame;
import static io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility.getBPTC;
import static io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility.getPayloadDibits;
import static io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility.toSoftSymbols;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the DMR data message factory.
 */
public class DMRDataMessageFactoryTest
{
    private static final int COLOR_CODE = 3;
    private static final int ERROR_COUNT = 12;
    //Soft symbol distance from the +/-3 and +/-1 decision boundary (PI/2) for low reliability symbol errors.
    private static final float LOW_RELIABILITY_MARGIN = 0.05f;

    /**
     * Finds the CSBK message in the list of messages
     */
    private static CSBKMessage getCSBK(List<IMessage> messages)
    {
        CSBKMessage csbk = null;

        for(IMessage message: messages)
        {
            if(message instanceof CSBKMessage csbkMessage)
            {
                csbk = csbkMessage;
            }
        }

        assertNotNull(csbk, "Expected a CSBK message: " + messages);
        return csbk;
    }

    /**
     * Test: an error free CSBK burst produces a valid CSBK message with the expected payload.
     */
    @Test
    void testValidCSBK()
    {
        Random random = new Random(1);
        CorrectedBinaryMessage bptc = createInterleavedBPTC(createCSBKPayload(random));
        CSBKMessage csbk = getCSBK(frame(toSoftSymbols(createBurst(bptc, COLOR_CODE, DataType.CSBK))));
        assertTrue(csbk.isValid());
        assertEquals(BPTC_196_96.extract(bptc).toString(), csbk.getMessage().toString());
    }

    /**
     * Test: when the hard decision decoder converges on an incorrect codeword (miscorrection) and the CSBK fails the
     * CRC check, the factory retries with soft decision decoding, which recovers the transmitted CSBK.
     *
     * Note: some miscorrections differ from the transmitted codeword by a single CRC protected bit, which the
     * CSBK CRC check corrects.  Those trials are verified to have the correct CRC protected content.
     */
    @Test
    void testSoftRetryAfterHardMiscorrection()
    {
        List<Integer> payloadDibits = getPayloadDibits();
        DMRMessageFactory factory = new DMRMessageFactory(new DMRCrcMaskManager(false));
        int crcFailures = 0;

        for(int trial = 0; trial < 3000 && crcFailures < 5; trial++)
        {
            Random random = new Random(1000 + trial);
            CorrectedBinaryMessage bptc = createInterleavedBPTC(createCSBKPayload(random));
            CorrectedBinaryMessage expected = BPTC_196_96.extract(bptc);
            boolean[] burst = createBurst(bptc, COLOR_CODE, DataType.CSBK);
            float[] softSymbols = toSoftSymbols(burst);
            Set<Integer> errors = new HashSet<>();

            while(errors.size() < ERROR_COUNT)
            {
                errors.add(payloadDibits.get(random.nextInt(payloadDibits.size())));
            }

            for(int error: errors)
            {
                addLowReliabilityError(softSymbols, burst, error, LOW_RELIABILITY_MARGIN);
            }

            //Only evaluate trials where the hard decision decoder reports success with an incorrect codeword
            CorrectedBinaryMessage hard = BPTC_196_96.extract(getBPTC(burst));

            if(hard.getCorrectedBitCount() < 0 || hard.toString().equals(expected.toString()))
            {
                continue;
            }

            //Without soft bits, the factory creates the CSBK from the miscorrected payload
            CorrectedBinaryMessage burstMessage = new CorrectedBinaryMessage(288);

            for(int x = 0; x < 288; x++)
            {
                burstMessage.set(x, burst[x]);
            }

            IMessage hardMessage = factory.create(SYNC_PATTERN, burstMessage, null, CACH.getCACH(burstMessage), 0L, 1);
            assertTrue(hardMessage instanceof CSBKMessage, "Trial " + trial);

            if(hardMessage.isValid())
            {
                //The CRC check corrected the miscorrected payload bit, so the CRC protected content must be correct
                assertEquals(expected.getSubMessage(0, 96).toString(),
                        ((CSBKMessage)hardMessage).getMessage().getSubMessage(0, 96).toString(), "Trial " + trial);
                continue;
            }

            crcFailures++;

            //With soft symbols, the factory retries with soft decision decoding and recovers the transmitted CSBK
            CSBKMessage csbk = getCSBK(frame(softSymbols));
            assertTrue(csbk.isValid(), "Trial " + trial);
            assertEquals(expected.toString(), csbk.getMessage().toString(), "Trial " + trial);
        }

        assertEquals(5, crcFailures, "Expected to find 5 hard decision miscorrections that fail the CRC check");
    }
}
