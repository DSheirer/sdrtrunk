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

package io.github.dsheirer.module.decode.nxdn.layer1.sync.control;

/**
 * Scalar implementation of long control soft sync detector.
 */
public class NXDNControlSoftSyncDetectorScalar extends NXDNControlSoftSyncDetector
{
    /**
     * Processes the demodulated soft symbol value and returns a correlation value against the preceding 24 soft
     * symbols that include this recent soft symbol.
     *
     * @return correlation score.
     */
    public float calculate()
    {
        float symbol;
        float score = 0;

        for(int x = 0; x < 22; x++)
        {
            symbol = mSymbols[mSymbolPointer + x];
            score += SYMBOLS[x] * symbol;
        }

        return score;
    }
}
