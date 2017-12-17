/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.message;

public enum MessageType
{
	CA_STRT( "Call Start" ),
	CA_ENDD( "Call End" ),
	CA_TOUT( "Call Timeout" ),
	CA_PAGE( "Call Page" ),
    DA_STRT( "Data Start" ),
	DA_ENDD( "Data End" ),
	DA_TOUT( "Data Timeout" ),
	FQ_CCHN( "Control Channel Frequency" ),
	FQ_RXCV( "Receive Frequency" ),
	FQ_RXLO( "Receive Frequency Low" ),
	FQ_RXHI( "Receive Frequency High" ),
	FQ_TXMT( "Transmit Frequency" ),
	FQ_TXLO( "Transmit Frequency Low" ),
	FQ_TXHI( "Transmit Frequency High" ),
	ID_ANIX( "ANI ID" ),
	ID_CHAN( "Channel ID" ),
	ID_ESNX( "ESN ID" ),
	ID_ESNH( "ESN ID Low" ),
	ID_ESNL( "ESN ID High" ),
	ID_FROM( "From ID" ),
	ID_MIDN( "Mobile ID" ),
	ID_NBOR( "Neighbor ID" ),
	ID_RDIO( "Radio ID" ),
	ID_SITE( "Site ID" ),
	ID_SYST( "System ID" ),
	ID_TGAS( "Assign Talkgroup ID" ),
	ID_TOTO( "To ID" ),
	ID_UNIQ( "Unique ID" ),
	MA_CHAN( "Channel Map" ),
	MA_CHNL( "Channel Map Low" ),
	MA_CHNH( "Channel Map High" ),
	MS_TEXT( "Text Message" ),
	MS_MGPS( "GPS Message"),
	MS_STAT( "Status Message" ),
	RA_KILL( "Radio Kill" ),
	RA_REGI( "Radio Register" ),
	RA_STUN( "Radio Stun" ),
	RQ_ACCE( "Request Access" ),
	SY_IDLE( "System Idle" ),
	UN_KNWN( "Unknown" );

	private String mDisplayText;
	
	MessageType( String displayText )
	{
		mDisplayText = displayText;
	}
	
	public String getDisplayText()
	{
		return mDisplayText;
	}
	
	public String toString()
	{
		return getDisplayText();
	}
}
