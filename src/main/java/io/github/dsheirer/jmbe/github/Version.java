/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.jmbe.github;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version representation and parsing class
 */
public class Version implements Comparable<Version>
{
    public final static Pattern VERSION_PATTERN = Pattern.compile("v?(\\d{1,5}).(\\d{1,5}).(\\d{1,5})(\\w*)");

    private Integer mMajor;
    private Integer mMinor;
    private Integer mRelease;
    private Character mPatch;

    /**
     * Constructs an instance
     * @param major version
     * @param minor version
     * @param release version
     * @param patch version (optional)
     */
    public Version(int major, int minor, int release, Character patch)
    {
        mMajor = major;
        mMinor = minor;
        mRelease = release;
        mPatch = patch;
    }

    /**
     * Parses the argument into a version instance.
     * @param version string (e.g. v1.0.6a)
     * @return
     */
    public static Version fromString(String version)
    {
        if(version != null)
        {
            Matcher m = VERSION_PATTERN.matcher(version.replace("\"", ""));

            if(m.matches())
            {
                int major = 0;
                int minor = 0;
                int release = 0;
                Character patch = null;

                try
                {
                    major = Integer.parseInt(m.group(1));
                }
                catch(Exception e)
                {
                    //Do nothing, we couldn't parse the value
                }

                try
                {
                    minor = Integer.parseInt(m.group(2));
                }
                catch(Exception e)
                {
                    //Do nothing, we couldn't parse the value
                }

                try
                {
                    release = Integer.parseInt(m.group(3));
                }
                catch(Exception e)
                {
                    //Do nothing, we couldn't parse the value
                }

                String rawPatch = m.group(4);

                if(rawPatch != null && rawPatch.length() >= 1)
                {
                    patch = rawPatch.charAt(0);
                }

                return new Version(major, minor, release, patch);
            }
        }

        return null;
    }

    public Integer getMajor()
    {
        return mMajor;
    }

    public boolean hasMajor()
    {
        return mMajor != null;
    }

    public Integer getMinor()
    {
        return mMinor;
    }

    public boolean hasMinor()
    {
        return mMinor != null;
    }

    public Integer getRelease()
    {
        return mRelease;
    }

    public boolean hasRelease()
    {
        return mRelease != null;
    }

    public char getPatch()
    {
        return mPatch;
    }

    public boolean hasPatch()
    {
        return mPatch != null;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(hasMajor() ? getMajor() : "x");
        sb.append(".").append(hasMinor() ? getMinor() : "x");
        sb.append(".").append(hasRelease() ? getRelease() : "x");
        if(hasPatch())
        {
            sb.append(mPatch);
        }

        return sb.toString();
    }

    @Override
    public int compareTo(Version other)
    {
        if(hasMajor() && other.hasMajor())
        {
            if(getMajor() != other.getMajor())
            {
                return Integer.compare(getMajor(), other.getMajor());
            }
            else
            {
                if(hasMinor() && other.hasMinor())
                {
                    if(getMinor() != other.getMinor())
                    {
                        return Integer.compare(getMinor(), other.getMinor());
                    }
                    else
                    {
                        if(hasRelease() && other.hasRelease())
                        {
                            if(getRelease() != other.getRelease())
                            {
                                return Integer.compare(getRelease(), other.getRelease());
                            }
                            else
                            {
                                if(hasPatch() && other.hasPatch())
                                {
                                    if(getPatch() != other.getPatch())
                                    {
                                        return Character.compare(getPatch(), other.getPatch());
                                    }
                                    else
                                    {
                                        return 0;
                                    }
                                }
                                else
                                {
                                    return hasPatch() ? 1 : -1;
                                }
                            }
                        }
                        else
                        {
                            return hasRelease() ? -1 : 1;
                        }
                    }
                }
                else
                {
                    return hasMinor() ? -1 : 1;
                }
            }
        }
        else
        {
            return hasMajor() ? -1 : 1;
        }
    }
}
