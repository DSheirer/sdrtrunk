/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.dsp.filter;

import org.apache.commons.math3.util.FastMath;

/***************************************************************************
 *   Copyright (C) 2011 by Paul Lutus                                      *
 *   lutusp@arachnoid.com                                                  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/
// http://en.wikipedia.org/wiki/Digital_biquad_filter

final public class IIRBiQuadraticFilter {

    public enum Type {

        BANDPASS, LOWPASS, HIGHPASS, NOTCH, PEAK, LOWSHELF, HIGHSHELF
    };
    double a0, a1, a2, b0, b1, b2;
    double x1, x2, y, y1, y2;
    double gain_abs;
    Type type;
    double center_freq, sample_rate, Q, gainDB;

    public IIRBiQuadraticFilter() {
    }

    public IIRBiQuadraticFilter(Type type, double center_freq, double sample_rate, double Q, double gainDB) {
        configure(type, center_freq, sample_rate, Q, gainDB);
    }

    // constructor without gain setting
    public IIRBiQuadraticFilter(Type type, double center_freq, double sample_rate, double Q) 
    {
        configure(type, center_freq, sample_rate, Q, 0);
    }

    public void reset() 
    {
        x1 = x2 = y1 = y2 = 0;
    }

    public double frequency() 
    {
        return center_freq;
    }

    public void configure(Type type, double center_freq, double sample_rate, double Q, double gainDB) 
    {
        reset();
        Q = (Q == 0) ? 1e-9 : Q;
        this.type = type;
        this.sample_rate = sample_rate;
        this.Q = Q;
        this.gainDB = gainDB;
        reconfigure(center_freq);
    }

    public void configure(Type type, double center_freq, double sample_rate, double Q) 
    {
        configure(type, center_freq, sample_rate, Q, 0);
    }

    // allow parameter change while running
    public void reconfigure(double cf) {
        center_freq = cf;
        // only used for peaking and shelving filters
        gain_abs = FastMath.pow(10, gainDB / 40);
        double omega = 2 * FastMath.PI * cf / sample_rate;
        double sn = FastMath.sin(omega);
        double cs = FastMath.cos(omega);
        double alpha = sn / (2 * Q);
        double beta = FastMath.sqrt(gain_abs + gain_abs);
        switch (type) {
            case BANDPASS:
                b0 = alpha;
                b1 = 0;
                b2 = -alpha;
                a0 = 1 + alpha;
                a1 = -2 * cs;
                a2 = 1 - alpha;
                break;
            case LOWPASS:
                b0 = (1 - cs) / 2;
                b1 = 1 - cs;
                b2 = (1 - cs) / 2;
                a0 = 1 + alpha;
                a1 = -2 * cs;
                a2 = 1 - alpha;
                break;
            case HIGHPASS:
                b0 = (1 + cs) / 2;
                b1 = -(1 + cs);
                b2 = (1 + cs) / 2;
                a0 = 1 + alpha;
                a1 = -2 * cs;
                a2 = 1 - alpha;
                break;
            case NOTCH:
                b0 = 1;
                b1 = -2 * cs;
                b2 = 1;
                a0 = 1 + alpha;
                a1 = -2 * cs;
                a2 = 1 - alpha;
                break;
            case PEAK:
                b0 = 1 + (alpha * gain_abs);
                b1 = -2 * cs;
                b2 = 1 - (alpha * gain_abs);
                a0 = 1 + (alpha / gain_abs);
                a1 = -2 * cs;
                a2 = 1 - (alpha / gain_abs);
                break;
            case LOWSHELF:
                b0 = gain_abs * ((gain_abs + 1) - (gain_abs - 1) * cs + beta * sn);
                b1 = 2 * gain_abs * ((gain_abs - 1) - (gain_abs + 1) * cs);
                b2 = gain_abs * ((gain_abs + 1) - (gain_abs - 1) * cs - beta * sn);
                a0 = (gain_abs + 1) + (gain_abs - 1) * cs + beta * sn;
                a1 = -2 * ((gain_abs - 1) + (gain_abs + 1) * cs);
                a2 = (gain_abs + 1) + (gain_abs - 1) * cs - beta * sn;
                break;
            case HIGHSHELF:
                b0 = gain_abs * ((gain_abs + 1) + (gain_abs - 1) * cs + beta * sn);
                b1 = -2 * gain_abs * ((gain_abs - 1) + (gain_abs + 1) * cs);
                b2 = gain_abs * ((gain_abs + 1) + (gain_abs - 1) * cs - beta * sn);
                a0 = (gain_abs + 1) - (gain_abs - 1) * cs + beta * sn;
                a1 = 2 * ((gain_abs - 1) - (gain_abs + 1) * cs);
                a2 = (gain_abs + 1) - (gain_abs - 1) * cs - beta * sn;
                break;
        }
        // prescale flter constants
        b0 /= a0;
        b1 /= a0;
        b2 /= a0;
        a1 /= a0;
        a2 /= a0;
    }

    // provide a static amplitude result for testing
    public double result(double f) {
        double phi = FastMath.pow((FastMath.sin(2.0 * FastMath.PI * f / (2.0 * sample_rate))), 2.0);
        return (FastMath.pow(b0 + b1 + b2, 2.0) - 4.0 * (b0 * b1 + 4.0 * b0 * b2 + b1 * b2) * phi + 16.0 * b0 * b2 * phi * phi) / (FastMath.pow(1.0 + a1 + a2, 2.0) - 4.0 * (a1 + 4.0 * a2 + a1 * a2) * phi + 16.0 * a2 * phi * phi);
    }

    // provide a static decibel result for testing
    public double log_result(double f) 
    {
        double r;
        try {
            r = 10 * FastMath.log10(result(f));
        } catch (Exception e) {
            r = -100;
        }
        if(Double.isInfinite(r) || Double.isNaN(r)) {
            r = -100;
        }
        return r;
    }

    // return the constant set for this filter
    public double[] constants() 
    {
        return new double[]{b0, b1, b2, a1, a2};
    }

    // perform one filtering step
    public double filter(double x) 
    {
        y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
        x2 = x1;
        x1 = x;
        y2 = y1;
        y1 = y;
        return (y);
    }
}
