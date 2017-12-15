package ua.in.smartjava.dsp.symbol;

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

	/**
	 * Allowable transition state 1 for trellis coded modulation.  Indicates 
	 * the dibit that differs from this dibit in the LSB bit position.
	 */
	public Dibit getAllowableTransition1()
	{
		switch( this )
		{
			case D00_PLUS_1:
				return D01_PLUS_3;
			case D01_PLUS_3:
				return D00_PLUS_1;
			case D10_MINUS_1:
				return D11_MINUS_3;
			case D11_MINUS_3:
				return D10_MINUS_1;
			default:
				/* We'll never get here */
				return D00_PLUS_1;
		}
	}
	
	/**
	 * Allowable transition state 2 for trellis coded modulation.  Indicates 
	 * the dibit that differs from this dibit in the MSB bit position.
	 */
	public Dibit getAllowableTransition2()
	{
		switch( this )
		{
			case D00_PLUS_1:
				return D10_MINUS_1;
			case D01_PLUS_3:
				return D11_MINUS_3;
			case D10_MINUS_1:
				return D00_PLUS_1;
			case D11_MINUS_3:
				return D01_PLUS_3;
			default:
				/* We'll never get here */
				return D00_PLUS_1;
		}
	}

	/**
	 * Indicates if the trellis coded modulation dibit is allowed to transition 
	 * to the dibit argument.  
	 */
	public boolean isAllowableTransition( Dibit dibit )
	{
		return dibit == getAllowableTransition1() ||
			   dibit == getAllowableTransition2();
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
