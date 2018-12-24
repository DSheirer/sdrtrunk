package io.github.dsheirer.module.decode.p25.reference;

public enum RejectReason
{
	R0( 0, "ANY REASON" ),
	R1( 1, "MRC NOT PROVISIONED FOR TDS" ),
	R2( 2, "MRC DSUT NOT SUPPORTED" ),
	R3( 3, "MAX TDS CONTEXTS EXCEEDED" ),
	R4( 4, "SNDCP VERSION NOT SUPPORTED" ),
	R5( 5, "TDS NOT SUPPORTED BY FNE" ),
	R6( 6, "TDS NOT SUPPORTED BY THIS SYSTEM" ),
	R7( 7, "IPV4 STATIC ADDRESS NOT CORRECT" ),
	R8( 8, "IPV4 STATIC ADDRESS NOT ALLOWED" ),
	R9( 9, "IPV4 STATIC ADDRESS IN USE" ),
	R10( 10, "IPV4 NOT SUPPORTED" ),
	R11( 11, "IPV4 DYNAMIC ADDRESS POOL EMPTY" ),
	R12( 12, "IPV4 DYNAMIC ADDRESS NOT SUPPORTED" ),
    UNKNOWN( -1, "UNKNOWN" );
    
    private int mValue;
    public String mLabel;
    
    private RejectReason(int value, String label )
    {
        mValue = value;
        mLabel = label;
    }
    
    public int getValue()
    {
    	return mValue;
    }
    
    public String getLabel()
    {
    	return mLabel;
    }
    
    public String toString()
    {
    	return getLabel();
    }
    
    public static RejectReason fromValue(int value )
    {
    	if( 0 <= value && value <= 12 )
    	{
    		return values()[ value ];
    	}
    	
        return UNKNOWN;
    }
}
