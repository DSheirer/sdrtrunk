package ua.in.smartjava.sample.real;

import java.util.Arrays;

import ua.in.smartjava.sample.Buffer;

public class RealBuffer extends Buffer
{
	/**
	 * Wrapper around float array containing real float samples
	 */
	public RealBuffer( float[] samples )
	{
		super( samples );
	}

	/**
	 * Creates a deep copy of the ua.in.smartjava.buffer
	 */
	public RealBuffer copyOf()
	{
		float[] copy = Arrays.copyOf( mSamples, mSamples.length );
		
		return new RealBuffer( copy );
	}
}
