package sample.simplex;

import java.util.Arrays;

public class SimplexBuffer
{
	private float[] mSamples;

	/**
	 * Wrapper around float array containing float samples
	 */
	public SimplexBuffer( float[] samples )
	{
		mSamples = samples;
	}
	
	/**
	 * Creates a deep copy of the buffer 
	 */
	public SimplexBuffer copyOf()
	{
		float[] copy = Arrays.copyOf( mSamples, mSamples.length );
		
		return new SimplexBuffer( copy );
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
