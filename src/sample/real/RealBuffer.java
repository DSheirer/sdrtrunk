package sample.real;

import java.util.Arrays;

public class RealBuffer
{
	private float[] mSamples;

	/**
	 * Wrapper around float array containing float samples
	 */
	public RealBuffer( float[] samples )
	{
		mSamples = samples;
	}

	/**
	 * Package private to allow subclass construction and subsequent definition
	 * of the sample data
	 */
	RealBuffer()
	{
	}
	
	protected void setSamples( float[] samples )
	{
		mSamples = samples;
	}
	
	/**
	 * Creates a deep copy of the buffer 
	 */
	public RealBuffer copyOf()
	{
		float[] copy = Arrays.copyOf( mSamples, mSamples.length );
		
		return new RealBuffer( copy );
	}
	
	/**
	 * Sample data
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
