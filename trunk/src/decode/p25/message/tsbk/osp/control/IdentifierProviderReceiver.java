package decode.p25.message.tsbk.osp.control;

import decode.p25.message.IdentifierProvider;

/**
 * Interface to allow messages to be augmented with IdentiferUpdateXXX type 
 * messages that provide the channel information necessary to calculate the
 * uplink and downlink frequency for the channel.
 */
public interface IdentifierProviderReceiver
{
	public void setIdentifierMessage( int identifier, IdentifierProvider message );
	
	public int[] getIdentifiers();
}
