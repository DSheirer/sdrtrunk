package decode.p25.message.pdu.osp.control;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.pdu.PDUMessage;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;
import dsp.fsk.P25MessageFramer;

public class RoamingAddressUpdateExtended extends PDUMessage 
{
	public static final int[] TARGET_ADDRESS = { 88,89,90,91,92,93,94,95,96,97,
		98,99,100,101,102,103,104,105,106,107,108,109,110,111 };
	public static final int LAST_MESSAGE_INDICATOR = 128;
	public static final int[] MESSAGE_SEQUENCE_NUMBER = { 132,133,134,135 };
	public static final int[] WACN_A = { 136,137,138,139,140,141,142,143,160,
		161,162,163,164,165,166,167,168,169,170,171 };
	public static final int[] SYSTEM_ID_A = { 172,173,174,175,176,177,178,179,
		180,181,182,183 };
	public static final int[] SOURCE_ID_FORMAT_1 = { 184,185,186,187,188,189,
		190,191,192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,207 };
	public static final int[] WACN_B = { 184,185,186,187,188,189,190,191,192,
		193,194,195,196,197,198,199,200,201,202,203 };
	public static final int[] SYSTEM_ID_B = { 204,205,206,207,208,209,210,211,
		212,213,214,215 };
	public static final int[] WACN_C = { 216,217,218,219,220,221,222,223,224,
		225,226,227,228,229,230,231,232,233,234,235 };
	public static final int[] SYSTEM_ID_C = { 236,237,238,239,240,241,242,243,
		244,245,246,247 };
	public static final int[] WACN_D = { 248,249,250,251,252,253,254,255,256,
		257,258,259,260,261,262,263,264,265,266,267 };
	public static final int[] SYSTEM_ID_D = { 268,269,270,271,272,273,274,275,
		276,277,278,279 };
	public static final int[] SOURCE_ID_FORMAT_2 = { 280,281,282,283,284,285,
		286,287,288,289,290,291,292,293,294,295,296,297,298,299,300,301,302,303 };
	public static final int[] WACN_E = { 280,281,282,283,284,285,
		286,287,288,289,290,291,292,293,294,295,296,297,298,299 };
	public static final int[] SYSTEM_ID_E = { 300,301,302,303,304,305,306,307,
		308,309,310,311 };
	public static final int[] WACN_F = { 312,313,314,315,316,317,318,319,320,
		321,322,323,324,325,326,327,328,329,330,331 };
	public static final int[] SYSTEM_ID_F = { 332,333,334,335,336,337,338,339,
		340,341,342,343 };
	public static final int[] WACN_G = { 344,345,346,347,348,349,350,351,352,
		353,354,355,356,357,358,359,360,361,362,363	};
	public static final int[] SYSTEM_ID_G = { 364,365,366,367,368,369,370,371,
		372,373,374,375 };
	public static final int[] SOURCE_ID_FORMAT_3 = { 376,377,378,379,380,381,
		382,383,384,385,386,387,388,389,390,391,392,393,394,395,396,397,398,399 };
	public static final int[] MULTIPLE_BLOCK_CRC_FORMAT_1 = { 224,225,226,227,
		228,229,230,231,232,233,234,235,236,237,238,239,240,241,242,243,244,245,
		246,247,248,249,250,251,252,253,254,255 };
	public static final int[] MULTIPLE_BLOCK_CRC_FORMAT_2 = { 320,321,322,323,
		324,325,326,327,328,329,330,331,332,333,334,335,336,337,338,339,340,341,
		342,343,344,345,346,347,348,349,350,351 };
	public static final int[] MULTIPLE_BLOCK_CRC_FORMAT_3 = { 416,417,418,419,
		420,421,422,423,424,425,426,427,428,429,430,431,432,433,434,435,436,437,
		438,439,440,441,442,443,444,445,446,447 };
	
	private enum Format{ FORMAT_1, FORMAT_2, FORMAT_3 };
	
	private Format mFormat;
	
	public RoamingAddressUpdateExtended( BitSetBuffer message,
            DataUnitID duid, AliasList aliasList )
    {
	    super( message, duid, aliasList );
	    
	    if( mMessage.size() == P25MessageFramer.PDU2_BEGIN )
	    {
	    	mFormat = Format.FORMAT_1;
	    }
	    else if( mMessage.size() == P25MessageFramer.PDU3_BEGIN )
	    {
	    	mFormat = Format.FORMAT_2;
	    }
	    else if( mMessage.size() == P25MessageFramer.PDU3_DECODED_END )
	    {
	    	mFormat = Format.FORMAT_3;
	    }
	    else
	    {
	    	mFormat = Format.FORMAT_1;
	    }
    }

    @Override
    public String getEventType()
    {
        return Opcode.ROAMING_ADDRESS_UPDATE.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " ROAMING ADDRESS STACK FROM:" );
        sb.append( getSourceID() );

        if( isLastBlock() )
        {
        	sb.append( " LAST BLOCK" );
        }
        
        sb.append( "MSG SEQ:" );
        sb.append( getMessageSequenceNumber() );
        
        sb.append( " TO:" );
        sb.append( getTargetAddress() );

        sb.append( " WACN A:" + getWACNA() );
        sb.append( " SYS A:" + getSystemIDA() );

        if( isFormat2() )
        {
            sb.append( " WACN B:" + getWACNB() );
            sb.append( " SYS B:" + getSystemIDB() );
            sb.append( " WACN C:" + getWACNC() );
            sb.append( " SYS C:" + getSystemIDC() );
            sb.append( " WACN D:" + getWACND() );
            sb.append( " SYS D:" + getSystemIDD() );
        }
        
        if( isFormat3() )
        {
            sb.append( " WACN E:" + getWACNE() );
            sb.append( " SYS E:" + getSystemIDE() );
            sb.append( " WACN F:" + getWACNF() );
            sb.append( " SYS F:" + getSystemIDF() );
            sb.append( " WACN G:" + getWACNG() );
            sb.append( " SYS G:" + getSystemIDG() );
        }
        
        return sb.toString();
    }
    
    public boolean isLastBlock()
    {
    	return mMessage.get( LAST_MESSAGE_INDICATOR );
    }
    
    public int getMessageSequenceNumber()
    {
    	return mMessage.getInt( MESSAGE_SEQUENCE_NUMBER );
    }
    
    public String getTargetAddress()
    {
    	return mMessage.getHex( TARGET_ADDRESS, 6 );
    }
    
    public String getWACNA()
    {
    	return mMessage.getHex( WACN_A, 5 );
    }
    
    public String getSystemIDA()
    {
    	return mMessage.getHex( SYSTEM_ID_A, 3 );
    }
    
    public String getWACNB()
    {
    	return isFormat2() ? mMessage.getHex( WACN_B, 5 ) : null;
    }
    
    public String getSystemIDB()
    {
    	return isFormat2() ? mMessage.getHex( SYSTEM_ID_B, 3 ) : null;
    }
    
    public String getWACNC()
    {
    	return isFormat2() ? mMessage.getHex( WACN_C, 5 ) : null;
    }
    
    public String getSystemIDC()
    {
    	return isFormat2() ? mMessage.getHex( SYSTEM_ID_C, 3 ) : null;
    }
    
    public String getWACND()
    {
    	return isFormat2() ? mMessage.getHex( WACN_D, 5 ) : null;
    }
    
    public String getSystemIDD()
    {
    	return isFormat2() ? mMessage.getHex( SYSTEM_ID_D, 3 ) : null;
    }
    
    public String getWACNE()
    {
    	return isFormat3() ? mMessage.getHex( WACN_E, 5 ) : null;
    }
    
    public String getSystemIDE()
    {
    	return isFormat3() ? mMessage.getHex( SYSTEM_ID_E, 3 ) : null;
    }
    
    public String getWACNF()
    {
    	return isFormat3() ? mMessage.getHex( WACN_F, 5 ) : null;
    }
    
    public String getSystemIDF()
    {
    	return isFormat3() ? mMessage.getHex( SYSTEM_ID_F, 3 ) : null;
    }
    
    public String getWACNG()
    {
    	return isFormat3() ? mMessage.getHex( WACN_G, 5 ) : null;
    }
    
    public String getSystemIDG()
    {
    	return isFormat3() ? mMessage.getHex( SYSTEM_ID_G, 3 ) : null;
    }
    
    public String getSourceID()
    {
    	switch( mFormat )
    	{
			case FORMAT_1:
		    	return mMessage.getHex( SOURCE_ID_FORMAT_1, 6 );
			case FORMAT_2:
		    	return mMessage.getHex( SOURCE_ID_FORMAT_2, 6 );
			case FORMAT_3:
		    	return mMessage.getHex( SOURCE_ID_FORMAT_2, 6 );
    	}
    	
    	return null;
    }
    
    public boolean isFormat2()
    {
    	return mFormat != Format.FORMAT_3;
    }
    
    public boolean isFormat3()
    {
    	return mFormat == Format.FORMAT_3;
    }
}
