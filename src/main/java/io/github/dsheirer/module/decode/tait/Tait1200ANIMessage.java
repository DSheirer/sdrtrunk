/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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
package io.github.dsheirer.module.decode.tait;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.map.Plottable;
import io.github.dsheirer.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class Tait1200ANIMessage extends Message
{
	private final static Logger mLog = LoggerFactory.getLogger( Tait1200ANIMessage.class );

	public static int[] REVS_1 = { 0,1,2,3 };
	public static int[] SYNC = { 4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19 };
	public static int[] SIZE = { 20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35 };
	public static int[] FROM_DIGIT_1 = { 36,37,38,39,40,41,42,43 };
	public static int[] FROM_DIGIT_2 = { 44,45,46,47,48,49,50,51 };
	public static int[] FROM_DIGIT_3 = { 52,53,54,55,56,57,58,59 };
	public static int[] FROM_DIGIT_4 = { 60,61,62,63,64,65,66,67 };
	public static int[] FROM_DIGIT_5 = { 68,69,70,71,72,73,74,75 };
	public static int[] FROM_DIGIT_6 = { 76,77,78,79,80,81,82,83 };
	public static int[] FROM_DIGIT_7 = { 84,85,86,87,88,89,90,91 };
	public static int[] FROM_DIGIT_8 = { 92,93,94,95,96,97,98,99 };
	public static int[] CHECKSUM_1 = { 100,101,102,103,104,105,106,107,108,109,
		110,111,112,113,114,115 };

	public static int[] REVS_2 = { 116,117,118,119,120,121,122,123,124,125,126,127,
		128,129,130,131	};
	public static int[] SIZE_2 = { 188,189,190,191,192,193,194,195,196,197,198,
		199,200,201,202,203 };
	public static int[] TO_DIGIT_1 = { 204,205,206,207,208,209,210,211 };
	public static int[] TO_DIGIT_2 = { 212,213,214,215,216,217,218,219 };
	public static int[] TO_DIGIT_3 = { 220,221,222,223,224,225,226,227 };
	public static int[] TO_DIGIT_4 = { 228,229,230,231,232,233,234,235 };
	public static int[] TO_DIGIT_5 = { 236,237,238,239,240,241,242,243 };
	public static int[] TO_DIGIT_6 = { 244,245,246,247,248,249,250,251 };
	public static int[] TO_DIGIT_7 = { 252,253,254,255,256,257,258,259 };
	public static int[] TO_DIGIT_8 = { 260,261,262,263,264,265,266,267 };
	public static int[] UNKNOWN_1 = { 268,269,270,271,272,273,274,275 };
	public static int[] CHECKSUM_2 = { 276,277,278,279,280,281,282,283,284,285,
		286,287,288,289,290,291 };

	private static SimpleDateFormat mSDF = new SimpleDateFormat( "yyyyMMdd HHmmss" );

    private BinaryMessage mMessage;
    private AliasList mAliasList;
    private CRC mCRC;
    
    public Tait1200ANIMessage( BinaryMessage message, AliasList list )
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
    
	@Override
    public String getFromID()
    {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append( getCharacter( FROM_DIGIT_1 ) );
    	sb.append( getCharacter( FROM_DIGIT_2 ) );
    	sb.append( getCharacter( FROM_DIGIT_3 ) );
    	sb.append( getCharacter( FROM_DIGIT_4 ) );
    	sb.append( getCharacter( FROM_DIGIT_5 ) );
    	sb.append( getCharacter( FROM_DIGIT_6 ) );
    	sb.append( getCharacter( FROM_DIGIT_7 ) );
    	sb.append( getCharacter( FROM_DIGIT_8 ) );
    	
    	return sb.toString();
    }
	
	@Override
    public Alias getFromIDAlias()
    {
		if( mAliasList != null )
		{
			return mAliasList.getTalkgroupAlias( getFromID() );
		}
		
		return null;
    }
    
	@Override
    public String getToID()
    {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append( getCharacter( TO_DIGIT_1 ) );
    	sb.append( getCharacter( TO_DIGIT_2 ) );
    	sb.append( getCharacter( TO_DIGIT_3 ) );
    	sb.append( getCharacter( TO_DIGIT_4 ) );
    	sb.append( getCharacter( TO_DIGIT_5 ) );
    	sb.append( getCharacter( TO_DIGIT_6 ) );
    	sb.append( getCharacter( TO_DIGIT_7 ) );
    	sb.append( getCharacter( TO_DIGIT_8 ) );
    	
    	return sb.toString();
    }
	
	@Override
    public Alias getToIDAlias()
    {
		if( mAliasList != null )
		{
			return mAliasList.getTalkgroupAlias( getToID() );
		}
		
		return null;
    }
    
    public char getCharacter( int[] bits )
    {
    	int value = mMessage.getInt( bits );
    	
    	return (char)value;
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

    	sb.append( "ANI FROM:" );
    	sb.append( getFromID() );
    	sb.append( " TO:" );
    	sb.append( getToID() );

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
	    return "ANI";
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
	
	/**
	 * Provides a listing of aliases contained in the message.
	 */
	public List<Alias> getAliases()
	{
		List<Alias> aliases = new ArrayList<Alias>();
		
		Alias from = getFromIDAlias();
		
		if( from != null )
		{
			aliases.add( from );
		}

		Alias to = getToIDAlias();
		
		if( to != null )
		{
			aliases.add( to );
		}
		
		return aliases;
	}
}
