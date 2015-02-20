package decode.p25.message.tsbk.motorola;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.reference.DataUnitID;

public abstract class PatchGroup extends MotorolaTSBKMessage
{
	public static final int[] PATCH_GROUP_ADDRESS = { 80,81,82,83,84,85,86,87,
		88,89,90,91,92,93,94,95 };
	public static final int[] GROUP_ADDRESS_1 = { 96,97,98,99,100,101,102,103,
		104,105,106,107,108,109,110,111 };
	public static final int[] GROUP_ADDRESS_2 = { 112,113,114,115,116,117,118,
		119,120,121,122,123,124,125,126,127 };
	public static final int[] GROUP_ADDRESS_3 = { 128,129,130,131,132,133,134,
		135,136,137,138,139,140,141,142,143 };

	public PatchGroup( BinaryMessage message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );
    }

	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( getMessageStub() );
		
		sb.append( " PATCH GROUP:" );
		sb.append( getPatchGroupAddress() );
		sb.append( " GRP1:" );
		sb.append( getGroupAddress1() );
		sb.append( " GRP2:" );
		sb.append( getGroupAddress2() );
		sb.append( " GRP3:" );
		sb.append( getGroupAddress3() );
		
	    return sb.toString();
    }
	
    public String getPatchGroupAddress()
    {
        return mMessage.getHex( PATCH_GROUP_ADDRESS, 4 );
    }
	
    public String getGroupAddress1()
    {
        return mMessage.getHex( GROUP_ADDRESS_1, 4 );
    }
	
    public String getGroupAddress2()
    {
        return mMessage.getHex( GROUP_ADDRESS_2, 4 );
    }
	
    public String getGroupAddress3()
    {
        return mMessage.getHex( GROUP_ADDRESS_3, 4 );
    }
	
	
}
