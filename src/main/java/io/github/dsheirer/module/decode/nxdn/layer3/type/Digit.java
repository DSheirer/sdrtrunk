package io.github.dsheirer.module.decode.nxdn.layer3.type;

/**
 * Dialed digits enumeration
 */
public enum Digit
{
    FILLER(0, "-"),
    D1(1, "1"),
    D2(2, "2"),
    D3(3, "3"),
    D4(4, "4"),
    D5(5, "5"),
    D6(6, "6"),
    D7(7, "7"),
    D8(8, "8"),
    D9(9, "9"),
    D0(10, "0"),
    D_TAR(11, "*"),
    D_POUND(12, "#");

    final int mValue;
    final String mLabel;

    /**
     * Constructs an instance
     * @param value from the message
     * @param label to display
     */
    Digit(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Utility method to lookup the digit from the transmitted value.
     * @param value from the message
     * @return matching entry or FILLER
     */
    public static Digit fromValue(int value)
    {
        for(Digit digit: Digit.values())
        {
            if(digit.mValue == value)
            {
                return digit;
           }
        }

        return FILLER;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
