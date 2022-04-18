/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.dsp.filter.fir.remez;

import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RemezFIRFilterDesigner
{
	private final static Logger mLog = LoggerFactory.getLogger( RemezFIRFilterDesigner.class );
	
	private static final double CONVERGENCE_THRESHOLD = 0.0001;
    public static final int MAXIMUM_ITERATION_COUNT = 40;
    public static final double TWO_PI = 2.0 * FastMath.PI;

    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat( "0.00000000" );

    private FIRFilterSpecification mSpecification;
	private Grid mGrid;
	private List<Integer> mExtremalIndices;
	
	private double[] mD;
	private double[] mGridErrors;
	private double[] mGridFrequencyResponse;
	private double[] mIdealFrequencyResponse;
	
	private double mDelta;
	
	private boolean mConverged;

	/**
	 * Linear FIR filter designer using the Parks-McClelland Remez exchange algorithm.
	 * 
	 * Construct a filter specification using the builder methods in the FIRFilterSpecification
	 * class and construct an instance of this class with the specification.  Check the isValid()
	 * method after construction to see if the filter design was successful.  If successful, use the
	 * getFrequencyResponse() and getImpulseResponse() methods to access the filter taps as the
	 * specified length.
	 * 
	 * If the filter cannot be designed from the specification the isValid() method will return
	 * false and any attempts to access the getFrequencyResponse() or getImpulseResponse() methods
	 * will throw a FilterDesignException.
	 * 
	 * @param specification that defines a Type 1-4 linear phase FIR Filter
	 */
	public RemezFIRFilterDesigner( FIRFilterSpecification specification )
	{
		mSpecification = specification;
		
		design();
	}

	/**
	 * Designs the filter according to the specification
	 */
	private void design()
	{
		mGrid = new Grid( mSpecification );
		mExtremalIndices = getInitialExtremalIndices();

    	int iterationCount = 0;
    	
    	try
    	{
        	do
        	{
            	calculateGridFrequencyResponse();
            	calculateGridError();
            	findExtremalIndices();
            	checkConvergence();
            	
            	iterationCount++;
        	}
        	while( !mConverged && iterationCount < MAXIMUM_ITERATION_COUNT );
    	}
    	catch( FilterDesignException fde )
    	{
    		mLog.error( "Filter design error - couldn't find extremal indices at count [" + 
    				iterationCount + "] try changing filter order up/down from [" +
    				mSpecification.getOrder() + "]" );
    		
    		mConverged = false;
    	}

    	if( mConverged )
    	{
        	//Update frequency response using the final set of extremal indices
        	calculateGridFrequencyResponse();
    	}
	}

	/**
	 * Indicates if the filter was successfully created from the filter specification.  Check this
	 * method before accessing frequency response or filter response, otherwise a FilterDesignException
	 * is thrown.
	 * 
	 * @return true if the filter was successfully designed.
	 */
	public boolean isValid()
	{
		return mConverged;
	}

	/**
	 * Frequency response of the designed filter with double precision
	 * 
	 * @return half ( length / 2 ) of filter frequency response sampled at 0.0 - PI radians
	 * @throws FilterDesignException if the specified filter cannot be designed
	 */
	public double[] getFrequencyResponse() throws FilterDesignException
	{
		if( !mConverged )
		{
			throw new FilterDesignException( "Can't create filter from specification - failed "
					+ "to converge" );
		}
		
		//Resample the final polynomial at the desired filter length
    	double[] resampled = resample();

    	//Apply Parks/McClelland frequency response corrections according to the filter type
    	return correctFrequencyResponse( resampled );
	}

	/**
	 * Impulse response of the designed filter with floating point precision.
	 * 
	 * @return filter impulse response
	 * @throws FilterDesignException if the specified filter cannot be designed
	 */
    public float[] getImpulseResponse() throws FilterDesignException
	{
		return convertToFloatArray( getImpulseResponseDoubles() );
	}
	
    /**
     * Impulse response of the designed filter with double precision
     */
    public double[] getImpulseResponseDoubles() throws FilterDesignException
    {
    	double[] frequencyResponse = getFrequencyResponse();
    	
    	int length = mSpecification.getFilterLength();
    	
    	double[] impulseResponse = new double[ length ];

    	double M;
    	
    	switch( mSpecification.getFilterType() )
    	{
    		case TYPE_1_ODD_LENGTH_EVEN_ORDER_SYMMETRICAL:
    			M = ( (double)length - 1.0 ) / 2.0;
    	    	
    			for( int n = 0; n < length; n++ )
    			{
    				double accumulator = frequencyResponse[ 0 ];
    				
    				double frequency = TWO_PI * ( n - M ) / length;
    				
    				for( int k = 1; k <= M; k++ )
    				{
    					accumulator += 2.0 * frequencyResponse[ k ] * FastMath.cos( frequency * (double)k );
    				}
    				
    				impulseResponse[ n ] = accumulator / (double)length;
    			}
    			break;
    		case TYPE_2_EVEN_LENGTH_ODD_ORDER_SYMMETRICAL:
    			double offset = (double)( length - 1 ) / 2.0;
    			
                for( int n = 0; n < length; n++ )
                {
    				double accumulator = frequencyResponse[ 0 ];
                    
    				double frequency = TWO_PI * ( (double)n - offset ) / (double)length;

                    for( int k = 1; k < frequencyResponse.length; k++ )
                    {
                    	accumulator += 2.0 * frequencyResponse[ k ] * FastMath.cos( frequency * (double)k );
                    }

                    impulseResponse[ n ] = accumulator / (double)length;
                }
                break;
    	}
    	
    	return impulseResponse;
    }

    /**
     * Calculates the frequency response of this filter at the specified cosine of the frequency
     * 
     * Implements Oppenheim/Schafer Discrete Time Signal Processing, 3e, 2016, equation 116a to
     * determine the actual response of the specified frequency cosine using Lagrange 
     * interpolation of the polynomial formed at the extremal index set.
     * 
     * @param cosineOfFrequency to calculate the frequency response
     * 
     * @return actual frequency response for the specified frequency with the current set of
     * extreaml indices
     */
    private double getFrequencyResponse( double cosineOfFrequency )
    {
    	double numerator = 0.0;
    	double denominator = 0.0;
    	
		for( int k = 0; k < mExtremalIndices.size() - 1; k++ )
    	{
    		double cosineDelta = cosineOfFrequency - mGrid.getCosineFrequencyGrid()[ mExtremalIndices.get( k ) ];
    		
    		//If this frequency is close to one of the polynomial points, use the polynomial point for the response
    		if( FastMath.abs( cosineDelta ) < 1.0e-7 )
    		{
    			return mIdealFrequencyResponse[ k ];
    		}
    		else
    		{
    			double dkOverCosineDelta = mD[ k ] / cosineDelta;
    			
        		numerator += dkOverCosineDelta * mIdealFrequencyResponse[ k ];
        		denominator += dkOverCosineDelta;
    		}
    	}

		return numerator / denominator;
    }
    
    /**
     * Calculated Delta value 
     * 
     * @return delta value
     */
	public double getDelta()
	{
		return mDelta;
	}
	
    /**
     * Creates initial array of extremal frequency indices spaced at intervals of the grid frequency
     * set for all frequencies where a frequency band weighting is specified.
     */
    private List<Integer> getInitialExtremalIndices()
    {
    	int count = mSpecification.getExtremaCount();
    	
    	int density = mSpecification.getGridDensity();
    	
    	List<Integer> extremalIndices = new ArrayList<>();

        for( int i = 0; i < count; i++ )
        {
            extremalIndices.add( i * density );
        }

        return extremalIndices;
    }
    
    /**
     * Calculates the actual frequency response of a dense frequency grid using the polynomial 
     * represented by the extremal indices.
     */
    private void calculateGridFrequencyResponse()
    {
    	double[] b = calculateB();
    	
    	calculateDelta( b );
    	
    	calculateC();
    	
    	calculateD( b );

    	updateGridFrequencyResponse();
    }

    /**
     * Updates the frequency response of frequencies specified in the dense frequency grid
     */
    private void updateGridFrequencyResponse()
    {
    	mGridFrequencyResponse = new double[ mGrid.getSize() ];

    	double[] gridFrequencyCosines = mGrid.getCosineFrequencyGrid();
    	
    	for( int i = 0; i < mGridFrequencyResponse.length; i++ )
    	{
    		mGridFrequencyResponse[ i ] = getFrequencyResponse( gridFrequencyCosines[ i ] );
    	}
    }
	
    /**
     * Calculates the value of b across the set of extremal indices.
     * 
     * Implements Oppenheim/Schafer Discrete Time Signal Processing, 3e, 2016, equation 115
     * 
     * @return the array of b values of length L+2
     */
    private double[] calculateB()
    {
    	int length = mExtremalIndices.size();
    	
    	double[] b = new double[ length ];

    	for( int k = 0; k < length; k++ )
    	{
    		b[ k ] = 1.0;
    		
    		double xk = mGrid.getCosineFrequencyGrid()[ mExtremalIndices.get( k ) ];
    		
        	for( int i = 0; i < length; i++ )
        	{
        		if( i != k )
        		{
        			double xi = mGrid.getCosineFrequencyGrid()[ mExtremalIndices.get( i ) ];
        			
        			double denominator = xk - xi;
        			
                    if( FastMath.abs( denominator ) < 0.00001 )
                    {
                        denominator = 0.00001;
                    }
                    
        			b[ k ] *= 1.0 / denominator;
        		}
        	}
    	}
    	
    	return b;
    }
    
    /**
     * Calculates the value of delta that represents the maximum ripple for the current set of 
     * extremal indices (L+2)
     * 
     * Implements Oppenheim/Schafer Discrete Time Signal Processing, 3e, 2016, equation 114
     * 
     * @param b calculated for the grid from equation 115
     */
    private void calculateDelta( double[] b )
    {
    	double numerator = 0.0;
    	double denominator = 0.0;

    	double sign = 1.0;
    	
    	for( int k = 0; k < b.length; k++ )
    	{
    		if( k < mExtremalIndices.size() )
    		{
    			int extremalIndex = mExtremalIndices.get( k );
    			
        		numerator += ( b[ k ] * mGrid.getDesiredResponse()[ extremalIndex ] );
        		denominator += b[ k ] * sign / mGrid.getWeight()[ extremalIndex ];
        		sign = -sign;
    		}
    		else
    		{
    			mLog.error( "Something went wrong -- the length of b exceeds the set of extremal indices" );
    		}
    	}
    	
    	mDelta = numerator / denominator;
    }

    /**
     * Calculates the ideal frequency response (C) of the current extremal index set.  This reference
     * is used to determine the level of error in the set of actual frequency responses calculated
     * from this set of extremal indices to determine convergence.
     * 
     * Implements Oppenheim/Schafer Discrete Time Signal Processing, 3e, 2016, equation 116b
     */
    private void calculateC()
    {
    	int length = mSpecification.getExtremaCount() - 1;
    	
    	mIdealFrequencyResponse = new double[ length ];

    	double sign = 1.0;
    	
    	for( int k = 0; k < length; k++ )
    	{
    		if( k < mExtremalIndices.size() )
    		{
    			int index = mExtremalIndices.get( k );
    			
        		mIdealFrequencyResponse[ k ] = mGrid.getDesiredResponse()[ index ] - 
        				( sign * mDelta / mGrid.getWeight()[ index ] );
        		
        		sign = -sign;
    		}
    	}
    }

    /**
     * Calculates the set of D values for the current extremal index set.
     * 
     * Implements Oppenheim/Schafer Discrete Time Signal Processing, 3e, 2016, equation 116c
     */
    private void calculateD( double[] b )
    {
    	int length = mExtremalIndices.size() - 1;
    	
    	mD = new double[ length ];
    	
    	for( int k = 0; k < length; k++ )
    	{
        	//Note: extremalCosines array is one index longer than d array, so we use length as
    		//a pointer to the final (L+2) extremalCosine index
    		mD[k] = b[k] * ( mGrid.getCosineFrequencyGrid()[ mExtremalIndices.get( k ) ] - 
    						 mGrid.getCosineFrequencyGrid()[ mExtremalIndices.get( length ) ] );
    	}
    }

    /**
     * Calculates the weighted error between the specification desired frequency response and the 
     * actual frequency response of the current extremal index set across the frequencies in the
     * dense frequency grid.
     * 
     * Implements Oppenheim/Schafer Discrete Time Signal Processing, 3e, 2016, equation 112
     */
    public void calculateGridError()
    {
    	int length = mGridFrequencyResponse.length;
    	
    	mGridErrors = new double[ length ];
    	
    	for( int i = 0; i < length; i++ )
    	{
            mGridErrors[ i ] = mGrid.getWeight()[ i ] * 
            		( mGrid.getDesiredResponse()[ i ] - mGridFrequencyResponse[ i ] );
    	}
    }

    /**
     * Identifies the indexes of the extremal error values using the Alternation theorem
     * 
     * @throws FilterDesignException if the full set of extremal indices can't be identified
     */
    private void findExtremalIndices() throws FilterDesignException
    {
    	mExtremalIndices.clear();
    	
    	//Check for extremum at error index 0
    	if( ( ( mGridErrors[ 0 ] > 0.0 && mGridErrors[ 0 ] > mGridErrors[ 1 ] ) ||
    		  ( mGridErrors[ 0 ] < 0.0 && mGridErrors[ 0 ] < mGridErrors[ 1 ] ) ) &&
    			isGTEDelta( mGridErrors[ 0 ] ) )
		{
    		mExtremalIndices.add( 0 );
		}
    	
        //Check for extrema in middle error indices )
        for( int x = 1; x < mGridErrors.length - 1; x++ )
        {
        	if( ( ( mGridErrors[ x ] > 0.0 && ( mGridErrors[ x - 1 ] <= mGridErrors[ x ] && mGridErrors[ x ] > mGridErrors[ x + 1 ] ) ) ||
        		  ( mGridErrors[ x ] < 0.0 && ( mGridErrors[ x - 1 ] >= mGridErrors[ x ] && mGridErrors[ x ] < mGridErrors[ x + 1 ] ) ) ) &&
        			isGTEDelta( mGridErrors[ x ] ) )
    		{
        		mExtremalIndices.add( x );
    		}
        }
        
        //Check for extremum at final error index
        int last = mGridErrors.length - 1;
        
        if( ( ( mGridErrors[ last ] > 0.0 && ( mGridErrors[ last ] > mGridErrors[ last - 1 ] ) ) ||
        	  ( mGridErrors[ last ] < 0.0 && ( mGridErrors[ last ] < mGridErrors[ last - 1 ] ) ) ) &&
        		isGTEDelta( mGridErrors[ last ] ) )
        {
        	mExtremalIndices.add( last );
        }

        //Ensure we have the minimum of extremals before we continue
        if( mExtremalIndices.size() < mSpecification.getExtremaCount() )
        {
            throw new FilterDesignException( "Couldn't find the minimum extremal frequencies in "
            		+ "error set" );
        }

        //Enforce alternation theory -- only one extremal for each transition about the zero axis 
        List<Integer> indicesToRemove = new ArrayList<>();
        
        Iterator<Integer> it = mExtremalIndices.iterator();
        Integer current = it.next();
        Integer next;

        boolean positiveAxis = mGridErrors[ current ] > 0.0;

        while( it.hasNext() )
        {
        	next = it.next();
        	
        	//Check for consecutive (redundant) indices on same side of zero axis - retain largest
        	if( !( positiveAxis ^ ( mGridErrors[ next ] > 0.0 ) ) )
        	{
        		if( FastMath.abs( mGridErrors[ next ] ) <= FastMath.abs( mGridErrors[ current ] ))
        		{
        			//Remove next error index that is less than current index and on 
        			it.remove();
        			next = current;
        		}
        		else
        		{
        			//Since we can't remove the current index with the iterator, queue for later removal
        			indicesToRemove.add( current );
        		}
        	}
        	else
        	{
        		positiveAxis = !positiveAxis;
        	}
        	
        	current = next;
        }

        //Remove redundant extremal indices that couldn't be removed via the iterator
    	mExtremalIndices.removeAll( indicesToRemove );

        //Truncate the list to one larger than needed by removing excess tailing indices
        while( mExtremalIndices.size() > mSpecification.getExtremaCount() )
        {
        	mExtremalIndices.remove( mExtremalIndices.size() - 1 );
        }
        
        //If we have one too many indices, delete the smaller of the first or last
        if( mExtremalIndices.size() > mSpecification.getExtremaCount() )
        {
        	int lastIndex = mExtremalIndices.size() - 1;
        	
        	if( FastMath.abs( mGridErrors[ mExtremalIndices.get( 0 )] ) >
        		FastMath.abs( mGridErrors[ mExtremalIndices.get( lastIndex )] ) )
			{
        		mExtremalIndices.remove( lastIndex );
			}
        	else
        	{
        		mExtremalIndices.remove( 0 );
        	}
        }
        
        //Detect if we have too few indices
        if( mExtremalIndices.size() < mSpecification.getExtremaCount() )
        {
            throw new FilterDesignException( "Couldn't find the minimum extremal frequencies in "
            		+ "error set" );
        }
    }

    /**
     * Indicates if the absolute value of the argument is greater than or equal to the delta
     * value with accuracy to 14 digits of precision.  This avoids rounding errors at 15 digits or 
     * precision or greater.
     * 
     * @param value to evaluate
     * 
     * @return true if the absolute value is greater than or equal to the absolute delta value
     */
    private boolean isGTEDelta( double value )
    {
    	return FastMath.abs( value ) - FastMath.abs( mDelta ) > -1.0e-5;
    }
    
    /**
     * Checks for convergence of the frequency response of the current set of extremal indices to
     * the filter specification by comparing the maximum absolute error value against the delta
     * value to determine if these two values are within the convergence threshold.
     */
    public void checkConvergence()
    {
        double maximum = FastMath.abs( mGridErrors[ mExtremalIndices.get( 0 ) ] );

        for( int i = 1; i < mExtremalIndices.size(); i++ )
        {
            double current = FastMath.abs( mGridErrors[ mExtremalIndices.get( i ) ] );

            if( current > maximum )
            {
                maximum = current;
            }
        }
        
        double convergence = maximum - FastMath.abs( mDelta );
        
        mConverged = convergence < CONVERGENCE_THRESHOLD;
    }
    
    /**
     * Coverts/casts the double array to a float array 
     */
    private static float[] convertToFloatArray( double[] samples )
    {
    	float[] converted = new float[ samples.length ];
    	
    	for( int x = 0; x < samples.length; x++ )
    	{
    		converted[ x ] = (float)samples[ x ];
    	}
    	
    	return converted;
    }
    
    /**
     * Resamples the designed filter at half of the filter length at evenly spaced intervals of the
     * frequency spectrum from 0 to PI radians.
     * 
     * @return frequency response of the filter sampled the desired filter length and order
     */
    private double[] resample()
    {
    	int length = mSpecification.getFilterLength();

    	if( length % 2 == 0 )
    	{
    		length--;
    	}

    	double half = (double)length / 2.0;

    	double[] resampled = new double[ (int)FastMath.ceil( half ) ] ;
    	
    	for( int x = 0; x < resampled.length; x++ )
    	{
    		resampled[ x ] = getFrequencyResponse( FastMath.cos( FastMath.PI * (double)x / half ) );
    	}

    	return resampled;
    }
    
    /**
     * Applies Parks/McClelland filter type correction to the final frequency response array
     * 
     * @param frequencyResponse of the resampled filter design from 0 <> ~ PI radians
     * 
     * @return corrected frequency response
     */
    private double[] correctFrequencyResponse( double[] frequencyResponse )
    {
    	double filterLength = mSpecification.getFilterLength();
    	
    	switch( mSpecification.getFilterType() )
    	{
			case TYPE_1_ODD_LENGTH_EVEN_ORDER_SYMMETRICAL:
				//No correction needed
				break;
			case TYPE_2_EVEN_LENGTH_ODD_ORDER_SYMMETRICAL:
				//No correction needed
				break;
			case TYPE_3_ODD_LENGTH_EVEN_ORDER_ANTI_SYMMETRICAL:
				for( int x = 0; x < frequencyResponse.length; x++ )
				{
		    		double frequencyRadians = FastMath.PI * ( ( (double)x ) / filterLength );
					frequencyResponse[ x ] *= FastMath.sin( TWO_PI * frequencyRadians );
				}
				break;
			case TYPE_4_EVEN_LENGTH_ODD_ORDER_ANTI_SYMMETRICAL:
				for( int x = 0; x < frequencyResponse.length; x++ )
				{
		    		double frequencyRadians = FastMath.PI * ( ( (double)x ) / filterLength );
					frequencyResponse[ x ] *= FastMath.sin( FastMath.PI * frequencyRadians );
				}
				break;
			default:
				break;
    	}
    	
    	return frequencyResponse;
    }
}
