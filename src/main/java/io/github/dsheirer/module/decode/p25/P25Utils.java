package io.github.dsheirer.module.decode.p25;

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
        StringBuilder sb = new StringBuilder();
        String formatted = Integer.toHexString(value).toUpperCase();

        if(formatted.length() < places)
        {
            for(int x = 0; x < (places - formatted.length()); x++)
            {
                sb.append("0");
            }
        }

        sb.append(formatted);

        return sb.toString();
    }
}
