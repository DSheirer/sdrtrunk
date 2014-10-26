package sample.complex;

import java.util.Arrays;

public class ComplexBuffer
{
	private float[] mSamples;

	/**
	 * Wrapper around float array containing interleaved I/Q samples
	 */
	public ComplexBuffer( float[] samples )
	{
		mSamples = samples;
	}

	/**
	 * Creates a deep copy of the buffer 
	 */
	public ComplexBuffer copyOf()
	{
		float[] copy = Arrays.copyOf( mSamples, mSamples.length );
		
		return new ComplexBuffer( copy );
	}

	/**
	 * Interleaved I/Q float sample array
	 */
	public float[] getSamples()
	{
		return mSamples;
	}

	/**
	 * Cleanup method to nullify all data and references
	 */
	public void dispose()
	{
		mSamples = null;
	}
}
