package module.record;

import java.io.IOException;

import module.Module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import record.wave.FloatWaveRecorder;
import sample.Listener;
import sample.real.IRealBufferListener;
import sample.real.RealBuffer;

public class RealBufferRecorderModule extends Module 
									implements IRealBufferListener
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( RealBufferRecorderModule.class );

	private FloatWaveRecorder mRecorder;
	private String mFilename;
	
	public RealBufferRecorderModule( String filename )
	{
		mFilename = filename;
		mRecorder = new FloatWaveRecorder( 48000, mFilename );
	}
	
	@Override
	public Listener<RealBuffer> getRealBufferListener()
	{
		return mRecorder;
	}

	@Override
	public void dispose()
	{
		try
		{
			mRecorder.stop();
		} 
		catch ( IOException e )
		{
			mLog.error( "Error stopping the recorder", e );
		}
	}

	@Override
	public void init()
	{
		mLog.debug( "Starting the recorder [" + mFilename + "]" );
		
		try
		{
			mRecorder.start();
		} 
		catch ( IOException e )
		{
			mLog.error( "Error starting the recorder", e );
		}
	}

}
