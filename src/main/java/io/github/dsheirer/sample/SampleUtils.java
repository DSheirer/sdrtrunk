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

package io.github.dsheirer.sample;

import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.sample.complex.ComplexSamples;
import java.util.Random;
import org.apache.commons.math3.util.FastMath;

public class SampleUtils
{
	public static final double TWO_PI = Math.PI * 2.0;

	/**
	 * Converts from an interleaved complex sample array to a sample record with the
	 * I and Q in separate arrays.
	 * @param samples that are interleaved complex samples
	 * @param timestamp of the samples
	 * @return deinterleaved complex samples instance
	 */
	public static ComplexSamples deinterleave(float[] samples, long timestamp)
	{
		float[] i = new float[samples.length / 2];
		float[] q = new float[samples.length / 2];

		int offset;

		for(int x = 0; x < i.length; x++)
		{
			offset = 2 * x;
			i[x] = samples[offset];
			q[x] = samples[offset + 1];
		}

		return new ComplexSamples(i, q, timestamp);
	}

	/**
	 * Converts a complex samples buffer to an array of interleaved (I, Q, etc.) samples
	 * @param complexSamples to interleave
	 * @return interleaved samples array
	 */
	public static float[] interleave(ComplexSamples complexSamples)
	{
		float[] i = complexSamples.i();
		float[] q = complexSamples.q();;

		float[] complex = new float[i.length + q.length];

		for(int x = 0; x < i.length; x++)
		{
			complex[2 * x] = i[x];
			complex[2 * x + 1] = q[x];
		}

		return complex;
	}

	/**
	 * Generates a buffer of complex samples where each sample's vector length is unity (1.0)
	 * @param count of buffer samples
	 * @return complex buffer
	 */
	public static ComplexSamples generateComplex(int count)
	{
		Random random = new Random();

		float[] i = new float[count];
		float[] q = new float[count];

		for(int x = 0; x < i.length; x++)
		{
			//Random angle in radians in range of -PI to + PI
			double angle = random.nextDouble() * TWO_PI - Math.PI;

			i[x] = (float)Math.cos(angle);
			q[x] = (float)Math.sin(angle);
		}

		return new ComplexSamples(i, q, System.currentTimeMillis());
	}

	/**
	 * Generates random complex samples with vector length in range 0.5 to 1.0 (unity).
	 * @param count of samples
	 * @return complex sample buffer
	 */
	public static ComplexSamples generateComplexRandomVectorLength(int count)
	{
		Random random = new Random();
		ComplexSamples unitySamples = generateComplex(count);
		float[] i = unitySamples.i();
		float[] q = unitySamples.q();

		float gain;

		//Apply random gain reduction to samples in range 0.5 to 1.0 gain
		for(int x = 0; x < i.length; x++)
		{
			gain = 0.5f + random.nextFloat() / 2.0f;
			i[x] *= gain;
			q[x] *= gain;
		}

		return new ComplexSamples(i, q, System.currentTimeMillis());
	}

    public static Complex multiply(Complex a, Complex b)
    {
        return new Complex((a.inphase() * b.inphase()) - (a.quadrature() * b.quadrature()),
                ((a.quadrature() * b.inphase()) + (a.inphase() * b.quadrature())));
    }

    public static Complex minus(Complex a, Complex b)
    {
        return new Complex(a.inphase() - b.inphase(), a.quadrature() - b.quadrature());
    }

    public static double magnitude(Complex sample)
    {
        return FastMath.sqrt(magnitudeSquared(sample));
    }

    public static int magnitudeSquared(Complex sample)
    {
        return (int)((sample.inphase() * sample.inphase()) + (sample.quadrature() * sample.quadrature()));
    }
}
