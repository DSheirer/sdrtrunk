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
package decode.fleetsync1;

import java.text.SimpleDateFormat;
import java.util.Date;

import map.Plottable;
import message.Message;
import alias.Alias;
import bits.BitSetBuffer;

public class Fleetsync1Message extends Message
{
	private static SimpleDateFormat mSDF = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );

    private BitSetBuffer mMessage;
    
    public Fleetsync1Message( BitSetBuffer message )
    {
        mMessage = message;
    }
    
    public boolean isValid()
    {
        return true;
    }
    
    @Override
    public String toString()
    {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append( mSDF.format( new Date( System.currentTimeMillis() ) ) );
    	sb.append( " " );
    	sb.append( "FS1 [" + mMessage.toString() + "]" );

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
	    // TODO Auto-generated method stub
	    return null;
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
	    // TODO Auto-generated method stub
	    return null;
    }

	@Override
    public Alias getFromIDAlias()
    {
	    // TODO Auto-generated method stub
	    return null;
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
	    // TODO Auto-generated method stub
	    return null;
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
	    return null;
    }
}
