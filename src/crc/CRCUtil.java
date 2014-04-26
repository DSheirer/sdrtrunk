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
package crc;

import java.util.BitSet;

public class CRCUtil
{
    public static BitSet check( BitSet originalMessage,
                         int messageLength,
                         BitSet polynomial,
                         int polynomialLength )
    {
        //Make a copy of the message, since we're going to modify it
        BitSet copy = originalMessage.get( 0, messageLength );

        //Iterate through the message bits
        for( int x = 0; x < messageLength; x++ )
        {
            //If we're aligned against a starting 1 in the message, divide it
            if( copy.get( 0 ) )
            {
                copy.xor( polynomial );
            }
            
            //Left shift the message
            copy = copy.get( 1, messageLength + polynomialLength - x );
        }
        
        return copy.get( 0, polynomialLength );
    }
    
    public static void log( String message )
    {
        System.out.println( message );
    }
    
    public enum Status
    {
        PASS, FAIL, BITINVERTED, WORDINVERTED;
    }

}
