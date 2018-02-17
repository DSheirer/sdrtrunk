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
    SEARCHING(50.0, Math.PI),
    COARSE(100.0, 0.01),
    FINE(150.0, 0.001),
    LOCKED(200.0, 0.0001);

    private double mLoopBandwidth;
    private double mVarianceThreshold;

    Tracking(double loopBandwidth, double varianceThreshold)
    {
        mLoopBandwidth = loopBandwidth;
        mVarianceThreshold = varianceThreshold;
    }

    public double getLoopBandwidth()
    {
        return mLoopBandwidth;
    }

    public double getVarianceThreshold()
    {
        return mVarianceThreshold;
    }

    public boolean meetsThreshold(double variance)
    {
        return variance < getVarianceThreshold();
    }
}
