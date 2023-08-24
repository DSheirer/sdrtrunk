/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.vector.calibrate;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract calibration plugin base class
 */
public abstract class Calibration
{
    protected static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");
    public static final Logger mLog = LoggerFactory.getLogger(Calibration.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(Calibration.class);
    private CalibrationType mType;
    private Implementation mImplementation;

    /**
     * Constructs an instance
     * @param type
     */
    public Calibration(CalibrationType type)
    {
        mType = type;
    }

    public Logger getLogger()
    {
        return mLog;
    }

    /**
     * Type of calibration plugin
     */
    public CalibrationType getType()
    {
        return mType;
    }

    /**
     * Indicates if this plugin is calibrated.
     */
    public boolean isCalibrated()
    {
        return getImplementation() != Implementation.UNCALIBRATED;
    }

    /**
     * Resets calibration status to uncalibrated for the plugin
     */
    public void reset()
    {
        setImplementation(Implementation.UNCALIBRATED);
    }

    /**
     * Optimal implementation for this plugin
     * @return implementation or uncalibrated if this plugin has not yet been calibrated
     */
    public Implementation getImplementation()
    {
        if(mImplementation == null)
        {
            String implementation = mPreferences.get(getType().getPreferenceKey(), Implementation.UNCALIBRATED.name());
            mImplementation = Implementation.valueOf(implementation);
        }

        return mImplementation;
    }

    /**
     * Sets the optimal implementation as determined via calibration.
     * @param implementation to set
     */
    protected void setImplementation(Implementation implementation)
    {
        mImplementation = implementation;
        mPreferences.put(getType().getPreferenceKey(), implementation.name());
    }

    /**
     * Executes calibration for the plugin
     */
    public abstract void calibrate() throws CalibrationException;

    /**
     * Generates an array of floating point samples in the range -1.0 - 1.0
     * @param size of array
     * @return generated samples
     */
    protected float[] getFloatSamples(int size)
    {
        Random random = new Random();

        float[] samples = new float[size];
        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = random.nextFloat() * 2.0f - 1.0f;
        }

        return samples;
    }

    /**
     * Generates an array of floating point samples in the range 0.0 - 1.0
     * @param size of array
     * @return generated samples
     */
    protected float[] getPositiveFloatSamples(int size)
    {
        Random random = new Random();

        float[] samples = new float[size];
        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = random.nextFloat();
        }

        return samples;
    }

    /**
     * Generates an array of floating point samples in the range -1.0 - 1.0
     * @param size of array
     * @return generated samples
     */
    protected short[] getShortSamples(int size)
    {
        Random random = new Random();

        short[] samples = new short[size];
        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = (short)((random.nextFloat() * 2.0f - 1.0f) * Short.MAX_VALUE);
        }

        return samples;
    }
}
