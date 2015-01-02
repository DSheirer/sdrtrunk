package decode.p25.message;


/**
 * Interface to allow messages to be augmented with IdentiferUpdateXXX type 
 * messages that provide the channel information necessary to calculate the
 * uplink and downlink frequency for the channel.
 */
public interface IdentifierReceiver
{
	public void setIdentifierMessage( int identifier, IdentifierProvider message );
	
	public int[] getIdentifiers();
}
