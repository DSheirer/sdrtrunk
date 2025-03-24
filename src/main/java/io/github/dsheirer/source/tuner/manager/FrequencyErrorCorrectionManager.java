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
package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerController;
import java.text.DecimalFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors measured frequency error PPM values from the tuner and (when enabled) applies the current
 * error measurement to the tuner controller to adjust the tuner's PPM and align the frequency value.
 */
public class FrequencyErrorCorrectionManager
{
    private final static Logger mLog = LoggerFactory.getLogger(FrequencyErrorCorrectionManager.class);

    private static final double FREQUENCY_CORRECTION_ERROR_THRESHOLD = 0.4;
    private static final long AUTO_CORRECTION_OBSERVATION_PERIOD_MILLISECONDS = 30 * 1000; //30 seconds
    private static final long INITIAL_AUTO_CORRECTION_OBSERVATION_PERIOD_MILLISECONDS = 5 * 1000; //5 seconds
    private long mObservationPeriodStart;
    private double mPPMRequired;
    private boolean mEnabled = true;
    private DecimalFormat mDecimalFormat = new DecimalFormat("0.0");
    private TunerController mTunerController;

    /**
     * Constructs a frequency error correction manager for the specified tuner controller.
     */
    public FrequencyErrorCorrectionManager(TunerController tunerController)
    {
        mTunerController = tunerController;
    }

    /**
     * Dispose of this manager instance and nullify references.
     */
    public void dispose()
    {
        setEnabled(false);
        mTunerController = null;
    }

    /**
     * Enables or disables automatic frequency PPM correction.
     * @param enabled to true to turn on automatic PPM adjustments.
     */
    public void setEnabled(boolean enabled)
    {
        mEnabled = enabled;
    }

    /**
     * Indicates if Auto-PPM is enabled
     */
    public boolean isEnabled()
    {
        return mEnabled;
    }

    /**
     * Resets monitoring
     */
    public void reset()
    {
        mObservationPeriodStart = 0;
        mPPMRequired = 0;
    }

    /**
     * Applies a PPM adjustment value to the tuner's current PPM setting, when auto-correction is enabled.
     */
    private void applyCorrection()
    {
        if(mEnabled && mTunerController != null && mTunerController.hasMeasuredFrequencyError())
        {
            double frequencyCorrection = mTunerController.getFrequencyCorrection();
            frequencyCorrection -= mTunerController.getPPMFrequencyError();

            try
            {
                mLog.info("Auto-Correcting Tuner PPM to [" + mDecimalFormat.format(frequencyCorrection) + "]");
                mTunerController.setFrequencyCorrection(frequencyCorrection);
                reset();
            }
            catch(SourceException se)
            {
                mLog.error("Error while applying automatic frequency error PPM correction value: " + frequencyCorrection);
            }
        }
    }

    /**
     * Updates the monitor with a new PPM frequency error measurement from the tuner.
     */
    public void updatePPM(double ppm)
    {
        if(ppm > FREQUENCY_CORRECTION_ERROR_THRESHOLD)
        {
            if(mObservationPeriodStart == 0)
            {
                mObservationPeriodStart = System.currentTimeMillis() - AUTO_CORRECTION_OBSERVATION_PERIOD_MILLISECONDS +
                        INITIAL_AUTO_CORRECTION_OBSERVATION_PERIOD_MILLISECONDS;
                mPPMRequired = ppm;
            }
            else
            {
                if(mPPMRequired > 0)
                {
                    mPPMRequired = ppm;

                    if(mObservationPeriodStart + AUTO_CORRECTION_OBSERVATION_PERIOD_MILLISECONDS < System.currentTimeMillis())
                    {
                        applyCorrection();
                    }
                }
                else
                {
                    //Opposite polarity ppm value ... restart the timer
                    mObservationPeriodStart = System.currentTimeMillis();
                    mPPMRequired = ppm;
                }
            }
        }
        else if(ppm < -FREQUENCY_CORRECTION_ERROR_THRESHOLD)
        {
            if(mObservationPeriodStart == 0)
            {
                mObservationPeriodStart = System.currentTimeMillis();
                mPPMRequired = ppm;
            }
            else
            {
                if(mPPMRequired < 0)
                {
                    mPPMRequired = ppm;

                    if(mObservationPeriodStart + AUTO_CORRECTION_OBSERVATION_PERIOD_MILLISECONDS < System.currentTimeMillis())
                    {
                        applyCorrection();
                    }
                }
                else
                {
                    //Opposite polarity ppm value ... restart the timer
                    mObservationPeriodStart = System.currentTimeMillis();
                    mPPMRequired = ppm;
                }
            }
        }
        else
        {
            reset();
        }
    }
}
