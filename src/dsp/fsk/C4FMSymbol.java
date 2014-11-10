package dsp.fsk;

public enum C4FMSymbol
{
	SYMBOL_PLUS_3( false, true ),
	SYMBOL_PLUS_1( false, false ),
	SYMBOL_MINUS_1( true, false ),
	SYMBOL_MINUS_3( true, true );
	
	private boolean mBit1;
	private boolean mBit2;
	
	private C4FMSymbol( boolean bit1, boolean bit2 )
	{
		mBit1 = bit1;
		mBit2 = bit2;
	}
	
	public boolean getBit1()
	{
		return mBit1;
	}
	
	public boolean getBit2()
	{
		return mBit2;
	}
	
	public static C4FMSymbol inverted( C4FMSymbol symbol )
	{
		switch( symbol )
		{
			case SYMBOL_MINUS_1:
				return SYMBOL_MINUS_3;
			case SYMBOL_MINUS_3:
				return SYMBOL_PLUS_3;
			case SYMBOL_PLUS_1:
				return SYMBOL_MINUS_1;
			case SYMBOL_PLUS_3:
			default:
				return SYMBOL_PLUS_1;
		}
	}
}
