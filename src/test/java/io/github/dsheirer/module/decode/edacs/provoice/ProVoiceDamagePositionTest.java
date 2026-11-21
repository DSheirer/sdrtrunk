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

package io.github.dsheirer.module.decode.edacs.provoice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests where the damaged voice frames of a ProVoice transmission are counted: start, middle or end.
 */
public class ProVoiceDamagePositionTest
{
    private static ProVoiceDamagePosition of(int... damagedPerFrame)
    {
        ProVoiceDamagePosition position = new ProVoiceDamagePosition();

        for(int damaged : damagedPerFrame)
        {
            position.add(damaged);
        }

        return position;
    }

    @Test
    public void emptyTransmission()
    {
        ProVoiceDamagePosition position = new ProVoiceDamagePosition();
        assertEquals(0, position.getStartFrames());
        assertEquals(0, position.getMiddleFrames());
        assertEquals(0, position.getEndFrames());
        assertEquals(0, position.getDamagedStart());
        assertEquals(0, position.getDamagedMiddle());
        assertEquals(0, position.getDamagedEnd());
    }

    @Test
    public void shortTransmissionIsAllStart()
    {
        //Four frames: nothing is counted as middle or end
        ProVoiceDamagePosition position = of(4, 4, 1, 0);
        assertEquals(4, position.getStartFrames());
        assertEquals(0, position.getMiddleFrames());
        assertEquals(0, position.getEndFrames());
        assertEquals(9, position.getDamagedStart());
        assertEquals(0, position.getDamagedMiddle());
        assertEquals(0, position.getDamagedEnd());
    }

    @Test
    public void framesAfterTheStartAreTheEndUntilThereAreMoreThanSix()
    {
        //Six start frames then three more: the three are the end, there is no middle and nothing is counted twice
        ProVoiceDamagePosition position = of(1, 1, 1, 1, 1, 1, 2, 3, 4);
        assertEquals(6, position.getStartFrames());
        assertEquals(0, position.getMiddleFrames());
        assertEquals(3, position.getEndFrames());
        assertEquals(6, position.getDamagedStart());
        assertEquals(0, position.getDamagedMiddle());
        assertEquals(9, position.getDamagedEnd());
    }

    @Test
    public void longTransmissionSplitsIntoStartMiddleAndEnd()
    {
        //20 frames: 6 start (4 each), 8 middle (1 each), 6 end (2 each)
        ProVoiceDamagePosition position = of(4, 4, 4, 4, 4, 4, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2);
        assertEquals(6, position.getStartFrames());
        assertEquals(8, position.getMiddleFrames());
        assertEquals(6, position.getEndFrames());
        assertEquals(24, position.getDamagedStart());
        assertEquals(8, position.getDamagedMiddle());
        assertEquals(12, position.getDamagedEnd());
        assertEquals("ProVoice damage by position: first 0.48 s 24 of 24, middle 8 of 32, last 0.48 s 12 of 24 voice frames",
            position.describe());
    }

    @Test
    public void endIsTheLastSixFramesWhateverTheLength()
    {
        //A length that is not a multiple of six, so the ring has wrapped part of the way round
        ProVoiceDamagePosition position = new ProVoiceDamagePosition();

        for(int x = 0; x < 6; x++)
        {
            position.add(0);
        }

        for(int x = 0; x < 11; x++)
        {
            position.add(3);
        }

        //The last six frames: 0, 0, 4, 0, 0, 1
        position.add(0);
        position.add(0);
        position.add(4);
        position.add(0);
        position.add(0);
        position.add(1);

        assertEquals(11, position.getMiddleFrames());
        assertEquals(6, position.getEndFrames());
        assertEquals(0, position.getDamagedStart());
        assertEquals(33, position.getDamagedMiddle());
        assertEquals(5, position.getDamagedEnd());
    }

    @Test
    public void resetStartsANewTransmission()
    {
        ProVoiceDamagePosition position = of(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4);
        position.reset();
        position.add(2);
        assertEquals(1, position.getStartFrames());
        assertEquals(0, position.getEndFrames());
        assertEquals(2, position.getDamagedStart());
        assertEquals(0, position.getDamagedEnd());
        assertEquals(0, position.getDamagedMiddle());
    }
}
