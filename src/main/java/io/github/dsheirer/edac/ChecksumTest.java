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
package io.github.dsheirer.edac;

import java.util.BitSet;

public class ChecksumTest
{

    /**
     * @param args
     */
    public static void main( String[] args )
    {
        log( "START!" );
        
        String msg = "111000001000010010001110";
        log(  msg + " check:" + String.format( "%02X", Checksum.get7BitChecksum( load( msg ), ChecksumType.LTR ) ) );
        
        log( "DONE!" );
    }
    
    public static void log( String message )
    {
        System.out.println( message );
    }
    
    public static String getString( BitSet bitset, int length )
    {
        StringBuilder sb = new StringBuilder();
        for( int x = 0; x < length; x++ )
        {
            if( bitset.get( x ) )
            {
                sb.append( "1" );
            }
            else
            {
                sb.append( "0" );
            }
        }
        
        return sb.toString();
    }
    
    public static String getStringReverse( BitSet bitset, int length )
    {
        StringBuilder sb = new StringBuilder();
        for( int x = length - 1; x > -1; x-- )
        {
            if( bitset.get( x ) )
            {
                sb.append( "1" );
            }
            else
            {
                sb.append( "0" );
            }
        }
        
        return sb.toString();
    }
    
    
    public static BitSet load( String message )
    {
        BitSet retVal = new BitSet();
        
        for( int x = 0; x < message.length(); x++ )
        {
            if( message.substring( x, x + 1 ).contentEquals( "1" ) )
            {
                retVal.set( x );
            }
        }
        
        return retVal;
    }

    
}
