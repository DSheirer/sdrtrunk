package spectrum;

public interface SpectralDisplayAdjuster
{
/**
	 * Gets the averaging value indicating the number of FFT results that are
	 * averaged to produce each FFT results output.
	 * 
	 * @return averaging value ( 2 - 50 )
	 */
	public int getAveraging();
	
	/**
	 * Sets the averaging value indicating the number of FFT results to average
	 * when producing an FFT output result.
	 * @param averaging value ( 2 - 50 )
	 */
	public void setAveraging( int averaging );

	/**
	 * Gets the dBm scale used when plotting the FFT bin results in the FFT
	 * spectral display.  This value determines the smallest displayed power
	 * spectrum value that is shown at the bottom of the display.
	 * 
	 * @return averaging value ( 1 - 50 )
	 */
	public int getDBScale();
	
	/**
	 * Sets the dBm scale from 0 (top of display) to the scale value (bottom).
	 * 
	 * @param baseline value ( -160 to -30 dBm )
	 */
	public void setDBScale( int baseline );
}
