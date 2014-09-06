package spectrum;

public interface SpectralDisplayAdjuster
{
	/**
	 * Gets the amplification multiplier that is applied against the FFT bin 
	 * results when displaying the value in the spectral display.
	 * 
	 * @return amplification value ( 1 - 100 )
	 */
	public int getAmplification();
	
	/**
	 * Sets the amplification multiplier that is applied against the FFT bin
	 * results when displaying the value in the spectral display.
	 * 
	 * @param amplification value ( 1 - 100 )
	 */
	public void setAmplification( int amplification );

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
	 * Gets the baseline used when plotting the FFT bin results in the FFT
	 * spectral display.  This value determines the vertical position within the
	 * spectral display upon which the amplified values are added and then 
	 * plotted.
	 * 
	 * @return averaging value ( 1 - 50 )
	 */
	public int getBaseline();
	
	/**
	 * Sets the baseline used when plotting the FFT bin results in the FFT
	 * spectral display.  This value determines the vertical position within the
	 * spectral display upon which the amplified values are added and then 
	 * plotted.
	 * 
	 * @param baseline value ( 1 - 50 )
	 */
	public void setBaseline( int baseline );
}
