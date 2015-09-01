package module.record;

import java.io.IOException;

import module.Module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import record.wave.ComplexWaveRecorder;
import sample.Listener;
import sample.complex.ComplexBuffer;
import sample.complex.IComplexBufferListener;

public class ComplexBufferRecorderModule extends Module 
									implements IComplexBufferListener
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( ComplexBufferRecorderModule.class );

	private ComplexWaveRecorder mRecorder;
	private String mFilename;
	
	public ComplexBufferRecorderModule( String filename )
	{
		mFilename = filename;
		mRecorder = new ComplexWaveRecorder( 48000, mFilename );
	}
	
	@Override
	public Listener<ComplexBuffer> getComplexBufferListener()
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
