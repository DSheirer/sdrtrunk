/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.util;

public class StringUtils
{
    private static final String[] ILLEGAL_FILENAME_CHARACTERS = {"#", "%", "&", "{", "}", "\\", "<", ">",
            "*", "?", "/", " ", "$", "!", "'", "\"", ":", "@", "+", "`", "|", "=", ","};

    /**
     * Compares string a to b and returns true if a is non-null and non-empty, b is non-null and non-empty, and
     * a is equal to b.
     * @param a string to compare
     * @param b string to compare
     */
    public static boolean isEqual(String a, String b)
    {
        return !isEmpty(a) && !isEmpty(b) && a.equals(b);
    }

    /**
     * Checks the string for null or empty condition
     */
    public static boolean isEmpty(String a)
    {
        return a == null || a.isEmpty();
    }

    /**
     * Replaces any illegal filename characters in the proposed filename
     */
    public static String replaceIllegalCharacters(String filename)
    {
        if(isEmpty(filename))
        {
            return filename;
        }

        for (String illegalCharacter : ILLEGAL_FILENAME_CHARACTERS)
        {
            filename = filename.replace(illegalCharacter, "-");
        }

        return filename;
    }
}
