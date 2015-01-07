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
package decode.tait;

import java.text.SimpleDateFormat;

import map.Plottable;
import message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.Alias;
import alias.AliasList;
import bits.BitSetBuffer;
import edac.CRC;

public class Tait1200MessageB extends Message
{
	private final static Logger mLog = LoggerFactory.getLogger( Tait1200MessageB.class );

	public static int[] SYNC = { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15 };

	private static SimpleDateFormat mSDF = new SimpleDateFormat( "yyyyMMdd HHmmss" );

    private BitSetBuffer mMessage;
    private AliasList mAliasList;
    private CRC mCRC;
    
    public Tait1200MessageB( BitSetBuffer message, AliasList list )
    {
    	mMessage = message;
        mAliasList = list;
        
//        checkCRC();
//
//        switch( mCRC )
//        {
//			case CORRECTED:
//	        	mLog.debug( "CORR:" + message.toString() );
//				break;
//			case FAILED_CRC:
//	        	mLog.debug( "FAIL:" + message.toString() );
//				break;
//			case PASSED:
//	        	mLog.debug( "PASS:" + message.toString() );
//				break;
//        }
    }
    
    private void checkCRC()
    {
//    	mCRC = CRCLJ.checkAndCorrect( mMessage );
    }
    
    public boolean isValid()
    {
//    	return mCRC == CRC.PASSED || mCRC == CRC.CORRECTED;
    	return true;
    }
    
    @Override
    public String toString()
    {
    	StringBuilder sb = new StringBuilder();

    	sb.append( "TAIT MESSAGE B " );
    	sb.append( mMessage.toString() );

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
    
	@Override
    public String getBinaryMessage()
    {
		return mMessage.toString();
	}

	@Override
    public String getProtocol()
    {
	    return "Tait-1200";
    }

	@Override
    public String getEventType()
    {
	    return "Unknown";
    }

	@Override
    public String getFromID()
    {
		return null;
    }

	@Override
    public Alias getFromIDAlias()
    {
		return null;
    }

	@Override
	public String getToID()
	{
		return null;
	}

	@Override
    public Alias getToIDAlias()
    {
		return null;
    }
	
	@Override
    public String getMessage()
    {
		return toString();
    }
	
	@Override
    public String getErrorStatus()
    {
		return "UNKNOWN";
//	    return mCRC.getDisplayText();
    }

	@Override
    public Plottable getPlottable()
    {
		return null;
    }
}
