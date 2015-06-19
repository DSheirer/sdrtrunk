package instrument.tap.stream;

import sample.complex.ComplexSample;

public class EyeDiagramData
{
	private ComplexSample[] mSamples;
	private float mLeftPoint;
	private float mRightPoint;
	private float mError;
	
	public EyeDiagramData( ComplexSample[] samples, float leftPoint, 
			float rightPoint, float error )
	{
		mSamples = samples;
		mLeftPoint = leftPoint;
		mRightPoint = rightPoint;
		mError = error;
	}
	
	public ComplexSample[] getSamples()
	{
		return mSamples;
	}

	public float getLeftPoint()
	{
		return mLeftPoint;
	}

	public float getRightPoint()
	{
		return mRightPoint;
	}
	
	public float getError()
	{
		return mError;
	}
}
