package test;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dsp.filter.interpolator.QPSKInterpolator;
import sample.complex.ComplexSample;
import util.Oscillator;

public class Interp
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( Interp.class );

	private Oscillator mNCO = new Oscillator( 1200, 48000 );
	
	public Interp()
	{
		
	}
	
	public ComplexSample next()
	{
		return mNCO.nextComplex();
	}
	
	public static void main( String[] args )
	{
		mLog.debug( "Starting" );

		Interp interp = new Interp();
		
		ComplexSample[] samples = new ComplexSample[ 10 ];
		
		for( int x = 0; x < 10; x++ )
		{
			samples[ x ] = interp.next();
		}
		
		QPSKInterpolator qi = new QPSKInterpolator();
		
		ComplexSample result = qi.filter( samples, 1, 0.25f );
		

		for( int x = 0; x < 10; x++ )
		{
			System.out.println( samples[x].toString() );
//			mLog.debug( "Sample " + x + ": " + samples[ x ].toString() );
		}
		mLog.debug( "Result: " + result.toString() );
		mLog.debug( "Finished" );
	}

}
