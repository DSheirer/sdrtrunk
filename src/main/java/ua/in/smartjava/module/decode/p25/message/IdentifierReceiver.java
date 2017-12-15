package ua.in.smartjava.module.decode.p25.message;


/**
 * Interface to allow messages to be augmented with IdentiferUpdateXXX type 
 * messages that provide the ua.in.smartjava.channel information necessary to calculate the
 * uplink and downlink frequency for the ua.in.smartjava.channel.
 */
public interface IdentifierReceiver
{
	public void setIdentifierMessage( int identifier, IBandIdentifier message );
	
	public int[] getIdentifiers();
}
