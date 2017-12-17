package io.github.dsheirer.dsp.filter.iir;

import io.github.dsheirer.sample.real.RealBuffer;

public class DeemphasisFilter
{
	private static final float MAX_SAMPLE_VALUE = 0.95f;
	private float mAlpha;
	private float mGain;
	private float mPrevious = 0.0f;
	
	public DeemphasisFilter( float sampleRate, float cutoff, float gain )
	{
		mAlpha = (float)Math.exp( -2.0 * Math.PI * cutoff * ( 1.0 / sampleRate ) );
		mGain = gain;
	}
	
	public float filter( float sample )
	{
		mPrevious = sample + ( mAlpha * mPrevious );
		
		return declip( mPrevious * mGain );
	}
	
	private float declip( float value )
	{
		if( value > MAX_SAMPLE_VALUE )
		{
			return MAX_SAMPLE_VALUE;
		}
		else if( value < -MAX_SAMPLE_VALUE )
		{
			return -MAX_SAMPLE_VALUE;
		}
		
		else return value;
	}
	
	public float[] filter( float[] samples )
	{
		for( int x = 0; x < samples.length; x++ )
		{
			samples[ x ] = filter( samples[ x ] );
		}
		
		return samples;
	}
	
	public RealBuffer filter( RealBuffer buffer )
	{
		float[] samples = buffer.getSamples();
		
		for( int x = 0; x < samples.length; x++ )
		{
			samples[ x ] = filter( samples[ x ] );
		}

		return buffer;
	}
}
