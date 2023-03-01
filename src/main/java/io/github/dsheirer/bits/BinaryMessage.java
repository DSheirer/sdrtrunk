/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.bits;

import io.github.dsheirer.edac.CRC;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryMessage extends BitSet
{
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(BinaryMessage.class);
    private static final int[] CHARACTER_7_BIT = new int[]{0, 1, 2, 3, 4, 5, 6};
    private static final int[] CHARACTER_8_BIT = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
    private static final String UTF_8 = "UTF-8";
    private static final String GB2312 = "GB2312";


    /**
     * Logical (ie constructed) size of this bitset, despite the actual size of
     * the super bitset that this class is based on
     */
    private int mSize;

    /**
     * Pointer to the next fill index location, when adding bits to this bitset
     * one at a time.
     */
    private int mPointer = 0;

    /**
     * Used for temporary storage of CRC check results when we're passing this
     * message to an EDAC function.
     */
    private CRC mCRC;

    /**
     * Constructs a bitset that buffers bits added one at a time, up to the size
     * of the this bitset.
     * <p>
     * Note: the super class bitset behind this class may have a size larger
     * that the size parameter specified.
     *
     * @param size of constructed bitset
     */
    public BinaryMessage(int size)
    {
        super(size);
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
    public BinaryMessage(int size, boolean[] bitsToPreload)
    {
        this(size);

        int pointer = 0;

        while(!this.isFull() && pointer < bitsToPreload.length)
        {
            try
            {
                this.add(bitsToPreload[pointer]);
            }
            catch(BitSetFullException e)
            {
                e.printStackTrace();
            }

            pointer++;
        }
    }

    /**
     * Constructs a new BitSetBuffer from an existing one
     */
    private BinaryMessage(BinaryMessage toCopyFrom)
    {
        this(toCopyFrom.size());
        this.or(toCopyFrom);
        this.mPointer = toCopyFrom.pointer();
        this.mCRC = toCopyFrom.mCRC;
        this.mSize = toCopyFrom.mSize;
    }

    public BinaryMessage(BitSet bitset, int size)
    {
        this(size);
        this.or(bitset);
        this.mPointer = size - 1;
    }

    public BinaryMessage(byte[] data)
    {
        this(BitSet.valueOf(data), data.length * 8);
    }

    /**
     * Returns a mew binary message containing the bits from (inclusive) to
     * end (exclusive).
     *
     * @param start
     * @param end
     * @return
     */
    public BinaryMessage getSubMessage(int start, int end)
    {
        BitSet subset = this.get(start, end);

        return new BinaryMessage(subset, end - start);
    }

    public CRC getCRC()
    {
        return mCRC;
    }

    public void setCRC(CRC crc)
    {
        mCRC = crc;
    }

    /**
     * Indicates the number of bit errors that were corrected in this message.
     *
     * @return number of corrected bits in this message
     * @see CorrectedBinaryMessage subclass that supports setting this value
     */
    public int getCorrectedBitCount()
    {
        return 0;
    }

    /**
     * Current pointer index
     */
    public int pointer()
    {
        return mPointer;
    }

    /**
     * Sets the pointer to a specific value
     *
     * @param index
     */
    public void setPointer(int index)
    {
        mPointer = index;
    }

    /**
     * Moves the current pointer position left (negative adjustment) or
     * right (positive adjustment)
     */
    public void adjustPointer(int adjustment)
    {
        mPointer += adjustment;
    }

    /**
     * Static method to construct a new BitSetBuffer, preloaded with the bits
     * from the preload parameter, and then filled with the bits from the
     * second bitsetbuffer parameter.
     *
     * @param preloadBits - boolean array of bits to be prepended to the new
     * bitset
     * @param bitsetToAppend - full bitset to be appended to the residual bits array
     * @return - new Bitset preloaded with residual bits and new bitset
     */
    public static BinaryMessage merge(boolean[] preloadBits, BinaryMessage bitsetToAppend)
    {
        BinaryMessage returnValue = new BinaryMessage(preloadBits.length + bitsetToAppend.size(), preloadBits);

        int pointer = 0;

        while(pointer < bitsetToAppend.size() && !returnValue.isFull())
        {
            try
            {
                returnValue.add(bitsetToAppend.get(pointer));
            }
            catch(BitSetFullException e)
            {
                e.printStackTrace();
            }

            pointer++;
        }

        return returnValue;
    }

    /**
     * @return a (new) copy of this binary message
     */
    public BinaryMessage copy()
    {
        return new BinaryMessage(this);
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

    public void setSize(int size)
    {
        mSize = size;
    }

    /**
     * Clears (sets to false or 0) the bits in this bitset and resets the
     * pointer to zero.
     */
    @Override
    public void clear()
    {
        this.clear(0, mSize);
        mPointer = 0;
    }

    /**
     * Adds a the bit parameters to this bitset, placing it in the index
     * specified by mPointer, and incrementing mPointer to prepare for the next
     * call to this method
     *
     * @param value
     * @throws BitSetFullException - if the size specified at construction is
     *                             exceeded.  Invoke full() to determine if the bitset is full either before
     *                             adding a new bit, or after adding a bit.
     */
    public void add(boolean value) throws BitSetFullException
    {
        if(!isFull())
        {
            this.set(mPointer++, value);
        }
        else
        {
            throw new BitSetFullException("bitset is full -- contains " +
                (mPointer + 1) + "bits");
        }
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        for(int x = 0; x < mSize; x++)
        {
            sb.append((this.get(x) ? "1" : "0"));
        }

        return sb.toString();
    }

    public String toHexString()
    {
        StringBuilder sb = new StringBuilder();

        for(int x = 0; x < size(); x += 4)
        {
            sb.append(getNibbleAsHex(x));
        }

        return sb.toString();
    }

    /**
     * Overrides the parent method which returns a BitSet so that we can return a BinaryMessage
     * @param from
     * @param to
     * @return
     */
    @Override
    public BinaryMessage get(int from, int to)
    {
        return new BinaryMessage(super.get(from, to), (to - from));
    }

    /**
     * Returns this bitset as an array of integer ones and zeros
     */
    public int[] toIntegerArray()
    {
        int[] values = new int[mSize];

        for(int i = nextSetBit(0); i >= 0 && i < mSize; i = nextSetBit(i + 1))
        {
            values[i] = 1;
        }

        return values;
    }

    /**
     * Returns this message as a little endian byte array.  Extra 0 bits will be padded to the end to make the overall
     * length a multiple of 8.
     *
     * @return little endian message byte array
     */
    public byte[] toByteArray()
    {
        int length = size() / 8;

        if(length * 8 < size())
        {
            length++;
        }

        byte[] bytes = new byte[length];

        int pointer = 0;

        for(int x = 0; x < length; x++)
        {
            bytes[x] = getByte(pointer);
            pointer += 8;
        }

        return bytes;
    }

    /**
     * Returns this bitset as a reversed bit order array of integer ones and zeros
     * from the specified index range
     */
    public int[] toReverseIntegerArray(int start, int end)
    {
        int[] values = new int[end - start + 1];

        for(int i = nextSetBit(start); i >= start && i <= end; i = nextSetBit(i + 1))
        {
            values[end - i] = 1;
        }

        return values;
    }

    /**
     * Returns a boolean array from startIndex to end of the bitset
     */
    public boolean[] getBits(int startIndex)
    {
        return getBits(startIndex, mSize - 1);
    }

    /**
     * Returns a boolean array of the right-most bitCount number of bits
     */
    public boolean[] right(int bitCount)
    {
        return getBits(mSize - bitCount - 1);
    }

    /**
     * Returns a boolean array representing the bits located from startIndex
     * through endIndex
     */
    public boolean[] getBits(int startIndex, int endIndex)
    {
        boolean[] returnValue = null;

        if(startIndex >= 0 &&
            startIndex < endIndex &&
            endIndex < mSize)
        {
            returnValue = new boolean[endIndex - startIndex + 1];

            int bitsetPointer = startIndex;
            int returnPointer = 0;

            while(bitsetPointer <= endIndex)
            {
                returnValue[returnPointer] = this.get(bitsetPointer);
                bitsetPointer++;
                returnPointer++;
            }
        }

        return returnValue;
    }

    /**
     * Returns the integer value represented by the bit array
     *
     * @param bits - an array of bit positions that will be treated as if they
     * were contiguous bits, with index 0 being the MSB and index
     * length - 1 being the LSB
     * @return - integer value of the bit array
     */
    public int getInt(int[] bits)
    {
        if(bits.length > 32)
        {
            throw new IllegalArgumentException("Overflow - must be 32 bits "
                + "or less to fit into a primitive integer value");
        }

        int value = 0;

        for(int index : bits)
        {
            value = Integer.rotateLeft(value, 1);

            if(get(index))
            {
                value++;
            }
        }

        return value;
    }

    /**
     * Returns the integer value represented by the bit array
     *
     * @param bits - an array of bit positions that will be treated as if they
     * were contiguous bits, with index 0 being the MSB and index
     * length - 1 being the LSB
     * @param offset to apply to each of the bit position in the bits array.
     * @return - integer value of the bit array
     */
    public int getInt(int[] bits, int offset)
    {
        if(bits.length > 32)
        {
            throw new IllegalArgumentException("Overflow - must be 32 bits "
                + "or less to fit into a primitive integer value");
        }

        int value = 0;

        for(int index : bits)
        {
            value = Integer.rotateLeft(value, 1);

            if(get(index + offset))
            {
                value++;
            }
        }

        return value;
    }

    public void setInt(int value, int[] indices)
    {
        for(int x = 0; x < indices.length; x++)
        {
            int mask = 1 << (indices.length - x - 1);

            if((value & mask) == mask)
            {
                set(indices[x]);
            }
            else
            {
                clear(indices[x]);
            }
        }
    }

    /**
     * Returns the byte value represented by the bit array
     *
     * @param bits - an array of bit positions that will be treated as if they
     * were contiguous bits, with index 0 being the MSB and index
     * length - 1 being the LSB
     * @return - byte value of the bit array
     */
    public byte getByte(int[] bits)
    {
        if(bits.length != 8)
        {
            throw new IllegalArgumentException("Invalid - there must be 8"
                + "indexes to form a proper byte");
        }

        int value = 0;

        for(int index : bits)
        {
            value = Integer.rotateLeft(value, 1);

            if(get(index))
            {
                value++;
            }
        }

        return (byte)(value & 0xFF);
    }

    /**
     * Returns the byte value represented by the bit array
     *
     * @param bits - an array of bit positions that will be treated as if they
     * were contiguous bits, with index 0 being the MSB and index
     * length - 1 being the LSB
     * @param offset to apply to each of the bit positions
     * @return - byte value of the bit array
     */
    public byte getByte(int[] bits, int offset)
    {
        if(bits.length != 8)
        {
            throw new IllegalArgumentException("Invalid - there must be 8"
                + "indexes to form a proper byte");
        }

        int value = 0;

        for(int index : bits)
        {
            value = Integer.rotateLeft(value, 1);

            if(get(index + offset))
            {
                value++;
            }
        }

        return (byte)(value & 0xFF);
    }

    /**
     * Returns the byte value contained between index and index + 7 bit positions.  If the length of this message is
     * shorter than index + 7, then the least significant bits are set to zero in the returned value.
     *
     * @param startIndex specifying the start of the byte value
     * @return byte value contained at index <> index + 7 bit positions
     */
    public byte getByte(int startIndex)
    {
        int value = 0;

        for(int x = 0; x < 8; x++)
        {
            value <<= 1;

            int index = startIndex + x;

            if(index <= size() && get(index))
            {
                value++;
            }
        }

        return (byte)value;
    }

    /**
     * Converts the message to a byte array.
     *
     * Note: BitSet.toByteArray() outputs the bytes in big-endian (inverted) order.  This method retains the correct
     * endianness where bit 0 is the most significant bit of the first byte.
     */
    public byte[] getBytes()
    {
        byte[] bytes = new byte[(int)FastMath.ceil((double)size() / 8.0)];

        for(int x = 0; x < bytes.length; x++)
        {
            bytes[x] = getByte(x * 8);
        }

        return bytes;
    }

    /**
     * Returns the 4-bit nibble value contained between index and index + 3 bit positions.  If the length of this
     * message is shorter than index + 3, then the least significant bits are set to zero in the returned value.
     *
     * @param startIndex specifying the start of the byte value
     * @return nibble value contained at index <> index + 3 bit positions
     */
    public int getNibble(int startIndex)
    {
        int value = 0;

        for(int x = 0; x < 4; x++)
        {
            value <<= 1;

            int index = startIndex + x;

            if(index <= size() && get(index))
            {
                value++;
            }
        }

        return value;
    }

    /**
     * Sets the byte value at index position through index + 7 position.
     *
     * @param index bit position where to write the  MSB of the byte value
     * @param value to write at the index through index + 7 bit positions
     */
    public void setByte(int index, byte value)
    {
        Validate.isTrue((index + 8) <= size());

        int mask = 0x80;

        for(int x = 0; x < 8; x++)
        {
            if((mask & value) == mask)
            {
                set(index + x);
            }
            else
            {
                clear(index + x);
            }

            mask = mask >> 1;
        }
    }

    /**
     * Returns the long value represented by the bit array
     *
     * @param bits - an array of bit positions that will be treated as if they
     * were contiguous bits, with index 0 being the MSB and index
     * length - 1 being the LSB
     * @return - integer value of the bit array
     */
    public long getLong(int[] bits)
    {
        if(bits.length > 64)
        {
            throw new IllegalArgumentException("Overflow - must be 64 bits "
                + "or less to fit into a primitive long value");
        }

        long value = 0;

        for(int index : bits)
        {
            value = Long.rotateLeft(value, 1);

            if(get(index))
            {
                value++;
            }
        }

        return value;
    }

    /**
     * Returns the long value represented by the bit array
     *
     * @param bits - an array of bit positions that will be treated as if they
     * were contiguous bits, with index 0 being the MSB and index
     * length - 1 being the LSB
     * @param offset to apply to each of the bits indices
     * @return - integer value of the bit array
     */
    public long getLong(int[] bits, int offset)
    {
        if(bits.length > 64)
        {
            throw new IllegalArgumentException("Overflow - must be 64 bits "
                + "or less to fit into a primitive long value");
        }

        long value = 0;

        for(int index : bits)
        {
            value = Long.rotateLeft(value, 1);

            if(get(index + offset))
            {
                value++;
            }
        }

        return value;
    }

    /**
     * Returns the bit values between start and end (inclusive) bit indices.  If the overall length of the bit sequence
     * is not a multiple of 8 bits, the value is zero padded with least significant bits to make it a multiple of 8.
     * @param start index
     * @param end index
     * @return hexadecimal representation of the bit sequence
     */
    public String getHex(int start, int end)
    {
        StringBuilder sb = new StringBuilder();

        for(int x = start; x <= end; x += 4)
        {
            sb.append(getNibbleAsHex(x));
        }

        return sb.toString();
    }

    /**
     * Format the byte value that starts at the specified index as hexadecimal.  If the length of the message is less
     * than the start index plus 7 bits, then the value represents those bits as high-order bits with zero padding in
     * the least significant bits to make up the 8 bit value.
     * @param startIndex of the byte.
     * @return hexadecimal representation of the byte value
     */
    public String getNibbleAsHex(int startIndex)
    {
        int value = getNibble(startIndex);
        return Integer.toHexString(value).toUpperCase();
    }

    /**
     * Converts up to 63 bits from the bit array into an integer and then
     * formats the value into hexadecimal, prefixing the value with zeros to
     * provide a total length of digitDisplayCount;
     *
     * @param bits
     * @param digitDisplayCount
     * @return
     */
    public String getHex(int[] bits, int digitDisplayCount)
    {
        if(bits.length <= 32)
        {
            int value = getInt(bits);

            return String.format("%0" + digitDisplayCount + "X", value);
        }
        else if(bits.length <= 64)
        {
            long value = getLong(bits);

            return String.format("%0" + digitDisplayCount + "X", value);
        }
        else
        {
            throw new IllegalArgumentException("BitSetBuffer.getHex() "
                + "maximum array length is 63 bits");
        }
    }

    /**
     * Returns the int value represented by the bit range.  This method will
     * parse the bits in big endian or little endian format.  The start value
     * represents the MSB and the end value represents the LSB of the value.
     *
     * start < end: little endian interpretation
     * end < start: big endian interpretation
     *
     * @param start - MSB of the value
     * @param end - LSB of the value
     * @return - int value of the bit range
     */
    public int getInt(int start, int end)
    {
        if(FastMath.abs(end - start) > 32)
        {
            throw new IllegalArgumentException("Overflow - must be 32 bits "
                + "or less to fit into a primitive integer value");
        }

        int value = 0;

        if(start < end)
        {
            for(int x = start; x <= end; x++)
            {
                value = Integer.rotateLeft(value, 1);

                if(get(x))
                {
                    value++;
                    ;
                }
            }
        }
        else
        {
            for(int x = end; x >= start; x--)
            {
                value = Integer.rotateLeft(value, 1);

                if(get(x))
                {
                    value++;
                    ;
                }
            }
        }

        return value;
    }

    /**
     * Returns the twos-complement value of the bits between start and end, inclusive
     * @param start bit position
     * @param end bit position
     * @return twos complement value
     */
    public int getTwosComplement(int start, int end)
    {
        BinaryMessage sub = getSubMessage(start, end);
        boolean negative = sub.get(start);
        sub.flip(0, sub.size());

        int value = sub.getInt(0, sub.size()) + 1;
        value *= (negative ? -1 : 1);
        return value;
    }


    /**
     * Returns the long value represented by the bit range.  This method will
     * parse the bits in big endian or little endian format.  The start value
     * represents the MSB and the end value represents the LSB of the value.
     *
     * start < end: little endian interpretation
     * end < start: big endian interpretation
     *
     * @param start - MSB of the value
     * @param end - LSB of the value
     * @return - long value of the bit range
     */
    public long getLong(int start, int end)
    {
        if(FastMath.abs(end - start) > 64)
        {
            throw new IllegalArgumentException("Overflow - must be 64 bits "
                + "or less to fit into a primitive long value");
        }

        long value = 0;

        if(start < end)
        {
            for(int x = start; x <= end; x++)
            {
                value = Long.rotateLeft(value, 1);

                if(get(x))
                {
                    value++;
                }
            }
        }
        else
        {
            for(int x = end; x >= start; x--)
            {
                value = Long.rotateLeft(value, 1);

                if(get(x))
                {
                    value++;
                }
            }
        }

        return value;
    }

    /**
     * Creates a buffer of size=width and fills the buffer with the fill value
     *
     * @param width - size of the buffer
     * @param fill - initial fill value
     * @return - filled buffer
     */
    public static BinaryMessage getBuffer(int width, long fill)
    {
        BinaryMessage buffer = new BinaryMessage(width);

        buffer.load(0, width, fill);

        return buffer;
    }

    /**
     * Loads the value into the buffer starting at the offset index, and
     * assuming that the value represents (width) number of bits.  The MSB of
     * the value will be located at the offset and the LSB of the value will
     * be located at ( offset + width ).
     *
     * @param offset - starting bit index for the MSB of the value
     * @param width - representative bit width of the value
     * @param value - value to be loaded into the buffer
     */
    public void load(int offset, int width, long value)
    {
        for(int x = 0; x < width; x++)
        {
            long mask = Long.rotateLeft(1, width - x - 1);

            if((mask & value) == mask)
            {
                set(offset + x);
            }
            else
            {
                clear(offset + x);
            }
        }
    }

    /**
     * Loads the binary message into this message starting at the offset.  Note: this method
     * does not do any length checking for the message being loaded and if that message is longer
     * than this message, it would be loaded outside of the size (length) boundary of this message, but will not
     * produce an error.
     *
     * @param offset into this message to load the binary message
     * @param binaryMessage to load
     */
    public void load(int offset, BinaryMessage binaryMessage)
    {
        for(int x = 0; x < binaryMessage.size(); x++)
        {
            if(binaryMessage.get(x))
            {
                set(x + offset);
            }
            else
            {
                clear(x + offset);
            }
        }
    }

    /**
     * Generates an array of message bit position indexes to support accessing
     * a contiguous field value
     *
     * @param start - starting bit position of the field
     * @param length - field length
     * @return - array of field indexes
     */
    public static int[] getFieldIndexes(int start, int length, boolean bigEndian)
    {
        int[] checksumIndexes = new int[length];

        for(int x = 0; x < length; x++)
        {
            if(bigEndian)
            {
                checksumIndexes[length - x - 1] = start + x;
            }
            else
            {
                checksumIndexes[x] = start + x;
            }
        }

        return checksumIndexes;
    }

    /**
     * Creates a binary message loaded with the byte array
     * @param bytes to load
     * @return loaded binary message
     */
    public static BinaryMessage from(byte[] bytes)
    {
        BinaryMessage message = new BinaryMessage(bytes.length * 8);

        for(int x = 0; x < bytes.length; x++)
        {
            message.setByte(x * 8, bytes[x]);
        }

        return message;
    }

    /**
     * Creates a bitsetbuffer loaded from a string of zeros and ones
     *
     * @param message - string containing only zeros and ones
     * @return - loaded buffer
     */
    public static BinaryMessage load(String message)
    {
        if(!message.matches("[01]*"))
        {
            throw new IllegalArgumentException(
                "Message must contain only zeros and ones");
        }

        BinaryMessage buffer = new BinaryMessage(message.length());

        for(int x = 0; x < message.length(); x++)
        {
            if(message.substring(x, x + 1).contentEquals("1"))
            {
                buffer.set(x);
            }
        }

        return buffer;
    }

    /**
     * Loads the hexadecimal text into a binary message
     * @param hex text to load from
     * @return loaded binary message
     */
    public static BinaryMessage loadHex(String hex)
    {
        if(!hex.matches("[0-9A-F]*"))
        {
            throw new IllegalArgumentException("Message must contain only 0-9 and A-F hexadecimal characters");
        }

        int length = hex.length() / 2;

        if(length * 2 < hex.length())
        {
            length++;
        }

        BinaryMessage buffer = new BinaryMessage(length * 8);

        int bufferOffset = 0;
        for(int x = 0; x < hex.length(); x += 2)
        {
            int endIndex = x + 2;
            if(endIndex > hex.length())
            {
                endIndex--;
            }

            String rawHex = hex.substring(x, endIndex);

            if(rawHex.length() == 1)
            {
                rawHex = rawHex + "0";
            }

            int value = Integer.parseInt(rawHex, 16);
            buffer.setByte(bufferOffset, (byte)(value & 0xFF));
            bufferOffset += 8;
        }

        return buffer;
    }

    /**
     * Left rotates the bits between start and end indices, number of places.
     */
    public void rotateLeft(int places, int startIndex, int endIndex)
    {
        for(int x = 0; x < places; x++)
        {
            rotateLeft(startIndex, endIndex);
        }
    }

    /**
     * Left rotates the bits between start and end and wraps the left-most
     * bit around to the end.
     */
    public void rotateLeft(int startIndex, int endIndex)
    {
        boolean wrapBit = get(startIndex);

        for(int x = startIndex; x < endIndex; x++)
        {
            if(get(x + 1))
            {
                set(x);
            }
            else
            {
                clear(x);
            }
        }

        if(wrapBit)
        {
            set(endIndex);
        }
        else
        {
            clear(endIndex);
        }
    }

    /**
     * Right rotates the bits between start and end indices, number of places.
     */
    public void rotateRight(int places, int startIndex, int endIndex)
    {
        for(int x = 0; x < places; x++)
        {
            rotateRight(startIndex, endIndex);
        }
    }

    /**
     * Right rotates the bits between start and end and wraps the right-most
     * bit around to the start.
     */
    public void rotateRight(int startIndex, int endIndex)
    {
        boolean wrapBit = get(endIndex);

        for(int x = endIndex - 1; x >= startIndex; x--)
        {
            if(get(x))
            {
                set(x + 1);
            }
            else
            {
                clear(x + 1);
            }
        }

        if(wrapBit)
        {
            set(startIndex);
        }
        else
        {
            clear(startIndex);
        }
    }

    /**
     * Performs exclusive or of the value against this bitset starting at the
     * offset position using width bits from the value.
     */
    public void xor(int offset, int width, int value)
    {
        BinaryMessage mask = new BinaryMessage(this.size());

        mask.load(offset, width, value);

        this.xor(mask);
    }

    /**
     * Parse ISO-7 bit characters from the message starting at the offset.
     * @param offset where to start parsing.
     * @param characterCount number of characters to parse
     * @return parsed message.
     */
    public String parseISO7(int offset, int characterCount)
    {
        byte[] bytes = new byte[characterCount];

        for(int x = 0; x < characterCount; x++)
        {
            bytes[x] = getByte(CHARACTER_7_BIT, x * 7 + offset);
        }

        return new String(bytes);
    }

    /**
     * Parse ISO-8 bit characters from the message starting at the offset.
     * @param offset where to start parsing.
     * @param characterCount number of characters to parse
     * @return parsed message.
     */
    public String parseISO8(int offset, int characterCount)
    {
        byte[] bytes = new byte[characterCount];

        for(int x = 0; x < characterCount; x++)
        {
            bytes[x] = getByte(CHARACTER_8_BIT, x * 8 + offset);
        }

        return new String(bytes);
    }

    /**
     * Parse UTF-8 characters from the message starting at the offset.
     * @param offset where to start parsing.
     * @param characterCount number of characters to parse
     * @return parsed message.
     * @throws UnsupportedEncodingException if UTF-8 encoding is not supported.
     */
    public String parseUTF8(int offset, int characterCount) throws UnsupportedEncodingException
    {
        byte[] bytes = new byte[characterCount];

        for(int x = 0; x < characterCount; x++)
        {
            bytes[x] = getByte(CHARACTER_8_BIT, x * 8 + offset);
        }

        return new String(bytes, UTF_8);
    }

    /**
     * Parse GB2312 Simplified Chinese Characters from the message starting at the offset.
     * @param offset where to start parsing.
     * @param characterCount number of characters to parse
     * @return parsed message.
     * @throws UnsupportedEncodingException if GB2312 encoding is not supported.
     */
    public String parseGB2312(int offset, int characterCount) throws UnsupportedEncodingException
    {
        int length = characterCount * 2;
        byte[] bytes = new byte[length];

        for(int x = 0; x < length; x++)
        {
            bytes[x] = getByte(CHARACTER_8_BIT, x * 8 + offset);
        }

        return new String(bytes, GB2312);
    }

    /**
     * Parse 16-bit unicode characters from the message starting at the offset.
     * @param offset where to start parsing.
     * @param characterCount number of characters to parse
     * @return parsed message.
     */
    public String parseUnicode(int offset, int characterCount)
    {
        int length = characterCount * 2;
        byte[] bytes = new byte[length];

        for(int x = 0; x < length; x++)
        {
            bytes[x] = getByte(CHARACTER_8_BIT, x * 8 + offset);
        }

        return new String(bytes);
    }

    /**
     * Parse 4-bit Binary Coded Decimal (BCD) characters from the message starting at the offset.
     * @param offset where to start parsing.
     * @param characterCount number of characters to parse
     * @return parsed message.
     */
    public String parseBCD4(int offset, int characterCount)
    {
        StringBuilder sb = new StringBuilder();

        int count = 0;

        while(count < characterCount)
        {
            int nibble = getNibble(offset);
            //This will parse digits 0-9 and anything higher will parse as hexadecimal A-F
            sb.append(Integer.toHexString(nibble).toUpperCase());
            offset += 4;
            count++;
        }

        return sb.toString();
    }
}
