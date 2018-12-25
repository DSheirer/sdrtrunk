/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.source.tuner.test;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Testing tuner that implements an internal oscillator to output a unity gain tone at a specified frequency offset
 * from a configurable center tune frequency and sample rate
 */
public class TestTuner extends Tuner
{
    private final static Logger mLog = LoggerFactory.getLogger(TestTuner.class);
    private static int mInstanceCounter = 1;
    private final int mInstanceID = mInstanceCounter++;

    public TestTuner(UserPreferences userPreferences)
    {
        super("Test Tuner", new TestTunerController(), userPreferences);
    }

    /**
     * Returns the tuner controller cast as a test tuner controller.
     */
    public TestTunerController getTunerController()
    {
        return (TestTunerController)super.getTunerController();
    }

    @Override
    public String getUniqueID()
    {
        return getName() + "-" + mInstanceID;
    }

    @Override
    public TunerClass getTunerClass()
    {
        return TunerClass.TEST_TUNER;
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerClass.TEST_TUNER.getTunerType();
    }

    @Override
    public double getSampleSize()
    {
        return 16.0;
    }
}
