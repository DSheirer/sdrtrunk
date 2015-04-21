package decode.p25.message.tsbk.motorola;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.reference.DataUnitID;

public class TrafficChannelBaseStationIdentification extends MotorolaTSBKMessage 
{
    public static final int[] CHARACTER_1 = { 80,81,82,83,84,85 };
    public static final int[] CHARACTER_2 = { 86,87,88,89,90,91 };
    public static final int[] CHARACTER_3 = { 92,93,94,95,96,97 };
    public static final int[] CHARACTER_4 = { 98,99,100,101,102,103 };
    public static final int[] CHARACTER_5 = { 104,105,106,107,108,109 };
    public static final int[] CHARACTER_6 = { 110,111,112,113,114,115 };
    public static final int[] CHARACTER_7 = { 116,117,118,119,120,121 };
    public static final int[] CHARACTER_8 = { 122,123,124,125,126,127 };
    
    public TrafficChannelBaseStationIdentification( BinaryMessage message, 
                                DataUnitID duid,
                                AliasList aliasList ) 
    {
    	super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return MotorolaOpcode.TRAFFIC_CHANNEL_ID.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " ID:" + getID() );
        
        return sb.toString();
    }
    
    
    public String getID()
    {
    	StringBuilder sb = new StringBuilder();

    	sb.append( getCharacter( CHARACTER_1 ) );
    	sb.append( getCharacter( CHARACTER_2 ) );
    	sb.append( getCharacter( CHARACTER_3 ) );
    	sb.append( getCharacter( CHARACTER_4 ) );
    	sb.append( getCharacter( CHARACTER_5 ) );
    	sb.append( getCharacter( CHARACTER_6 ) );
    	sb.append( getCharacter( CHARACTER_7 ) );
    	sb.append( getCharacter( CHARACTER_8 ) );
    	
		return sb.toString();
    }
    
    private String getCharacter( int[] field )
    {
    	int value = mMessage.getInt( field );
    	
    	if( value != 0 )
    	{
    		return String.valueOf( (char)( value + 43 ) );
    	}
    	
    	return null;
    }
}
