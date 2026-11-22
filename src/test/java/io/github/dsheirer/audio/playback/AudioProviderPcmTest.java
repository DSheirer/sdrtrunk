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
package io.github.dsheirer.audio.playback;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for audio provider sample conversion
 */
class AudioProviderPcmTest
{
    @Test
    void outOfRangeSamplesAreLimitedInsteadOfWrapping()
    {
        assertEquals(Short.MAX_VALUE, AudioProvider.toPcm(1.35f));
        assertEquals(-Short.MAX_VALUE, AudioProvider.toPcm(-26.0f));
    }

    @Test
    void inRangeSamplesAreScaled()
    {
        assertEquals(0, AudioProvider.toPcm(0.0f));
        assertEquals(16383, AudioProvider.toPcm(0.5f));
        assertEquals(Short.MAX_VALUE, AudioProvider.toPcm(1.0f));
    }
}
