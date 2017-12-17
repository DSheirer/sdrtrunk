package io.github.dsheirer.dsp.filter.smoothing;

import java.util.Map;
import java.util.Set;

public abstract class SmoothingFilter
{
	public static final int SMOOTHING_MINIMUM = 3;
	public static final int SMOOTHING_MAXIMUM = 29;
	public static final int SMOOTHING_DEFAULT = 9;
	
	public enum SmoothingType
	{
		NONE,
		RECTANGLE,
		TRIANGLE,
		GAUSSIAN;
	}

	private Map<Integer,float[]> mCoefficientMap;
	private float[] mCoefficients;
	private float[] mNewCoefficients;
	private boolean mNewCoefficientsAvailable;
	
	public SmoothingFilter( Map<Integer,float[]> coefficients, int index )
	{
		mCoefficientMap = coefficients;
		
		if( mCoefficientMap != null )
		{
			mCoefficients = mCoefficientMap.get( index );
		}
	}
	
	public abstract SmoothingType getSmoothingType();
	
	public Set<Integer> getPointSizeList()
	{
		return mCoefficientMap.keySet();
	}
	
	public int getPointSize()
	{
		return mCoefficients.length;
	}
	
	public void setPointSize( int pointSize )
	{
		if( mCoefficientMap != null )
		{
			if( mCoefficientMap.containsKey( pointSize ) )
			{
				mNewCoefficients = mCoefficientMap.get( pointSize );
				mNewCoefficientsAvailable = true;
			}
			else
			{
				throw new IllegalArgumentException( "Point size [" + pointSize + 
						"] is not valid in the current coefficient map" );
			}
		}
	}
	
	public float[] filter( float[] data )
	{
		if( mNewCoefficientsAvailable )
		{
			mCoefficients = mNewCoefficients;
			mNewCoefficientsAvailable = false;
		}

		int middle = mCoefficients.length / 2;
		
		float[] filtered = new float[ data.length ];

		int toCopy = middle;
		
		System.arraycopy( data, 0, filtered, 0, toCopy );
		System.arraycopy( data, data.length - toCopy, filtered, filtered.length - toCopy, toCopy );
		
		float accumulator;
		
		for( int x = 0; x < data.length - mCoefficients.length + 1; x++ )
		{
			accumulator = 0.0f;
			
			for( int y = 0; y < mCoefficients.length; y++ )
			{
				accumulator += data[ x + y ] * mCoefficients[ y ];
			}
			
			filtered[ x + middle ] = accumulator;
		}
		
		return filtered;
	}
}
