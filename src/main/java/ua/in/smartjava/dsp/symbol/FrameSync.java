package ua.in.smartjava.dsp.symbol;

public enum FrameSync
{
	P25_PHASE1_ERROR_90_CCW( 0xFFEFAFAAEEAAl ),
	P25_PHASE1_NORMAL(       0x5575F5FF77FFl ),  // +33333 -3 +33 -33 +33 -3333 +3 -3 +3 -33333
	P25_PHASE1_ERROR_90_CW(  0x001050551155l ),
	P25_PHASE1_ERROR_180(    0xAA8A0A008800l );
	
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
