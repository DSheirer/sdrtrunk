package io.github.dsheirer.bits;

public class SyncDetector implements ISyncProcessor
{
	private long mPattern;
	private ISyncDetectListener mListener;

	public SyncDetector( long pattern )
	{
		mPattern = pattern;
	}
	
	public SyncDetector( long pattern, ISyncDetectListener listener )
	{
		this( pattern );
		
		mListener = listener;
	}
	
	public void setListener( ISyncDetectListener listener )
	{
		mListener = listener;
	}

	@Override
	public void checkSync( long value )
	{
		if( value == mPattern && mListener != null )
		{
			mListener.syncDetected();
		}
	}
}
