/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.edac;

public enum CRC
{
    PASSED("*", "PASS", true),
    PASSED_INV("#", "PASS INVERTED", true),
    FAILED_CRC("f", "FAIL CRC", false),
    FAILED_PARITY("p", "FAIL PARITY", false),
    CORRECTED("C", "CORRECTED", true),
    UNKNOWN("-", "UNKNOWN", false);

    private String mAbbreviation;
    private String mDisplayText;
    private boolean mPass;

    CRC(String abbreviation, String displayText, boolean pass)
    {
        mAbbreviation = abbreviation;
        mDisplayText = displayText;
        mPass = pass;
    }

    public String getAbbreviation()
    {
        return mAbbreviation;
    }

    public String getDisplayText()
    {
        return mDisplayText;
    }

    public boolean passes()
    {
        return mPass;
    }

    public static String format(CRC[] checks)
    {
        if(checks == null)
        {
            return CRC.UNKNOWN.getAbbreviation();
        }

        StringBuilder sb = new StringBuilder();

        for (CRC check : checks) {
            if (check != null) {
                sb.append(check.getAbbreviation());
            } else {
                sb.append(CRC.UNKNOWN.getAbbreviation());
            }
        }

        return sb.toString();
    }
}
