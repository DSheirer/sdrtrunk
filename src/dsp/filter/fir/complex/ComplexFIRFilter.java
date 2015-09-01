package dsp.filter.fir.complex;

import sample.complex.Complex;
import dsp.filter.fir.FIRFilter;
import dsp.filter.fir.real.RealFIRFilter;

public class ComplexFIRFilter extends FIRFilter
{
	private RealFIRFilter mIFilter;
	private RealFIRFilter mQFilter;
	
	/**
	 * Complex FIR Filter for processing complex sample pairs.  Wraps two real 
	 * FIR filters for processing each of the inphase and quadrature samples.
	 * 
	 * @param coefficients - filter taps
	 * @param gain - gain to apply to filtered outputs - use 1.0f for no gain
	 */
	public ComplexFIRFilter( float[] coefficients, float gain )
	{
		mIFilter = new RealFIRFilter( coefficients, gain );
		mQFilter = new RealFIRFilter( coefficients, gain );
	}
	
	public float[] getCoefficients()
	{
		return mIFilter.getCoefficients();
	}
	
	public float filterInphase( float sample )
	{
		return mIFilter.filter( sample );
	}
	
	public float filterQuadrature( float sample )
	{
		return mQFilter.filter( sample );
	}
	
	public Complex filter( Complex sample )
	{
		float i = filterInphase( sample.inphase() );
		float q = filterQuadrature( sample.quadrature() );
		
		return new Complex( i, q );
	}
	
	@Override
	public void dispose()
	{
		mIFilter.dispose();
		mQFilter.dispose();
	}
}
