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

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.id.priority.Priority;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AudioModuleMuteTest
{
    private static class TestAudioModule extends AbstractAudioModule
    {
        TestAudioModule()
        {
            super(new AliasList("test"));
        }

        @Override
        public void reset()
        {
        }

        @Override
        public void start()
        {
        }
    }

    @Test
    public void mutingEndsTheSegmentInProgressAndMarksItNotMonitored()
    {
        TestAudioModule module = new TestAudioModule();
        AudioSegment playing = module.getAudioSegment();
        assertEquals(Priority.DEFAULT_PRIORITY, playing.monitorPriorityProperty().get());

        module.setMuted(true);

        assertTrue(module.isMuted());
        assertTrue(playing.isDoNotMonitor());
        assertTrue(playing.completeProperty().get());
    }

    @Test
    public void segmentsCreatedWhileMutedAreNotMonitoredButStillRecordable()
    {
        TestAudioModule module = new TestAudioModule();
        module.setMuted(true);

        AudioSegment segment = module.getAudioSegment();
        assertTrue(segment.isDoNotMonitor());
        assertFalse(segment.completeProperty().get());
    }

    @Test
    public void unmutingStartsANewSegmentWithNormalPriority()
    {
        TestAudioModule module = new TestAudioModule();
        module.setMuted(true);
        AudioSegment muted = module.getAudioSegment();

        module.setMuted(false);

        assertFalse(module.isMuted());
        assertTrue(muted.completeProperty().get());
        AudioSegment next = module.getAudioSegment();
        assertNotSame(muted, next);
        assertFalse(next.isDoNotMonitor());
    }
}
