/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsherer.sdrplay.test;

import com.github.dsheirer.sdrplay.SDRplay;
import com.github.dsheirer.sdrplay.parameter.tuner.GainReduction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SDRplayTest
{
    public static Logger mlog = LoggerFactory.getLogger(SDRplayTest.class);

    public SDRplayTest()
    {
    }

    @Test
    public void loadLibrary()
    {
        mlog.info("Loading SDRplay API");
        SDRplay sdrplay = new SDRplay();
        assertTrue(sdrplay.isAvailable(), "Library is not available");
    }

    @Test
    @DisplayName("Ensure each Gain Reduction entry has the specified number of gain indices")
    void testGainReductionIndices()
    {
        for(GainReduction gainReduction: GainReduction.values())
        {
            if(gainReduction != GainReduction.UNKNOWN)
            {
                for(int x = GainReduction.MIN_GAIN_INDEX; x <= GainReduction.MAX_GAIN_INDEX; x++)
                {
                    int grValue = gainReduction.getGainReduction(x);
                    assertTrue(0 <= grValue, "Gain reduction value greater than 0");
                    assertTrue(59 >= grValue, "Gain reduction value less than 60");

                    int lnaValue = gainReduction.getLnaState(x);
                    assertTrue(0 <= lnaValue, "LNA value greater than 0");
                    assertTrue(27 >= lnaValue, "LNA value less than 60");
                }
            }
        }
    }

}
