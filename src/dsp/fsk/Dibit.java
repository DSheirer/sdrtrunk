package dsp.fsk;

public enum Dibit
{
	D01_PLUS_3( false, true,  1, 4 ),
	D00_PLUS_1( false, false, 0, 0 ),
	D10_MINUS_1( true, false, 2, 8 ),
	D11_MINUS_3( true, true,  3, 12 );
	
	private boolean mBit1;
	private boolean mBit2;
	private int mLowValue;
	private int mHighValue;
	
	private Dibit( boolean bit1, boolean bit2, int lowValue, int highValue )
	{
		mBit1 = bit1;
		mBit2 = bit2;
		mLowValue = lowValue;
		mHighValue = highValue;
	}
	
	public boolean getBit1()
	{
		return mBit1;
	}
	
	public boolean getBit2()
	{
		return mBit2;
	}
	
	public int getLowValue()
	{
		return mLowValue;
	}
	
	public int getHighValue()
	{
		return mHighValue;
	}
	
	public static Dibit inverted( Dibit symbol )
	{
		switch( symbol )
		{
			case D10_MINUS_1:
				return D11_MINUS_3;
			case D11_MINUS_3:
				return D01_PLUS_3;
			case D00_PLUS_1:
				return D10_MINUS_1;
			case D01_PLUS_3:
			default:
				return D00_PLUS_1;
		}
	}
}
