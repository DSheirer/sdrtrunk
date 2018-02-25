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
package io.github.dsheirer.dsp.psk;

import io.github.dsheirer.sample.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EarlyLateDetector
{
    private static final float PI_4_ROTATION = (float)(Math.PI / 4.0);
    private final static Logger mLog = LoggerFactory.getLogger(EarlyLateDetector.class);

    /**
     * Symbol timing error detector designed to optimize the sample timing point such that the current symbol's
     * inphase and quadrature approach the optimal 45 degree (.707) sampling instant.
     */
    public EarlyLateDetector()
    {
    }

    public float getError(Complex early, Complex current, Complex late)
    {
        float inphaseError = getError(early.inphase(), current.inphase(), late.inphase());
        float quadratureError = getError(early.quadrature(), current.quadrature(), late.quadrature());
        float error = inphaseError + quadratureError;

        mLog.debug("I:" + inphaseError + " Q:" + quadratureError + " E:" + error);
        return error;
//        return getError(early.inphase(), current.inphase(), late.inphase()) -
//               getError(early.quadrature(), current.quadrature(), late.quadrature());
    }

    private float getError(float early, float current, float late)
    {
        if(early < current) //Ascending slope
        {
            if(current > 0.0)
            {
                //Positive hemisphere, ascending slope
                return PI_4_ROTATION - current;
            }
            else
            {
                //Negative hemisphere, ascending slope
                return -PI_4_ROTATION - current;
            }
        }
        else //Descending slope
        {
            if(current > 0.0)
            {
                //Positive hemisphere, descending slope
                return current - PI_4_ROTATION;
            }
            else
            {
                //Negative hemisphere, descending slope
                return current + PI_4_ROTATION;
            }
        }
    }
}
