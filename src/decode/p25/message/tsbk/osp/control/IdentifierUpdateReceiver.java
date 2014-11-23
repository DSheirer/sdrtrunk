package decode.p25.message.tsbk.osp.control;

/**
 * Interface to allow messages to be augmented with IdentiferUpdateXXX type 
 * messages that provide the channel information necessary to calculate the
 * uplink and downlink frequency for the channel.
 */
public interface IdentifierUpdateReceiver
{
	public void setIdentifierMessage( int identifier, IdentifierUpdate message );
	
	public int[] getIdentifiers();
}
