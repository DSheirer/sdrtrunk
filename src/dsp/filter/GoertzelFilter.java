/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package dsp.filter;

import dsp.filter.Window.WindowType;


/**
 * Implements the Goertzel Filter as described in
 * http://en.wikipedia.org/wiki/Goertzel_algorithm
 * 
 * Calculates the magnitude of a specific frequency component within a window of
 * samples, using a Goertzel Filter (ie an optimized DFT).
 */
public class GoertzelFilter
{
    private int mSampleRate;
    private long mTargetFrequency;
    private int mBlockSize;
    private double[] mWindowCoefficients;

    private double mCoefficient;

    /**
     * Constructs the Goertzel Filter to use for block processing of
     * time-domain signal samples.
     * 
     * @param sampleRate
     *            - sample rate (Hz)
     * @param targetFrequency
     *            - target frequency (Hz)
     * @param blockSize
     *            - number of samples per block of processing
     * @param window 
     *            - window to apply against the samples prior to calculating
     *            the magnitude
     */
    public GoertzelFilter( int sampleRate, 
        	    		   long targetFrequency, 
        	    		   int blockSize,
        	    		   WindowType window )
    {
    	mSampleRate = sampleRate;
    	mTargetFrequency = targetFrequency;
    	mBlockSize = blockSize;
    	mWindowCoefficients = Window.getWindow( window, blockSize );
    	init();
    }

    /**
     * Establish the pre-calculated values to use in the filter
     */
    private void init()
    {
    	double normalizedFrequency = (double) mTargetFrequency / mSampleRate;
    	mCoefficient = 2.0D * Math.cos( 2 * Math.PI * normalizedFrequency );
    }

    
    /**
     * Returns the power (dB) of the target frequency within the block of 
     * samples as normalized against the bin(0) magnitude value
     * 
     * @param samples
     *            - array of time-domain samples -- array size must be the same
     *            size as the windowSize parameter passed upon construction
     * 
     * @return - power measurement in dB

     * @throws IllegalArgumentException if the sample array size is not equal 
     * 		to the defined block size
     */
    public int getPower( float[] samples ) throws IllegalArgumentException
    {
    	// Verify size of samples array against block size
    	if ( samples.length != mBlockSize )
    	{
    	    throw new IllegalArgumentException(
    		    "Sample array size does not equal Block Size" );
    	}
    
    	// Apply the window against the samples
    	samples = Window.apply( mWindowCoefficients, samples );
    
    	// Process the samples
    	double s = 0.0D;
    	double s_prev = 0.0D;
    	double s_prev2 = 0.0D;
    
    	for ( int x = 0; x < samples.length; x++ )
    	{
    	    s = samples[x] + ( mCoefficient * s_prev ) - s_prev2;
    	    s_prev2 = s_prev;
    	    s_prev = s;
    	}
    
    	//power = s_prev2 * s_prev2 + s_prev * s_prev - coeff * s_prev * s_prev2 ;
    
    	double magnitude = ( s_prev2 * s_prev2 ) + ( s_prev * s_prev) - (mCoefficient * s_prev * s_prev2);
    	int binZero = getBinZeroPower( samples );
    	
    	int power = (int) (20 * Math.log10( magnitude / binZero ) );
    	
    	return power;
    }

    /**
     * Sums all of the sample values to derive the bin 0 power
     */
    private int getBinZeroPower( float[] samples )
    {
        int retVal = 0;
        
        for( int x = 0; x < samples.length; x++ )
        {
            retVal += samples[ x ];
        }
        
        return retVal;
    }
    
    /**
     * @return the Sample Rate
     */
    public int getSampleRate()
    {
    	return mSampleRate;
    }

    /**
     * @return the Target Frequency
     */
    public long getTargetFrequency()
    {
    	return mTargetFrequency;
    }

    /**
     * @return the Block Size
     */
    public int getBlockSize()
    {
    	return mBlockSize;
    }

}
