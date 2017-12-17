package io.github.dsheirer.spectrum;

import io.github.dsheirer.dsp.filter.smoothing.SmoothingFilter.SmoothingType;

public interface SpectralDisplayAdjuster
{
	/**
	 * Gets the averaging value indicating the number of FFT results that are
	 * averaged to produce each FFT results output.
	 * 
	 * @return averaging value ( 2 - 50 )
	 */
	public int getAveraging();
	public void setAveraging( int averaging );

	/**
	 * Sets the smoothing filter averaging window width.
	 * 
	 * Valid values are odd in the range 3 - 29
	 */
	public int getSmoothing();
	public void setSmoothing( int smoothing );

	/**
	 * Sets the type of smoothing filter to use.
	 */
	public SmoothingType getSmoothingType();
	public void setSmoothingType( SmoothingType type );
	
}
