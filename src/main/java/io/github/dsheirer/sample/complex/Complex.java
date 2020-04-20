/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.sample.complex;

import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * Complex sample and related utility methods
 */
public class Complex implements Serializable
{
    private final static Logger mLog = LoggerFactory.getLogger(Complex.class);

    private static final long serialVersionUID = 1L;

    private float mLeft;
    private float mRight;

    public Complex(float left, float right)
    {
        mLeft = left;
        mRight = right;
    }

    public Complex()
    {
        this(0.0f, 0.0f);
    }

    public void setInphase(float inphase)
    {
        mLeft = inphase;
    }

    public void setQuadrature(float quadrature)
    {
        mRight = quadrature;
    }

    public void setValues(float inphase, float quadrature)
    {
        mLeft = inphase;
        mRight = quadrature;
    }

    public void setValues(Complex sampleToCopy)
    {
        mLeft = sampleToCopy.left();
        mRight = sampleToCopy.right();
    }

    public Complex copy()
    {
        return new Complex(mLeft, mRight);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("I:");
        sb.append(mLeft);
        sb.append(" Q:");
        sb.append(mRight);

        return sb.toString();
    }

    /**
     * Returns a new sample representing the conjugate of this one
     */
    public Complex conjugate()
    {
        return new Complex(mLeft, -mRight);
    }

    /**
     * Multiplies this sample by the scalor value
     */
    public void multiply(float scalor)
    {
        mLeft *= scalor;
        mRight *= scalor;
    }

    public static Complex multiply(Complex sample, float scalor)
    {
        return new Complex(sample.left() * scalor,
            sample.right() * scalor);
    }

    /**
     * Calculates the inphase component of multiplying two complex numbers, A and B.
     */
    public static float multiplyInphase(float inphaseA, float quadratureA, float inphaseB, float quadratureB)
    {
        return (inphaseA * inphaseB) - (quadratureA * quadratureB);
    }

    /**
     * Calculates the quadrature component of multiplying two complex numbers, A and B.
     */
    public static float multiplyQuadrature(float inphaseA, float quadratureA, float inphaseB, float quadratureB)
    {
        return (quadratureA * inphaseB) + (inphaseA * quadratureB);
    }

    /**
     * Multiplies this sample by the multiplier sample
     */
    public void multiply(Complex multiplier)
    {
        float inphase = multiplyInphase(inphase(), quadrature(),
            multiplier.inphase(), multiplier.quadrature());
        float quadrature = multiplyQuadrature(inphase(), quadrature(),
            multiplier.inphase(), multiplier.quadrature());

        mLeft = inphase;
        mRight = quadrature;
    }

    /**
     * Multiplies both samples returning a new sample with the results
     */
    public static Complex multiply(Complex sample1, Complex sample2)
    {
        float inphase = multiplyInphase(sample1.inphase(), sample1.quadrature(),
            sample2.inphase(), sample2.quadrature());
        float quadrature = multiplyQuadrature(sample1.inphase(), sample1.quadrature(),
            sample2.inphase(), sample2.quadrature());

        return new Complex(inphase, quadrature);
    }

    public static Complex multiply(Complex sample,
                                   Float inphase, Float quadrature)
    {
        float i = multiplyInphase(sample.inphase(), sample.quadrature(),
            inphase, quadrature);
        float q = multiplyQuadrature(sample.inphase(), sample.quadrature(),
            inphase, quadrature);

        return new Complex(i, q);
    }

    public static Complex multiply(Float inphase, Float quadrature, Complex sample)
    {
        float i = multiplyInphase(inphase, quadrature, sample.inphase(), sample.quadrature());
        float q = multiplyQuadrature(inphase, quadrature, sample.inphase(), sample.quadrature());

        return new Complex(i, q);
    }

    public static Complex multiply(float inphaseA, float quadratureA, float inphaseB, float quadratureB)
    {
        float i = multiplyInphase(inphaseA, quadratureA, inphaseB, quadratureB);
        float q = multiplyQuadrature(inphaseA, quadratureA, inphaseB, quadratureB);

        return new Complex(i, q);
    }

    /**
     * Adds the adder sample value to this sample
     */
    public void add(Complex adder)
    {
        mLeft += adder.left();
        mRight += adder.right();
    }

    /**
     * Adds the two complex samples returning a new complex sample with the result
     */
    public static Complex add(Complex first, Complex second)
    {
        return new Complex(first.left() + second.left(),
            first.right() + second.right());
    }

    /**
     * Adds the two complex samples returning a new complex sample with the result
     */
    public static Complex subtract(Complex first, Complex second)
    {
        return new Complex(first.left() - second.left(),
            first.right() - second.right());
    }

    /**
     * Magnitude of this sample
     */
    public float magnitude()
    {
        return (float)FastMath.sqrt(magnitudeSquared());
    }

    public static float magnitude(float inphase, float quadrature)
    {
        return (float) FastMath.sqrt((inphase * inphase) + (quadrature * quadrature));
    }

    /**
     * Magnitude squared of this sample
     */
    public float magnitudeSquared()
    {
        return norm();
    }

    /**
     * Norm of this sample = ( i * i ) + ( q * q )
     */
    public float norm()
    {
        return (float)((inphase() * inphase()) +
            (quadrature() * quadrature()));
    }

    /**
     * Returns the vector length to 1 (unit circle)
     */
    public void normalize()
    {
        float magnitude = magnitude();

        if(magnitude != 0)
        {
            multiply((float)(1.0f / magnitude()));
        }
    }

    /**
     * Maintains the vector magnitude close to 1.0 (unit circle) when the magnitude is already
     * close to 1.0, avoiding square root multiplication.  If you need a faster way to set the
     * magnitude to close to 1.0 without the square root of normalize(), use fastNormalize2().
     */
    public void fastNormalize()
    {
        multiply((float)(1.9999f - magnitudeSquared()));
    }

    /**
     * Sets vector magnitude to close to 1.0f without the magnitude square root operation
     */
    public void normalizeFast()
    {
        float inphaseAbsolute = FastMath.abs(inphase());
        float quadratureAbsolute = FastMath.abs(quadrature());

        float gain;

        if(inphaseAbsolute > quadratureAbsolute)
        {
            gain = 1.0f / (inphaseAbsolute + (0.4f * quadratureAbsolute));
        }
        else
        {
            gain = 1.0f / (quadratureAbsolute + (0.4f * inphaseAbsolute));
        }

        multiply(gain);
    }

    public float left()
    {
        return mLeft;
    }

    public float right()
    {
        return mRight;
    }

    public float inphase()
    {
        return mLeft;
    }

    /**
     * Absolute value of in-phase component
     */
    public float inPhaseAbsolute()
    {
        return FastMath.abs(mLeft);
    }

    public float quadrature()
    {
        return mRight;
    }

    /**
     * Absolute value of quadrature component
     */
    public float quadratureAbsolute()
    {
        return FastMath.abs(mRight);
    }

    public float x()
    {
        return mLeft;
    }

    public float y()
    {
        return mRight;
    }

    public float real()
    {
        return mLeft;
    }

    /**
     * Absolute value of real component
     */
    public float realAbsolute()
    {
        return FastMath.abs(mLeft);
    }

    public float imaginary()
    {
        return mRight;
    }

    /**
     * Absolute value of imaginary component
     */
    public float imaginaryAbsolute()
    {
        return FastMath.abs(mRight);
    }

    /**
     * Returns the greater absolute value between left and right values
     */
    public float maximumAbsolute()
    {
        return FastMath.max(FastMath.abs(mLeft), FastMath.abs(mRight));
    }

    /**
     * Creates a new complex sample representing the angle with unit circle
     * magnitude
     *
     * @param angle in radians
     * @return
     */
    public static Complex fromAngle(double angle)
    {
        return new Complex((float)FastMath.cos(angle), (float)FastMath.sin(angle));
    }

    /**
     * Sets this complex instance with the specified angle in radians.
     * @param angle in radians
     */
    public void setAngle(double angle)
    {
        setInphase((float)FastMath.cos(angle));
        setQuadrature((float)FastMath.sin(angle));
    }

    /**
     * Angle of this sample in radians
     */
    public float angle()
    {
        return (float)FastMath.atan2(y(), x());
    }

    /**
     * Angle in degrees
     * @return angle value (-180.0 to 180.0) relative to engineering 0 degrees (ie due East)
     */
    public double angleDegrees()
    {
        return FastMath.toDegrees(FastMath.atan2(y(), x()));
    }

    /**
     * Angle in polar degrees relative to true North
     */
    public double polarAngleDegrees()
    {
        double angle = FastMath.toDegrees(FastMath.atan2(y(), x()));
        return (450.0 - angle) % 360.0;
    }

    /**
     * Constrains the i and q quantities to +/- value.
     *
     * @param value - maximum absolute value
     */
    public void clip(float value)
    {
        if(mLeft > value)
        {
            mLeft = value;
        }
        else if(mLeft < -value)
        {
            mLeft = -value;
        }

        if(mRight > value)
        {
            mRight = value;
        }
        else if(mRight < -value)
        {
            mRight = -value;
        }
    }

    /**
     * Angle of this sample in degrees
     */
    public float polarAngle()
    {
        return (float)FastMath.toDegrees(angle());
    }

    /**
     * Provides an approximate magnitude value for this sample.
     */
    public float envelope()
    {
        return envelope(mLeft, mRight);
    }

    public static float envelope(float inphase, float quadrature)
    {
        float inphaseAbsolute = FastMath.abs(inphase);
        float quadratureAbsolute = FastMath.abs(quadrature);

        if(inphaseAbsolute > quadratureAbsolute)
        {
            return inphaseAbsolute + (0.4f * quadratureAbsolute);
        }
        else
        {
            return quadratureAbsolute + (0.4f * inphaseAbsolute);
        }
    }
}
