package bits;

public class SoftSyncDetector implements ISyncProcessor
{
	private ISyncDetectListener mListener;
	private long mPattern;
	private int mThreshold;
	
	public SoftSyncDetector( ISyncDetectListener listener, long pattern, int threshold )
	{
		this( pattern, threshold );
		
		mListener = listener;
	}
	
	public SoftSyncDetector( long pattern, int threshold )
	{
		mPattern = pattern;
		mThreshold = threshold;
	}
	
	public void dispose()
	{
		mListener = null;
	}

	@Override
	public void checkSync( long value )
	{
		long difference = value ^ mPattern;
		
		if( ( difference == 0 || Long.bitCount( difference ) <= mThreshold ) &&
			  mListener != null )
		{
			mListener.syncDetected();
		}
	}
	
	public void setThreshold( int threshold )
	{
		mThreshold = threshold;
	}
	
	public void setListener( ISyncDetectListener listener )
	{
		mListener = listener;
	}
}
