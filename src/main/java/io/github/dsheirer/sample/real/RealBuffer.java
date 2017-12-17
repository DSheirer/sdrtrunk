package io.github.dsheirer.sample.real;

import io.github.dsheirer.sample.Buffer;

import java.util.Arrays;

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
	 * Creates a deep copy of the buffer
	 */
	public RealBuffer copyOf()
	{
		float[] copy = Arrays.copyOf( mSamples, mSamples.length );
		
		return new RealBuffer( copy );
	}
}
