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

import java.util.Arrays;

/**
 * Window creator
 * 
 * Blackman, Hamming and Hanning windows 
 * based on windowing methods described at:
 * http://www.mstarlabs.com/dsp/goertzel/goertzel.html
 * and
 * Understanding Digital Signal Processing 3e, Lyons, p.91
 * and 
 * Digital Filters for Everyone, Allred, p. 132
 * 
 */
public class Window
{
    public static double[] getWindow( WindowType type, int length )
    {
    	switch( type )
    	{
    		case BLACKMAN:
    			return getBlackmanWindow( length );
    		case COSINE:
    			return getCosineWindow( length );
    		case HAMMING:
    			return getHammingWindow( length );
    		case HANNING:
    			return getHanningWindow( length );
    		case NONE:
			default:
				return getRectangularWindow( length );
    	}
    }

    public static double[] getRectangularWindow( int length )
    {
    	double[] coefficients = new double[ length ];

    	Arrays.fill( coefficients, 1.0d );
    	
    	return coefficients;
    }
    
    public static double[] getCosineWindow( int length )
    {
    	double[] coefficients = new double[ length ];


    	if( length % 2 == 0 ) //Even length
    	{
        	int half = (int)( ( length - 1 ) / 2 );

        	for( int x = -half; x < length / 2 + 1; x++ )
        	{
    		    coefficients[ x + half ] = Math.cos( 
    		    		( (double)x * Math.PI ) / ( (double)length + 1.0d ) );
        	}
    	}
    	else //Odd length
    	{
        	int half = (int) length / 2;

        	for( int x = -half; x < half + 1; x++ )
        	{
    		    coefficients[ x + half ] = Math.cos( 
    		    		( (double)x * Math.PI ) / ( (double)length + 1.0d ) );
        	}
    	}

    	return coefficients;
    }

    public static double[] getBlackmanWindow( int length )
    {
    	double[] coefficients = new double[ length ];
    	
    	for( int x = 0; x < length; x++ )
    	{
		    coefficients[ x ] = .426591D - 
    	    	  ( .496561D * Math.cos( ( Math.PI * 2.0D * (double)x ) / (double)( length - 1 ) ) +
    	    	  ( .076848D * Math.cos( ( Math.PI * 4.0D * (double)x ) / (double)( length - 1 ) ) ) );
    	}

    	return coefficients;
    }
    
    public static double[] getHammingWindow( int length )
    {
    	double[] coefficients = new double[ length ];
    	
    	if( length % 2 == 0 ) //Even length
    	{
        	for( int x = 0; x < length; x++ )
        	{
    		    coefficients[ x ] = .54D - ( .46D * 
		    		Math.cos( ( Math.PI * ( 2.0D * (double)x + 1.0d ) ) / (double)( length - 1 ) ) );
        	}
    	}
    	else //Odd length
    	{
        	for( int x = 0; x < length; x++ )
        	{
    		    coefficients[ x ] = .54D - ( .46D * 
    		    		Math.cos( ( 2.0D * Math.PI * (double)x ) / (double)( length - 1 ) ) );
        	}
    	}

    	return coefficients;
    }

    public static double[] getHanningWindow( int length )
    {
    	double[] coefficients = new double[ length ];

    	if( length % 2 == 0 ) //Even length
    	{
        	for( int x = 0; x < length; x++ )
        	{
    		    coefficients[ x ] = .5D - ( .5D * 
    		    		Math.cos( ( Math.PI * ( 2.0D * (double)x + 1 ) ) / (double)( length - 1 ) ) );
        	}
    	}
    	else //Odd length
    	{
        	for( int x = 0; x < length; x++ )
        	{
    		    coefficients[ x ] = .5D - ( .5D * 
    		    		Math.cos( ( 2.0D * Math.PI * (double)x ) / (double)( length - 1 ) ) );
        	}
    	}


    	return coefficients;
    }

    /**
     * Apply the window against an array of float-type samples
     */
    public static float[] apply( double[] coefficients, float[] samples )
    {
		for( int x = 0; x < samples.length; x++ )
		{
		    samples[ x ] = (float)( samples[ x ] * coefficients[ x ] );
		}
		
		return samples;
    }
    
    public static float[] apply( WindowType type, float[] samples )
    {
    	double[] coefficients = getWindow( type, samples.length );
    	
    	return apply( coefficients, samples );
    }
    
    /**
     * Apply the window against an array of float-type samples
     */
    public static double[] apply( double[] coefficients, double[] samples )
    {
		for( int x = 0; x < samples.length; x++ )
		{
		    samples[ x ] = samples[ x ] * coefficients[ x ];
		}
		
		return samples;
    }

    public static double[] apply( WindowType type, double[] samples )
    {
    	double[] coefficients = getWindow( type, samples.length );
    	
    	return apply( coefficients, samples );
    }
    
    
    
    /**
     * Window types
     */
    public enum WindowType
    {
    	BLACKMAN, COSINE, HAMMING, HANNING, NONE;
    }
    
	/**
	 * Utility to log the double arrays
	 */
	public static String arrayToString( double[] array, boolean breaks )
	{
		StringBuilder sb = new StringBuilder();
		for( int x = 0; x < array.length; x++ )
		{
			sb.append( x + ":" + array[ x ] );

			if( breaks )
			{
				sb.append( "\n" );
			}
			else
			{
				if( x % 8 == 7 )
				{
					sb.append( "\n" );
				}
				else
				{
					sb.append( "\t" );
				}
			}
		}
		
		return sb.toString();
	}

	public static void main( String[] args )
    {
    	System.out.println(  arrayToString( getBlackmanWindow( 31 ), false ) );
    }
}
