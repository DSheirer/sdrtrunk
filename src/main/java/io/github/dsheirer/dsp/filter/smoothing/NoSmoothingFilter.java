package io.github.dsheirer.dsp.filter.smoothing;


public class NoSmoothingFilter extends SmoothingFilter
{
	public NoSmoothingFilter()
	{
		super( null, 3 );
	}

	public float[] filter( float[] data )
	{
		return data;
	}

	public static float[] getCoefficients( int points )
	{
		return null;
	}
	
	public int getPointSize()
	{
		return SMOOTHING_DEFAULT;
	}

	@Override
	public SmoothingType getSmoothingType()
	{
		return SmoothingType.NONE;
	}
}
