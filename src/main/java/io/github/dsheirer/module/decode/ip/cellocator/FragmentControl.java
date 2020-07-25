package io.github.dsheirer.module.decode.ip.cellocator;

/**
 * Provides fragment information for messages that are too long and have to be fragmented.
 */
public class FragmentControl
{
    public enum Origination{UNIT,APPLICATION};
    private static final int BIT_MASK_ORIGINATION = 0x80;
    private static final int BIT_MASK_LAST_FRAGMENT = 0x40;
    private static final int BIT_MASK_FRAGMENT_INDEX = 0x3F;
    private int mValue;

    /**
     * Constructs an instance
     * @param value of the fragment control field byte
     */
    public FragmentControl(int value)
    {
        mValue = value;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOrigination().name());
        sb.append(" ORIGINATED FRAGMENT #").append(getFragmentIndex());

        if(isLastFragment())
        {
            sb.append(" FINAL");
        }

        return sb.toString();
    }

    /**
     * Indicates the origination of the message fragment
     * @return origination, unit or application
     */
    public Origination getOrigination()
    {
        return isSet(BIT_MASK_ORIGINATION) ? Origination.UNIT : Origination.APPLICATION;
    }

    /**
     * Indicates if this is the last fragment
     */
    public boolean isLastFragment()
    {
        return isSet(BIT_MASK_LAST_FRAGMENT);
    }

    /**
     * Fragment index for this fragment, 0-31
     */
    public int getFragmentIndex()
    {
        return (mValue & BIT_MASK_FRAGMENT_INDEX);
    }

    /**
     * Indicates if the bits that are set in the bit mask are also set in the value field
     */
    private boolean isSet(int bitMask)
    {
        return (mValue &  bitMask) == bitMask;
    }
}
