package io.github.dsheirer.module.decode.ip.cellocator;

/**
 * Provides fragment information for messages that are too long and have to be fragmented.
 */
public class PacketControl
{
    public enum Direction {TO_UNIT,FROM_UNIT};
    private static final int BIT_MASK_DIRECTION = 0x80;
    private static final int BIT_MASK_OUT_OF_SPACE = 0x40;
    private int mValue;

    /**
     * Constructs an instance
     * @param value of the fragment control field byte
     */
    public PacketControl(int value)
    {
        mValue = value;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" MODULAR PACKET ").append(getDirection());

        if(isOutOfSpace())
        {
            sb.append(" (MESSAGE EXCEEDED SPACE)");
        }

        return sb.toString();
    }

    /**
     * Indicates the origination of the message fragment
     * @return origination, unit or application
     */
    public Direction getDirection()
    {
        return isSet(BIT_MASK_DIRECTION) ? Direction.TO_UNIT : Direction.FROM_UNIT;
    }

    /**
     * Indicates if the message contents exceeded the available space in this message
     */
    public boolean isOutOfSpace()
    {
        return isSet(BIT_MASK_OUT_OF_SPACE);
    }

    /**
     * Indicates if the bits that are set in the bit mask are also set in the value field
     */
    private boolean isSet(int bitMask)
    {
        return (mValue &  bitMask) == bitMask;
    }
}
