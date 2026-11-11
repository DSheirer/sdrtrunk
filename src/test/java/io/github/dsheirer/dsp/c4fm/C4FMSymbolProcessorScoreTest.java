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

package io.github.dsheirer.dsp.c4fm;

import io.github.dsheirer.module.decode.p25.phase1.P25P1SymbolProcessorC4FM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * JUnit tests for the sync correlation score in the C4FM symbol processor.
 */
public class C4FMSymbolProcessorScoreTest
{
    /**
     * A lagging sync detect on the first symbol after a buffer reload, followed by a coarse timing search that steps
     * all the way left, scores a candidate whose first sync symbol starts just before the start of the buffer.
     */
    @Test
    public void scoreBeforeBufferStart()
    {
        float samplesPerSymbol = 25000f / 4800f;
        P25P1SymbolProcessorC4FM processor = new P25P1SymbolProcessorC4FM(null, null);
        processor.setSamplesPerSymbol(samplesPerSymbol);
        //Sync workspace, sized as in setSamplesPerSymbol(), plus a 1024 sample buffer
        int syncWorkspaceLength = (int)Math.ceil((processor.mSyncDetector.getDibitLength() + 1) * samplesPerSymbol);
        processor.mBuffer = new float[syncWorkspaceLength + 1024];

        //Offset to the final sync symbol that puts the first sync symbol at buffer index -0.3
        double offset = (processor.mSyncDetector.getDibitLength() - 1) * samplesPerSymbol - 0.3;
        assertDoesNotThrow(() -> processor.score(offset, samplesPerSymbol));
    }
}
