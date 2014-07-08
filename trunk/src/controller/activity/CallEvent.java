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
package controller.activity;

import java.text.SimpleDateFormat;
import java.util.Date;

import alias.Alias;
import alias.AliasList;
import decode.DecoderType;

public abstract class CallEvent
{
	protected long mEventTime = System.currentTimeMillis();
	
	private SimpleDateFormat mSDF = new SimpleDateFormat( "yyyyMMdd','HHmmss" );

	protected DecoderType mDecoderType;
	protected CallEventType mCallEventType;
	protected AliasList mAliasList;
	protected String mFromID;
	protected String mToID;
	protected String mDetails;
	
	public CallEvent( DecoderType decoder,
					  CallEventType callEventType, 
					  AliasList aliasList,
					  String fromID,
					  String toID,
					  String details )
	{
		mDecoderType = decoder;
		mCallEventType = callEventType;
		mAliasList = aliasList;
		mFromID = fromID;
		mToID = toID;
		mDetails = details;
	}
	
	public DecoderType getDecoderType()
	{
		return mDecoderType;
	}
	
	public CallEventType getCallEventType()
	{
		return mCallEventType;
	}

	public AliasList getAliasList()
	{
		return mAliasList;
	}
	
	public void setAliasList( AliasList aliasList )
	{
	    mAliasList = aliasList;
	}
	
	public boolean hasAliasList()
	{
		return mAliasList != null;
	}
	
	public long getEventTime()
	{
		return mEventTime;
	}
	
	public String getFromID()
	{
		return mFromID;
	}
	
	public void setFromID( String id )
	{
		mFromID = id;
	}
	
	public String getToID()
	{
		return mToID;
	}
	
	public void setToID( String id )
	{
		mToID = id;
	}

	public String getDetails()
	{
		return mDetails;
	}
	
	public void setDetails( String details )
	{
		mDetails = details;
	}
	
	public abstract Alias getFromIDAlias();
	
	public abstract Alias getToIDAlias();
	
	public abstract int getChannel();

	public abstract long getFrequency();
	
	public static String getCSVHeader()
	{
		return "DATE,TIME,DECODER,EVENT,FROM,FROM_ALIAS,TO,TO_ALIAS,"
				+ "CHANNEL,FREQUENCY,DETAILS";
	}
	
	public String toCSV()
	{
		StringBuilder sb = new StringBuilder();

		sb.append( "'" );
		sb.append( mSDF.format( new Date( getEventTime() ) ) );
		sb.append( "','" );
		sb.append( getDecoderType().toString() );
		sb.append( "','" );
		sb.append( getCallEventType().toString() );
		sb.append( "'," );
		if( getFromID() != null )
		{
			sb.append( "'" );
			sb.append( getFromID() );
			sb.append( "'" );
		}
		sb.append( "," );
		
		Alias fromAlias = getFromIDAlias();
		
		if( fromAlias != null )
		{
			sb.append( "'" );
			sb.append( fromAlias.getName() );
			sb.append( "'" );
		}
		sb.append( "," );

		if( getToID() != null )
		{
			sb.append( "'" );
			sb.append( getToID() );
			sb.append( "'" );
		}
		sb.append( "," );
		
		Alias toAlias = getToIDAlias();
		
		if( toAlias != null )
		{
			sb.append( "'" );
			sb.append( toAlias.getName() );
			sb.append( "'" );
		}
		sb.append( ",'" );
		sb.append( getChannel() );
		sb.append( "','" );
		sb.append( getFrequency() );
		sb.append( "'," );
		if( getDetails() != null )
		{
			sb.append( "'" );
			sb.append( getDetails() );
			sb.append( "'" );
		}
		
		return sb.toString();
	}
	
	public enum CallEventType
	{
	    ACKNOWLEDGE( "Acknowledge" ),
		CALL_DETECT( "Call Detect" ),
		CALL_START( "Call Start" ),
		CALL_START_UNIQUE_ID( "Call Start" ),
		CALL_START_NO_TUNER( "Call - No Tuner" ),
		CALL_END( "Call End" ),
		CALL_TIMEOUT( "Call Timeout" ),
		EMERGENCY( "EMERGENCY" ),
		GPS( "GPS" ),
		ID_ANI( "ANI" ),
		ID_UNIQUE( "Unique ID" ),
		PAGE( "Page" ),
		REGISTER( "Register" ),
		REGISTER_ESN( "ESN" ),
		SDM( "SDM" ),
		STATUS( "Status" ),
		UNKNOWN( "Unknown" );
		
		private String mLabel;
		
		private CallEventType( String label )
		{
			mLabel = label;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public String toString()
		{
			return mLabel;
		}
	}
}
