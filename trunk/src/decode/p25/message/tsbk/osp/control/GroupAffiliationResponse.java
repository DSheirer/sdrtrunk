package decode.p25.message.tsbk.osp.control;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class GroupAffiliationResponse extends TSBKMessage
{
    public enum AffiliationStatus { ACCEPT, FAIL, DENY, REFUSED };

    public static final int LOCAL_GLOBAL_AFFILIATION_FLAG = 80;
    public static final int[] GROUP_AFFILIATION_VALUE = { 86,87 };
    public static final int[] ANNOUNCEMENT_GROUP_ADDRESS = { 88,89,90,91,
        92,93,94,95,96,97,98,99,100,101,102,103 };
    public static final int[] GROUP_ADDRESS = { 104,105,106,107,108,109,110,111,
        112,113,114,115,116,117,118,119 };
    public static final int[] TARGET_ADDRESS = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };
    
    public GroupAffiliationResponse( BitSetBuffer message, 
                                DataUnitID duid,
                                AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.GROUP_AFFILIATION_RESPONSE.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( super.getMessage() );

        sb.append( " STATUS:" + getStatus().name() );
        sb.append( " ANNOUNCE GROUP:" + getAnnouncementGroupAddress() );
        sb.append( " GRP ADDR:" + getGroupAddress() );
        sb.append( " TGT ADDR: " + getTargetAddress() );
        
        return sb.toString();
    }
    
    public String getAffiliation()
    {
        return mMessage.get( LOCAL_GLOBAL_AFFILIATION_FLAG) ? " GLOBAL" : " LOCAL";
    }
    
    public AffiliationStatus getStatus()
    {
        int status = mMessage.getInt( GROUP_AFFILIATION_VALUE );
        
        return AffiliationStatus.values()[ status ];
    }
    
    public String getAnnouncementGroupAddress()
    {
        return mMessage.getHex( ANNOUNCEMENT_GROUP_ADDRESS, 4 );
    }
    
    public String getGroupAddress()
    {
        return mMessage.getHex( GROUP_ADDRESS, 4 );
    }
    
    @Override
    public String getFromID()
    {
        return getGroupAddress();
    }
    
    public String getTargetAddress()
    {
        return mMessage.getHex( TARGET_ADDRESS, 6 );
    }

    @Override
    public String getToID()
    {
        return getTargetAddress();
    }
}
