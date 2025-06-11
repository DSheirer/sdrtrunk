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

package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.module.decode.dmr.sync.DMRHardSyncDetector;
import java.nio.ByteBuffer;

/**
 * Processes a stream of dibit symbol decisions to detect DMR sync patterns and produce decoded DMR messages.
 *
 * Note: this symbol processor is primarily used for offline analysis of a captured bit stream recording.
 */
public class DMRHardSymbolProcessor
{
    private final DMRMessageFramer mMessageFramer;
    private final DMRHardSyncDetector mSyncDetector = new DMRHardSyncDetector();
    //Dibit delay line sizing: CACH(12) + MESSAGE_PREFIX(54) + SYNC(24)
    private DibitDelayLine mDibitDelayLine = new DibitDelayLine(90);


    /**
     * Constructs an instance
     * @param messageFramer to receive dibits and sync notifications.
     */
    public DMRHardSymbolProcessor(DMRMessageFramer messageFramer)
    {
        mMessageFramer = messageFramer;
    }

    /**
     * Processes an individual dibit.
     * @param dibit to process
     */
    public void process(Dibit dibit)
    {
        Dibit ejected = mDibitDelayLine.insert(dibit);
        mMessageFramer.receive(ejected);

        if(mSyncDetector.processAndDetect(dibit))
        {
            mMessageFramer.syncDetected(mSyncDetector.getDetectedPattern());
        }
    }

    /**
     * Primary method for streaming decoded symbol byte arrays.
     *
     * @param buffer to process into a stream of dibits for processing.
     */
    public void receive(ByteBuffer buffer)
    {
        for(byte value : buffer.array())
        {
            for(int x = 0; x <= 3; x++)
            {
                process(Dibit.parse(value, x));
            }
        }
    }
}
