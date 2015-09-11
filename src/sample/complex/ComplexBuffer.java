package sample.complex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	 * Converts this sample buffer into a list of complex samples
	 */
	public List<Complex> getComplexSamples()
	{
		List<Complex> samples = new ArrayList<Complex>();
		
		for( int x = 0; x < mSamples.length; x += 2 )
		{
			samples.add( new Complex( mSamples[ x ], mSamples[ x + 1 ] ) );
		}
		
		return samples;
	}

	/**
	 * Cleanup method to nullify all data and references
	 */
	public void dispose()
	{
		mSamples = null;
	}
}
