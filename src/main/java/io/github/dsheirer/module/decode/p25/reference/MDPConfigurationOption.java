package io.github.dsheirer.module.decode.p25.reference;

public enum MDPConfigurationOption
{
	INTERNAL( "INTERNAL INTERFACE", 0 ),
	MDP_SLIP( "MDP SLIP", 1 ),
	MDP_PPP( "MDP PPP", 2 ),
	UNKNOWN( "UNKNOWN", -1 );
	
	private String mLabel;
	private int mValue;
	
	private MDPConfigurationOption( String label, int value )
	{
		mLabel = label;
		mValue = value;
	}
	
	public String getLabel()
	{
		return mLabel;
	}
	
	public int getValue()
	{
		return mValue;
	}
	
	public static MDPConfigurationOption fromValue( int value )
	{
		if( 0 <= value && value <= 2 )
		{
			return values()[ value ];
		}
		
		return UNKNOWN;
	}
}
