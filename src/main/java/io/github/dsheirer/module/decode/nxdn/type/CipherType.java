package io.github.dsheirer.module.decode.nxdn.type;

/**
 * Data protection types
 */
public enum CipherType
{
    UNENCRYPTED("UNENCRYPTED"),
    SCRAMBLE("SCRAMBLE"),
    DES("DES ENCRYPTION"),
    AES("AES ENCRYPTION");

    private String mLabel;

    /**
     * Constructs an instance
     *
     * @param label to display
     */
    CipherType(String label)
    {
        mLabel = label;
    }

    /**
     * Utility method to look up the cipher type from the transmitted value.
     *
     * @param value to look up
     * @return type
     */
    public static CipherType fromValue(int value)
    {
        return switch(value)
        {
            case 1 -> SCRAMBLE;
            case 2 -> DES;
            case 3 -> AES;
            default -> UNENCRYPTED;
        };
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
