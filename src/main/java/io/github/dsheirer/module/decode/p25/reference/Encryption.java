package io.github.dsheirer.module.decode.p25.reference;

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
	AES_256( 0x84 ),
	AES_CBC( 0x85 ),
	DES_XL( 0x9F ), /* Motorola Proprietary */
	DVI_XL( 0xA0 ), /* Motorola Proprietary */
	DVP_XL( 0xA1 ), /* Motorola Proprietary */
	ADP( 0xAA ),
	UNKNOWN( -1 );
	
	private int mValue;
	
	private Encryption( int value )
	{
		mValue = value;
	}
	
	public static Encryption fromValue( int value )
	{
		switch( value )
		{
			case 0x00:
				return ACCORDION_1_3;
			case 0x01:
				return BATON_AUTO_EVEN;
			case 0x02:
				return FIREFLY_TYPE1;
			case 0x03:
				return MAYFLY_TYPE1;
			case 0x04:
				return SAVILLE;
			case 0x41:
				return BATON_AUTO_ODD;
			case 0x80:
				return UNENCRYPTED;
			case 0x81:
				return DES_OFB;
			case 0x82:
				return TRIPLE_DES_2_KEY;
			case 0x83:
				return TRIPLE_DES_3_KEY;
			case 0x84:
				return AES_256;
			case 0x85:
				return AES_CBC;
			case 0x9F:
				return DES_XL;
			case 0xA0:
				return DVI_XL;
			case 0xA1:
				return DVP_XL;
			case 0xAA:
				return ADP;
			default:
				return UNKNOWN;
		}
		
	}
}
