package io.github.dsheirer.dsp.window;

import java.util.EnumSet;

/**
 * Window types
 */
public enum WindowType
{
    BLACKMAN("Blackman"),
    BLACKMAN_HARRIS_4("Blackman-Harris 4"),
    BLACKMAN_HARRIS_7("Blackman-Harris 7"),
    BLACKMAN_NUTALL("Blackman-Nutall"),
    COSINE("Cosine"),
    FLAT_TOP("Flat Top"),
    HAMMING("Hamming"),
    HANN("Hann"),
    KAISER("Kaiser"),
    NUTALL("Nutall"),
    NONE("None");

    private String mLabel;

    WindowType(String label)
    {
        mLabel = label;
    }


    public String toString()
    {
        return mLabel;
    }

    public static final EnumSet<WindowType> NO_PARAMETER_WINDOWS = EnumSet.of(WindowType.BLACKMAN, WindowType.BLACKMAN_HARRIS_4,
            WindowType.BLACKMAN_HARRIS_7, WindowType.BLACKMAN_NUTALL, WindowType.COSINE, WindowType.FLAT_TOP,
            WindowType.HAMMING, WindowType.HANN, WindowType.NUTALL);
}
