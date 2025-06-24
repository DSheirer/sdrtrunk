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

package io.github.dsheirer.dsp.filter.decimate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating real and complex decimation filters.
 */
public class DecimationFilterFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(DecimationFilterFactory.class);
    private static final int[] SUPPORTED_RATES = new int[]{0,2,4,8,16,32,64,128,256,512,1024};

    /**
     * Creates a real-valued decimation filter for float array sample buffers providing greater than 100 dB of
     * attenuation for out of band signal aliases.  Supports power of 2 decimation rates up to 1024.
     *
     * @param decimationRate requested @see SUPPORTED_RATES
     * @return constructed decimation filter
     */
    public static IRealDecimationFilter getRealDecimationFilter(int decimationRate)
    {
        switch(decimationRate)
        {
            case 0:
            case 1:
                return new RealDecimateX0Filter();
            case 2:
                return new RealDecimateX2Filter();
            case 4:
                return new RealDecimateX4Filter();
            case 8:
                return new RealDecimateX8Filter();
            case 16:
                return new RealDecimateX16Filter();
            case 32:
                return new RealDecimateX32Filter();
            case 64:
                return new RealDecimateX64Filter();
            case 128:
                return new RealDecimateX128Filter();
            case 256:
                return new RealDecimateX256Filter();
            case 512:
                return new RealDecimateX512Filter();
            case 1024:
                return new RealDecimateX1024Filter();
            default:
                throw new IllegalArgumentException("Unsupported decimation rate: " + decimationRate +
                        ".  Supported decimation rates are:" + SUPPORTED_RATES);
        }
    }

    /**
     * Finds the greatest factor of 2 decimation rate that is less than the requested rate.
     * @param requestedRate for decimation
     * @return factor of 2 decimation rate that is closest to the requested rate
     */
    public static int getDecimationRate(int requestedRate)
    {
        if(requestedRate < 0)
        {
            throw new IllegalArgumentException("Requested decimation rate must be greater than 2");
        }

        for(int x = SUPPORTED_RATES.length - 1; x >= 0; x--)
        {
            if(SUPPORTED_RATES[x] < requestedRate)
            {
                return SUPPORTED_RATES[x];
            }
        }

        return 2;
    }
}
