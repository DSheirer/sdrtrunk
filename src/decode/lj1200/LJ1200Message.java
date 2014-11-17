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
package decode.lj1200;

import java.text.SimpleDateFormat;
import java.util.BitSet;

import map.Plottable;
import message.Message;
import alias.Alias;
import alias.AliasList;
import bits.BitSetBuffer;
import crc.CRC;
import crc.CRCFleetsync;

public class LJ1200Message extends Message
{
	public static int[] SYNC = { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15 };

	public static int[] VRC = { 16,17,18,19,20,21,22,23 };
	
	public static int[] LRC = { 24,25,26,27,28,29,30,31 };

	public static int[] FUNCTION = { 32,33,34,35 };
	
	public static int[] ADDRESS = { 36,37,38,39,40,41,42,43,44,45,46,47,
		48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63 };

	public static int[] CRC = { 64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79 };
	
	private static SimpleDateFormat mSDF = new SimpleDateFormat( "yyyyMMdd HHmmss" );

    private BitSetBuffer mMessage;
    private AliasList mAliasList;
    private CRC mCRC;
    
    public LJ1200Message( BitSetBuffer message, AliasList list )
    {
        mMessage = message;
        mAliasList = list;
        checkParity();
    }
    
    private void checkParity()
    {
    	mCRC = detectAndCorrect( 16, 79 );
    }
    
    private CRC detectAndCorrect( int start, int end )
    {
    	BitSet original = mMessage.get( start, end );

    	CRC crc = CRCFleetsync.check( original );

    	if( crc == crc.FAILED_PARITY )
    	{
    		int[] errorBitPositions = CRCFleetsync.findBitErrors( original );
    		
    		if( errorBitPositions != null )
    		{
    			for( int errorBitPosition: errorBitPositions )
    			{
    				mMessage.flip( start + errorBitPosition );
    			}
    			
    			crc = crc.CORRECTED;
    		}
    	}
    	
    	return crc;
    }
    
    public boolean isValid()
    {
    	return true;
    }
    
    public String getVRC()
    {
    	return mMessage.getHex( VRC, 2 );
    }
    
    public String getLRC()
    {
    	return mMessage.getHex( LRC, 2 );
    }
    
    public int getFunction()
    {
    	return mMessage.getInt( FUNCTION );
    }
    
    public String getAddress()
    {
    	return mMessage.getHex( ADDRESS, 7 );
    }
    
    @Override
    public String toString()
    {
    	StringBuilder sb = new StringBuilder();

    	sb.append( "VRC: " + getVRC() );
    	sb.append( " LRC: " + getLRC() );
    	sb.append( " FUNCTION: " + getFunction() );
    	sb.append( " ADDRESS:" + getAddress() );

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
	    return "LJ-1200";
    }

	@Override
    public String getEventType()
    {
	    return "MESSAGE";
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
		return getAddress();
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
    }

	@Override
    public Plottable getPlottable()
    {
		return null;
    }
}
