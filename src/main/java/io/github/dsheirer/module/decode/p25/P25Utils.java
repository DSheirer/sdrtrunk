package io.github.dsheirer.module.decode.p25;

import org.apache.commons.lang3.StringUtils;

public class P25Utils
{
    /**
     * Formats a NAC code to three hexadecimal characters using zeros to prepad the value to three places.
     * @param nac to format
     * @return formatted nac
     */
    public static String formatNAC(int nac)
    {
        return formatHex(nac, 3);
    }

    /**
     * Formats the value as hexadecimal with the minimum number of character places prepadding with zeros as needed.
     * @param value to format
     * @param places minimum for the formatted value
     * @return formatted value
     */
    public static String formatHex(int value, int places)
    {
        return StringUtils.leftPad(Integer.toHexString(value).toUpperCase(), places, '0');
    }

    /**
     * Adds spaces to the string builder until it is the specified length
     * @param sb to pad with spaces
     * @param length of the stringbuilder when complete
     */
    public static void pad(StringBuilder sb, int length)
    {
        pad(sb, length, " ");
    }

    /**
     * Adds pad characters to the string builder until it is the specified length
     * @param sb to pad with spaces
     * @param length of the stringbuilder when complete
     * @param padCharacter to use for padding
     */
    public static void pad(StringBuilder sb, int length, String padCharacter)
    {
        while(sb.length() < length)
        {
            sb.append(padCharacter);
        }
    }
}
