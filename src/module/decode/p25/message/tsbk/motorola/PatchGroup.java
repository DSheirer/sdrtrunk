package module.decode.p25.message.tsbk.motorola;

import module.decode.p25.reference.DataUnitID;
import alias.AliasList;
import bits.BinaryMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

	private List<String> mPatchedTalkgroups;

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
		
		sb.append( " " );
		sb.append( getPatchGroupAddress() );
		sb.append(" ");
		sb.append(getPatchedTalkgroups());

	    return sb.toString();
    }
	
    public String getPatchGroupAddress()
    {
        return mMessage.getHex( PATCH_GROUP_ADDRESS, 4 );
    }

	/**
	 * List of de-deplicated patched talkgroups contained in this message
	 */
	public List<String> getPatchedTalkgroups()
	{
		if(mPatchedTalkgroups == null)
		{
			mPatchedTalkgroups = new ArrayList<>();

			mPatchedTalkgroups.add(getGroupAddress1());

			String group2 = getGroupAddress2();

			if(!mPatchedTalkgroups.contains(group2))
			{
				mPatchedTalkgroups.add(group2);
			}

			String group3 = getGroupAddress3();

			if(!mPatchedTalkgroups.contains(group3))
			{
				mPatchedTalkgroups.add(group3);
			}
		}

		return mPatchedTalkgroups;
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
