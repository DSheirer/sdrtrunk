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
package decode.mdc1200;

import java.text.SimpleDateFormat;
import java.util.Date;

import map.Plottable;
import message.Message;
import alias.Alias;
import alias.AliasList;
import bits.BitSetBuffer;

public class MDCMessage extends Message
{
	private static SimpleDateFormat mSDF = new SimpleDateFormat( "yyyyMMdd HHmmss" );

	private static int[] sSYNC1 = { 0,1,2,3,4,5,6,7,8,9,
									10,11,12,13,14,15,16,17,18,19,
									20,21,22,23,24,25,26,27,28,29,
									30,31,32,33,34,35,36,37,38,39 };
	
	private static int[] sOPCODE = { 47,46,45,44,43,42,41,40 };
	private static int sANI_FLAG = 40;
	private static int sDIRECTION_FLAG = 45;
	private static int sACKNOWLEDGE_REQUIRED_FLAG = 46;
	private static int sPACKET_TYPE_FLAG = 47;
	private static int sEMERGENCY_FLAG = 48;
	private static int[] sARGUMENT = { 49,50,51,52,53,54 };
	private static int sBOT_EOT_FLAG = 55;
	private static int[] sDIGIT_2 = { 59,58,57,56 };
	private static int[] sDIGIT_1 = { 63,62,61,60 };
	private static int[] sDIGIT_4 = { 67,66,65,64 };
	private static int[] sDIGIT_3 = { 71,70,69,68 };

    private BitSetBuffer mMessage;
    private AliasList mAliasList;
    
    public MDCMessage( BitSetBuffer message, AliasList list )
    {
        mMessage = message;
        mAliasList = list;
    }
    
    public boolean isValid()
    {
    	//TODO: add CRC and/or convolution decoding/repair
        return true;
    }

    public PacketType getPacketType()
    {
    	if( mMessage.get( sPACKET_TYPE_FLAG ) )
    	{
    		return PacketType.DATA;
    	}
    	else
    	{
    		return PacketType.CMND;
    	}
    }
    
    public Acknowledge getResponse()
    {
    	if( mMessage.get( sACKNOWLEDGE_REQUIRED_FLAG ) )
    	{
    		return Acknowledge.YES;
    	}
    	else
    	{
    		return Acknowledge.NO;
    	}
    }
    
    public Direction getDirection()
    {
    	if( mMessage.get( sDIRECTION_FLAG ) )
    	{
    		return Direction.OUT;
    	}
    	else
    	{
    		return Direction.IN;
    	}
    }
    
    public int getOpcode()
    {
    	return mMessage.getInt( sOPCODE );
    }
    
    public int getArgument()
    {
    	return mMessage.getInt( sARGUMENT );
    }
    
    public boolean isEmergency()
    {
    	return mMessage.get( sEMERGENCY_FLAG );
    }
    
    
    public boolean isANI()
    {
    	return mMessage.get( sANI_FLAG );
    }
    
    public boolean isBOT()
    {
    	return mMessage.get( sBOT_EOT_FLAG );
    }
    
    public boolean isEOT()
    {
    	return !mMessage.get( sBOT_EOT_FLAG );
    }
    
    public String getUnitID()
    {
    	StringBuilder sb = new StringBuilder();

    	sb.append( Integer.toHexString( mMessage.getInt( sDIGIT_1 ) ).toUpperCase() );
    	sb.append( Integer.toHexString( mMessage.getInt( sDIGIT_2 ) ).toUpperCase() );
    	sb.append( Integer.toHexString( mMessage.getInt( sDIGIT_3 ) ).toUpperCase() );
    	sb.append( Integer.toHexString( mMessage.getInt( sDIGIT_4 ) ).toUpperCase() );
    	
    	return sb.toString();
    }
    
    public MDCMessageType getMessageType()
    {
		switch( getOpcode() )
		{
			case 0:
				if( isEmergency() )
		    	{
		    		return MDCMessageType.EMERGENCY;
		    	}
			case 1:
				return MDCMessageType.ANI;
			default:
				return MDCMessageType.UNKNOWN;
		}
    }
    
    @Override
    public String toString()
    {
    	StringBuilder sb = new StringBuilder();

    	sb.append( mSDF.format( new Date( System.currentTimeMillis() ) ) );

    	sb.append( getMessage() );

    	sb.append( " [" + mMessage.toString() + "]" );

    	return sb.toString();
    }

    /**
     * Pads spaces onto the end of the value to make it 'places' long
     */
    public String pad( String value, int places, String padCharacter )
    {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append( value );
    	
    	while( sb.length() < places )
    	{
    		sb.append( padCharacter );
    	}
    	
    	return sb.toString();
    }
    
    /**
     * Pads an integer value with additional zeroes to make it decimalPlaces long
     */
    public String format( int number, int decimalPlaces )
    {
    	StringBuilder sb = new StringBuilder();

    	int paddingRequired = decimalPlaces - ( String.valueOf( number ).length() );

    	for( int x = 0; x < paddingRequired; x++)
    	{
    		sb.append( "0" );
    	}
    	
    	sb.append( number );
    	
    	return sb.toString();
    }
    
    @SuppressWarnings( "unused" )
    private int getInt( int[] bits )
    {
    	int retVal = 0;
    	
    	for( int x = 0; x < bits.length; x++ )
    	{
    		if( mMessage.get( bits[ x ] ) )
    		{
    			retVal += 1<<x;
    		}
    	}
    	
    	return retVal;
    }

	@Override
    public String getBinaryMessage()
    {
		return mMessage.toString();
	}

	@Override
    public String getProtocol()
    {
	    return "MDC-1200";
    }

	@Override
    public String getEventType()
    {
	    // TODO Auto-generated method stub
	    return null;
    }

	@Override
    public String getFromID()
    {
	    return getUnitID();
    }

	@Override
    public Alias getFromIDAlias()
    {
		return mAliasList.getMDC1200Alias( getFromID() );
    }

	@Override
    public String getToID()
    {
	    // TODO Auto-generated method stub
	    return null;
    }

	@Override
    public Alias getToIDAlias()
    {
	    // TODO Auto-generated method stub
	    return null;
    }

	@Override
    public String getMessage()
    {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append( "MDC1200 UNIT:" + getUnitID() );
    	if( isEmergency() )
    	{
    		sb.append( " **EMERGENCY**" );
    	}
    	
    	if( isBOT() )
    	{
    		sb.append( " BOT" );
    	}
    	
    	if( isEOT() )
    	{
    		sb.append( " EOT" );
    	}
    	
    	sb.append( " OPCODE:" + format( getOpcode(), 2 ) );
    	sb.append( " ARG:" + format( getArgument(), 3 ) );
    	sb.append( " TYPE:" + getPacketType().toString() );
    	sb.append( " ACK:" + getResponse().toString() );
    	sb.append( " DIR:" + pad( getDirection().toString(), 3, " " ) );

    	return sb.toString();
    }

	@Override
    public String getErrorStatus()
    {
	    // TODO Auto-generated method stub
	    return null;
    }

	@Override
    public Plottable getPlottable()
    {
	    // TODO Auto-generated method stub
	    return null;
    }
	
	private enum PacketType { CMND, DATA };
	private enum Acknowledge { YES, NO };
	private enum Direction { IN, OUT };
}
