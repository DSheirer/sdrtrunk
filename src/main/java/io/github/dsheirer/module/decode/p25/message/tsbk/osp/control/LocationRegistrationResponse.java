package io.github.dsheirer.module.decode.p25.message.tsbk.osp.control;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Opcode;
import io.github.dsheirer.module.decode.p25.reference.Response;

public class LocationRegistrationResponse extends TSBKMessage
{
	public static final int[] REGISTRATION_RESPONSE = { 86,87 };
	public static final int[] GROUP_ADDRESS = { 88,89,90,91,92,93,94,95,96,97,
		98,99,100,101,102,103 };
	public static final int[] RFSS_ID = { 104,105,106,107,108,109,110,111 };
	public static final int[] SITE_ID = { 112,113,114,115,116,117,118,119 };
    public static final int[] TARGET_ADDRESS = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };
    
    public LocationRegistrationResponse( BinaryMessage message, 
                                DataUnitID duid,
                                AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.LOCATION_REGISTRATION_RESPONSE.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );
        
        sb.append( " REGISTRATION:" + getResponse().name() );
        sb.append( " GROUP:" + getGroupAddress() );
        sb.append( " RFSS: " + getRFSSID() );
        sb.append( " SITE: " + getSiteID() );
        sb.append( " TGT ADDR: " + getTargetAddress() );
        
        return sb.toString();
    }
    
    public Response getResponse()
    {
    	return Response.fromValue( mMessage.getInt( REGISTRATION_RESPONSE ) );
    }
    
    public String getGroupAddress()
    {
    	return mMessage.getHex( GROUP_ADDRESS, 4 );
    }

    public String getRFSSID()
    {
        return mMessage.getHex( RFSS_ID, 2 );
    }
    
    public String getSiteID()
    {
    	return mMessage.getHex( SITE_ID, 2 );
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
