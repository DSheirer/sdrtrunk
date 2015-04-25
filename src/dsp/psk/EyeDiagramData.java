package dsp.psk;

import sample.complex.ComplexSample;

public class EyeDiagramData
{
	private ComplexSample[] mSamples;
	private float mSamplePoint;
	private float mMiddlePoint;
	
	public EyeDiagramData( ComplexSample[] samples, float samplePoint, float middlePoint )
	{
		mSamples = samples;
		
		mSamplePoint = samplePoint;
		mMiddlePoint = middlePoint;
	}
	
	public ComplexSample[] getSamples()
	{
		return mSamples;
	}
	
	public float getSamplePoint()
	{
		return mSamplePoint;
	}
	
	public float getMiddlePoint()
	{
		return mMiddlePoint;
	}
}
