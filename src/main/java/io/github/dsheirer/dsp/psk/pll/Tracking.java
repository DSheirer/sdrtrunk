/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.psk.pll;

public enum Tracking
{
//    SEARCHING(50.0, 0.01, Math.PI),
//    COARSE(100.0, 0.001, 0.01),
//    FINE(150.0, 0.0001, 0.001),
//    LOCKED(200.0, 0.0, 0.0001);
    SEARCHING(100.0, 0.4, Math.PI),
    COARSE(200.0, 0.2, 0.4),
    FINE(400.0, 0.1, 0.2),
    LOCKED(600.0, 0.0, 0.1);

    private double mLoopBandwidth;
    private double mThresholdMinimum;
    private double mThresholdMaximum;

    Tracking(double loopBandwidth, double thresholdMinimum, double thresholdMaximum)
    {
        mLoopBandwidth = loopBandwidth;
        mThresholdMinimum = thresholdMinimum;
        mThresholdMaximum = thresholdMaximum;
    }

    /**
     * Loop bandwidth setting
     */
    public double getLoopBandwidth()
    {
        return mLoopBandwidth;
    }

    /**
     * Minimum error value for this entry
     */
    public double getThresholdMinimum()
    {
        return mThresholdMinimum;
    }

    /**
     * Maximum error value for this entry
     */
    public double getThresholdMaximum()
    {
        return mThresholdMaximum;
    }

    /**
     * Indicates if the error value is within the range for this entry
     */
    public boolean contains(double standardDeviation)
    {
        return getThresholdMinimum() <= standardDeviation && standardDeviation < getThresholdMaximum();
    }
}
