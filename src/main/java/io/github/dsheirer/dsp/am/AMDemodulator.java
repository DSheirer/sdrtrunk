package io.github.dsheirer.dsp.am;

public abstract class AMDemodulator
{
	private float mGain;
	
	public AMDemodulator( float gain )
	{
		mGain = gain;
	}
	
    public float demodulate( float inphase, float quadrature )
    {
    	return (float)Math.sqrt( ( inphase * inphase ) + 
    							 ( quadrature * quadrature ) ) * mGain; 
    }
    
	public void setGain( float gain )
	{
		mGain = gain;
	}
}
