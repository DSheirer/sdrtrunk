package dsp.symbol;

public enum FrameSync
{
	P25_PHASE1( 0x5575F5FF77FFl ),          //+3 +3 +3 +3 +3 -3 +3 +3 -3 -3 +3 +3 -3 -3 -3 -3 +3 -3 +3 -3 -3 -3 -3 -3
	P25_PHASE1_INVERTED( 0xFFDF5F55DD55l );
	
	private long mSync;
	
	private FrameSync( long sync )
	{
		mSync = sync;
	}
	
	public long getSync()
	{
		return mSync;
	}
}
