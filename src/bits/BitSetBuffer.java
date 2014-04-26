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
package bits;

import java.util.BitSet;

public class BitSetBuffer extends BitSet
{
    private static final long serialVersionUID = 1L;

    /**
     * Logical (ie constructed) size of this bitset, despite the actual size of
     * the super bitset that this class is based on
     */
    private int mSize = 0;
    
    /**
     * Pointer to the next fill index location, when adding bits to this bitset
     * one at a time.
     */
    private int mPointer = 0;

    /**
     * Bitset that buffers bits added one at a time, up to the size of the this
     * bitset. 
     * 
     * Note: the super class bitset behind this class may have a size larger 
     * that the size parameter specified.
     * @param size
     */
    public BitSetBuffer( int size )
    {
        super( size );
        mSize = size;
    }
    
    /**
     * Constructs a bitset buffer and preloads it with the bits contained in
     * the bitsToPreload parameter.  If the bitsToPreload are longer than the
     * size of the bitset, only those bits that fit will be preloaded
     * 
     * @param size
     * @param bitsToPreload
     */
    public BitSetBuffer( int size, boolean[] bitsToPreload )
    {
        this( size );
        
        int pointer = 0;
        
        while( !this.isFull() && pointer < bitsToPreload.length )
        {
            try
            {
                this.add( bitsToPreload[ pointer ] );
            }
            catch( BitSetFullException e )
            {
                e.printStackTrace();
            }
            
            pointer++;
        }
    }

    /**
     * Constructs a new BitSetBuffer from an existing one
     */
    private BitSetBuffer( BitSetBuffer toCopyFrom )
    {
        this( toCopyFrom.size() );
        this.or( toCopyFrom );
        this.mPointer = toCopyFrom.pointer();
    }
    
    /**
     * Current pointer index
     */
    public int pointer()
    {
        return mPointer;
    }

    /**
     * Static method to construct a new BitSetBuffer, preloaded with the bits
     * from the preload parameter, and then filled with the bits from the 
     * second bitsetbuffer parameter.
     * 
     * @param preloadBits - boolean array of bits to be prepended to the new
     *          bitset
     * @param bitsetToAppend - full bitset to be appended to the residual bits array 
     * @return - new Bitset preloaded with residual bits and new bitset
     */
    public static BitSetBuffer merge( boolean[] preloadBits, BitSetBuffer bitsetToAppend )
    {
        BitSetBuffer returnValue = new BitSetBuffer( preloadBits.length + bitsetToAppend.size(), preloadBits );

        int pointer = 0;
        
        while( pointer < bitsetToAppend.size() && !returnValue.isFull() )
        {
            try
            {
                returnValue.add( bitsetToAppend.get( pointer ) );
            }
            catch( BitSetFullException e )
            {
                e.printStackTrace();
            }
            
            pointer++;
        }
        
        return returnValue;
    }
    
    /**
     * Returns a (new) copy of this bitsetbuffer
     * @return
     */
    public BitSetBuffer copy()
    {
        return new BitSetBuffer( this );
    }
    
    public boolean isFull()
    {
        return mPointer >= mSize;
    }

    /**
     * Overrides the in-build size() method of the bitset and returns the value
     * specified at instantiation.  The actual bitset size may be larger than
     * this value, and that size is managed by the super class.
     */
    @Override
    public int size()
    {
        return mSize;
    }

    /**
     * Clears (sets to false or 0) the bits in this bitset and resets the
     * pointer to zero.
     */
    @Override
    public void clear()
    {
        this.clear( 0,  mSize );
        mPointer = 0;
    }

    /**
     * Adds a the bit parameters to this bitset, placing it in the index 
     * specified by mPointer, and incrementing mPointer to prepare for the next
     * call to this method
     * @param value
     * @throws BitSetFullException - if the size specified at construction is
     * exceeded.  Invoke full() to determine if the bitset is full either before
     * adding a new bit, or after adding a bit.
     */
    public void add( boolean value ) throws BitSetFullException
    {
        if( !isFull() )
        {
            this.set( mPointer++, value );
        }
        else
        {
            throw new BitSetFullException( "bitset is full -- contains " + ( mPointer + 1 ) + " bits" );
        }
    }
    
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        for( int x = 0; x < mSize; x++ )
        {
            sb.append( ( this.get( x ) ? "1" : "0" ) );
        }
        
        return sb.toString();
    }
    
    /**
     * Returns a boolean array from startIndex to end of the bitset
     */
    public boolean[] getBits( int startIndex )
    {
        return getBits( startIndex, mSize - 1 );
    }
    
    /**
     * Returns a boolean array of the right-most bitCount number of bits
     */
    public boolean[] right( int bitCount )
    {
        return getBits( mSize - bitCount - 1 );
    }
    
    /**
     * Returns a boolean array representing the bits located from startIndex
     * through endIndex
     */
    public boolean[] getBits( int startIndex, int endIndex )
    {
        boolean[] returnValue = null;
        
        if( startIndex >= 0 && 
            startIndex < endIndex && 
            endIndex < mSize )
        {
            returnValue = new boolean[ endIndex - startIndex + 1 ];

            int bitsetPointer = startIndex;
            int returnPointer = 0;
            
            while( bitsetPointer <= endIndex )
            {
                returnValue[ returnPointer ] = this.get( bitsetPointer );
                bitsetPointer++;
                returnPointer++;
            }
        }

        return returnValue;
    }
    
    /**
     * Returns the integer value represented by the bit array
     * @param bits - an array of bit positions that will be treated as if they
     * 			were contiguous bits, with index 0 being the MSB and index
     * 			length - 1 being the LSB
     * @return - integer value of the bit array
     */
    public int getInt( int[] bits )
    {
    	int retVal = 0;
    	
    	for( int x = 0; x < bits.length; x++ )
    	{
    		if( get( bits[ x ] ) )
    		{
    			retVal += 1<<( bits.length - 1 - x );
    		}
    	}
    	
    	return retVal;
    }
    
    public String getHex( int[] bits, int digitDisplayCount )
    {
    	int value = getInt( bits );
    	
    	return String.format( "%0" + digitDisplayCount + "X", value );
    }

    /**
     * Returns the integer value represented by the bit range
     * @param start - MSB of the integer
     * @param end - LSB of the integer
     * @return - integer value of the bit range
     */
    public int getInt( int start, int end )
    {
    	if( end - start > 32 )
    	{
    		throw new IllegalArgumentException( "BitSetBuffer - requested bit "
    				+ "length [" + ( end - start ) + "] is larger than an "
    						+ "integer (32 bits)" );
    	}
    	
    	int retVal = 0;
    	
    	for( int x = start; x <= end; x++ )
    	{
    		if( get( x ) )
    		{
    			retVal += 1<<( end - x );
    		}
    	}
    	
    	return retVal;
    }
    
    /**
     * Returns the long value represented by the bit range
     * @param start - MSB of the long
     * @param end - LSB of the long
     * @return - long value of the bit range
     */
    public long getLong( int start, int end )
    {
    	long retVal = 0;
    	
    	for( int x = start; x <= end; x++ )
    	{
    		if( get( x ) )
    		{
    			retVal += 1<<( end - x );
    		}
    	}
    	
    	return retVal;
    }
}
