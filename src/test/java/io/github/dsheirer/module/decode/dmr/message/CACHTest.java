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

package io.github.dsheirer.module.decode.dmr.message;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * JUnit tests for the DMR Common Announcement Channel (CACH).
 */
public class CACHTest
{
    /**
     * Test: the CACH payload soft bits are de-interleaved consistently with the payload bits, so that each soft bit
     * corresponds to the payload bit at the same position.
     */
    @Test
    void testPayloadSoftBits()
    {
        Random random = new Random(1);

        for(int trial = 0; trial < 50; trial++)
        {
            CorrectedBinaryMessage burst = new CorrectedBinaryMessage(288);
            float[] softBits = new float[288];

            for(int x = 0; x < 288; x++)
            {
                if(random.nextBoolean())
                {
                    burst.set(x);
                }

                //Unique magnitude per bit position so that a position mismatch can't go undetected
                float magnitude = 1.0f + x / 1000.0f;
                softBits[x] = burst.get(x) ? magnitude : -magnitude;
            }

            CACH cach = CACH.getCACH(burst, softBits);
            BinaryMessage payload = cach.getPayload();
            float[] payloadSoftBits = cach.getPayloadSoftBits();
            assertNotNull(payloadSoftBits);
            assertEquals(17, payloadSoftBits.length);

            for(int x = 0; x < 17; x++)
            {
                assertEquals(payload.get(x), payloadSoftBits[x] > 0, "Trial " + trial + " payload bit " + x);
            }
        }
    }

    /**
     * Test: payload soft bits are not available when the burst soft bits are not provided.
     */
    @Test
    void testNoSoftBits()
    {
        assertNull(CACH.getCACH(new CorrectedBinaryMessage(288)).getPayloadSoftBits());
        assertNull(CACH.getCACH(new CorrectedBinaryMessage(288), null).getPayloadSoftBits());
    }
}
