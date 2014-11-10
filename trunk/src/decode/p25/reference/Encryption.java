package decode.p25.reference;

public enum Encryption
{
	ACCORDION_1_3( 0x00 ),
	BATON_AUTO_EVEN( 0x01 ),
	FIREFLY_TYPE1( 0x02 ),
	MAYFLY_TYPE1( 0x03 ),
	SAVILLE( 0x04 ),
	BATON_AUTO_ODD( 0x41 ),
	UNENCRYPTED( 0x80 ),
	DES_OFB( 0x81 ),
	TRIPLE_DES_2_KEY( 0x82 ),
	TRIPLE_DES_3_KEY( 0x83 ),
	AES( 0x84 );
	
	private int mValue;
	
	private Encryption( int value )
	{
		mValue = value;
	}
}
