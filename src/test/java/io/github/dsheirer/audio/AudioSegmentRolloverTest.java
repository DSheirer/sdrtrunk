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

package io.github.dsheirer.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.alias.AliasList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests audio segment rollover at the maximum audio segment length.
 */
public class AudioSegmentRolloverTest
{
    private static final float[] ONE_SECOND_OF_AUDIO = new float[8000];

    @Test
    public void defaultMaximumSegmentLengthIsOneMinute()
    {
        AudioModule module = new AudioModule(new AliasList("test"), Collections.emptyList());
        List<AudioSegment> segments = new ArrayList<>();
        module.setAudioSegmentListener(segments::add);

        for(int seconds = 0; seconds < 60; seconds++)
        {
            module.addAudio(ONE_SECOND_OF_AUDIO);
        }

        assertEquals(1, segments.size(), "A full minute of audio should fit in a single segment");

        module.addAudio(ONE_SECOND_OF_AUDIO);

        assertEquals(2, segments.size(), "Audio beyond one minute should roll over to a new segment");
        assertEquals(60_000, segments.get(0).getDuration());
        assertTrue(segments.get(0).isComplete(), "First segment should be closed");
        assertTrue(segments.get(1).isLinkedTo(segments.get(0)), "New segment should link to the first segment");
    }
}
