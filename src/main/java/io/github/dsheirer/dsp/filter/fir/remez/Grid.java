package io.github.dsheirer.dsp.filter.fir.remez;

import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import org.apache.commons.math3.util.FastMath;

public class Grid
{
    public static final double TWO_PI = 2.0 * FastMath.PI;
    
    private double[] mFrequencyGrid;
    private double[] mCosineOfFrequencyGrid;
    private double[] mDesiredResponse;
    private double[] mWeight;

    /**
     * Creates a dense frequency grid from the filter specification for those frequency bands
     * where the weighted value is non-zero.  This grid is appropriate for use in the Remez
     * exchange algorithm.
     */
    public Grid( FIRFilterSpecification specification )
    {
    	create( specification );
    }
    
    private void create( FIRFilterSpecification specification )
    {
        int gridSize = specification.getGridSize();
        
        mFrequencyGrid = new double[ gridSize ];
        mCosineOfFrequencyGrid = new double[ gridSize ];
        mDesiredResponse = new double[ gridSize ];
        mWeight = new double[ gridSize ];
        
        double gridFrequencyInterval = specification.getGridFrequencyInterval();
        
        double bandZeroEdge = specification.getFrequencyBands().get( 0 ).getStart();
        
        // For differentiator, hilbert, symmetry is odd and Grid[0] = max(delf, bands[0])
        double grid0 = specification.isAntiSymmetrical() && 
        			   ( gridFrequencyInterval > bandZeroEdge ) ? gridFrequencyInterval : bandZeroEdge;
        
        int j = 0;

        double maxRipple = specification.getMaxBandAmplitude();

        for( int x = 0; x < specification.getFrequencyBands().size(); x++ )
        {
        	FIRFilterSpecification.FrequencyBand band = specification.getFrequencyBands().get( x );
        	
            double lowFrequency = ( x == 0 ? grid0 : band.getStart() );

            for( int i = 0; i < band.getGridSize(); i++ )
        	{
                mDesiredResponse[ j ] = band.getAmplitude();

                //Set the weighting function equally across this band's dense grids
                mWeight[ j ] = band.getWeight( maxRipple );

                //Frequency grid is linear taper from start frequency to stop frequency in
                //gridFrequencyInterval increments
                mFrequencyGrid[ j ] = lowFrequency;
                
                mCosineOfFrequencyGrid[ j ] = FastMath.cos( TWO_PI * mFrequencyGrid[ j ] );

                lowFrequency += gridFrequencyInterval;
                
                j++;
        	}
            
            int lastIndex = j - 1;
            
            mFrequencyGrid[ lastIndex ] = band.getEnd();
        }
    }

    /**
     * Size of the dense frequency grid
     */
    public int getSize()
    {
        return mFrequencyGrid.length;
    }

    /**
     * The dense frequency grid
     */
    public double[] getFrequencyGrid()
    {
        return mFrequencyGrid;
    }
    
    public double[] getCosineFrequencyGrid()
    {
    	return mCosineOfFrequencyGrid;
    }

    /**
     * Desired frequency response
     */
    public double[] getDesiredResponse()
    {
        return mDesiredResponse;
    }

    /**
     * Weighting function
     */
    public double[] getWeight()
    {
        return mWeight;
    }
}
