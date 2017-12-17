package io.github.dsheirer.module.decode.p25.message;

/**
 * Interface for adjacent site (ie neighbor) messages
 */
public interface IAdjacentSite
{
	public String getUniqueID();
	public String getRFSS();
	public String getSystemID();
	public String getSiteID();
	public String getLRA();
	public String getSystemServiceClass();
	
	public String getDownlinkChannel();
	public long getDownlinkFrequency();
	
	public String getUplinkChannel();
	public long getUplinkFrequency();
}
