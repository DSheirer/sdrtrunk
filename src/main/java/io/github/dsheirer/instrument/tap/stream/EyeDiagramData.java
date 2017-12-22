package io.github.dsheirer.instrument.tap.stream;


public class EyeDiagramData
{
	private float[] mSamplesInphase;
	private float[] mSamplesQuadrature;
	private float mLeftPoint;
	private float mRightPoint;
	private float mError;
	
	public EyeDiagramData( float[] samplesInphase, float[] samplesQuadrature, 
			float leftPoint, float rightPoint, float error )
	{
		mSamplesInphase = samplesInphase;
		mSamplesQuadrature = samplesQuadrature;
		mLeftPoint = leftPoint;
		mRightPoint = rightPoint;
		mError = error;
	}
	
	public float[] getInphaseSamples()
	{
		return mSamplesInphase;
	}

	public float[] getQuadratureSamples()
	{
		return mSamplesQuadrature;
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
