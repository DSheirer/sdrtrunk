package io.github.dsheirer.module.decode.p25.reference;

public enum ExtendedFunction
{
	RADIO_CHECK( 0x0000, "RADIO CHECK" ),
	RADIO_DETACH( 0x007D, "RADIO DETACH" ),
	RADIO_UNINHIBIT( 0x007E, "RADIO UNINHIBIT" ),
	RADIO_INHIBIT( 0x007F, "RADIO INHIBIT" ),
	RADIO_CHECK_ACK( 0x0080, "RADIO CHECK ACK" ),
	RADIO_DETACH_ACK( 0x00FD, "RADIO DETACH ACK" ),
	RADIO_UNINHIBIT_ACK( 0x00FE, "RADIO UNINHIBIT ACK" ),
	RADIO_INHIBIT_ACK( 0x00FF, "RADIO INHIBIT ACK" ),
	
	GROUP_CONTROL_COMMAND( 0x0100, "GROUP CONTROL COMMAND" ),
	UNIT_DYNAMIC_COMMAND( 0x0200, "UNIT DYNAMIC COMMAND" ),
	GROUP_DYNAMIC_COMMAND( 0x0300, "GROUP DYNAMIC COMMAND" ),
	
	UNKNOWN( -1, "UNKNOWN" );

	private int mFunction;
	private String mLabel;
	
	private ExtendedFunction( int function, String label )
	{
		mFunction = function;
		mLabel = label;
	}
	
	public String getLabel()
	{
		return mLabel;
	}
	
	public static ExtendedFunction fromValue( int function )
	{
		switch( function )
		{
			case 0x0000:
				return RADIO_CHECK;
			case 0x007D:
				return RADIO_DETACH;
			case 0x007E:
				return RADIO_UNINHIBIT;
			case 0x007F:
				return RADIO_INHIBIT;
			case 0x0080:
				return RADIO_CHECK_ACK;
			case 0x00FD:
				return RADIO_DETACH_ACK;
			case 0x00FE:
				return RADIO_UNINHIBIT_ACK;
			case 0x00FF:
				return RADIO_INHIBIT_ACK;
		}
		
		if( ( function & 0x0100 ) == 0x0100 )
		{
			return GROUP_CONTROL_COMMAND;
		}
		if( ( function & 0x0200 ) == 0x0200 )
		{
			return UNIT_DYNAMIC_COMMAND;
		}
		if( ( function & 0x0300 ) == 0x0300 )
		{
			return GROUP_DYNAMIC_COMMAND;
		}

		return UNKNOWN;
	}
}
