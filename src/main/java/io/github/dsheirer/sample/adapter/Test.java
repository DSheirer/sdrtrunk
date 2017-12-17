package io.github.dsheirer.sample.adapter;

import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Test
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( HackRFTunerController.class );
	
	public static void main( String[] args )
	{
		mLog.debug( "Starting" );
		
		ShortToFloatMap map1 = new ShortToFloatMap();

		for( short x = -32768; x < -32758; x++ )
		{
			mLog.debug( "X:" + x + " value:" + map1.get( x ) );
		}
		mLog.debug( "Finished" );
	}

}
