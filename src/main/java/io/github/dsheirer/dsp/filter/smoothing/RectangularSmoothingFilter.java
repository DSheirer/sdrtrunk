package io.github.dsheirer.dsp.filter.smoothing;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RectangularSmoothingFilter extends SmoothingFilter
{
	private static Map<Integer,float[]> mMap = new HashMap<>();

	static
	{
		mMap.put( 3, getCoefficients( 3 ) );
		mMap.put( 5, getCoefficients( 5 ) );
		mMap.put( 7, getCoefficients( 7 ) );
		mMap.put( 9, getCoefficients( 9 ) );
		mMap.put( 11, getCoefficients( 11 ) );
		mMap.put( 13, getCoefficients( 13 ) );
		mMap.put( 15, getCoefficients( 15 ) );
		mMap.put( 17, getCoefficients( 17 ) );
		mMap.put( 19, getCoefficients( 19 ) );
		mMap.put( 21, getCoefficients( 21 ) );
		mMap.put( 23, getCoefficients( 23 ) );
		mMap.put( 25, getCoefficients( 25 ) );
		mMap.put( 27, getCoefficients( 27 ) );
		mMap.put( 29, getCoefficients( 29 ) );
		mMap.put( 31, getCoefficients( 31 ) );
	}
	
	public RectangularSmoothingFilter()
	{
		super( mMap, 3 );
	}
	
	public static float[] getCoefficients( int points )
	{
		float[] coefficients = new float[ points ];
		
		Arrays.fill( coefficients, 1.0f / (float)points );
		
		return coefficients;
	}
	
	@Override
	public SmoothingType getSmoothingType()
	{
		return SmoothingType.RECTANGLE;
	}
}
