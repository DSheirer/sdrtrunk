/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
 *
 ******************************************************************************/
package alias.id;

import org.apache.commons.lang3.Validate;

import java.util.regex.Pattern;

public class WildcardID implements Comparable<WildcardID>
{
    public static final String WILDCARD = "*";
    public static final String REGEX_WILDCARD = ".";

    private String mValue;
    private Pattern mPattern;
    private int mWeight;

    /**
     * Wildcard identifier for matching to string identifiers containing single-character (*) wildcard values.
     *
     * Supports ordering from most-specific to least-specific using the build-in weighting calculation where identifier
     * patterns containing fewer wildcards closer to the least significant digit will be weighted more heavily than
     * identifier patterns containing more wildcards, or wildcard characters in the most significant digits.
     *
     * @param value is a string value containing one or more asterisk (*) single character wildcard with a maximum
     * length of 10 characters.
     *
     * @throws AssertionError if the value is null, longer than 10 characters, or doesn't contain at least 1 asterisk
     * @throws IllegalArgumentException if the value cannot be compiled as a regular expression after converting the
     * asterisk characters to the regex single-character wildcard value (.)
     */
    public WildcardID(String value)
    {
        Validate.isTrue(value != null && value.contains(WILDCARD) && value.length() < 10);

        mValue = value;

        try
        {
            mPattern = Pattern.compile(value.replace(WILDCARD, REGEX_WILDCARD));
        }
        catch(Exception e)
        {
            throw new IllegalArgumentException("Invalid regex pattern for alias ID value [" + value + "]", e);
        }

        mWeight = calculateWeight();
    }

    public String value()
    {
        return mValue;
    }

    /**
     * Indicates if the id matches the internal regex pattern for this wildcard alias ID
     * @param id
     * @return
     */
    public boolean matches(String id)
    {
        return id != null && mPattern.matcher(id).matches();
    }

    public int weight()
    {
        return mWeight;
    }

    /**
     * Calculates a weighting value for wildcard character quantity and significant digit location
     */
    private int calculateWeight()
    {
        int weight = 0;
        int characterCount = -1;

        for( int x = 0; x < mValue.length(); x++)
        {
            if(mValue.substring(x, x + 1).equals(WILDCARD))
            {
                weight += (int)(Math.pow(2, mValue.length() - x - 1));         //Position weight
                characterCount++;
            }
        }

        weight += (int)(Math.pow(2, characterCount)) * 1000; //Character count weight

        return weight;
    }

    @Override
    public int compareTo(WildcardID otherWildcardID)
    {
        return Integer.compare(this.weight(), otherWildcardID.weight());
    }
}
