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

import java.util.EnumSet;
import java.util.Locale;

public enum OSType
{
    LINUX_X86_32,
    LINUX_X86_64,
    LINUX_ARM_32,
    LINUX_AARCH_64,

    OSX_AARCH_64,
    OSX_X86_64,

    WINDOWS_AARCH_64,
    WINDOWS_X86_32,
    WINDOWS_X86_64,

    UNKNOWN;

    public static EnumSet<OSType> LINUX_TYPES = EnumSet.of(LINUX_X86_32, LINUX_X86_64, LINUX_ARM_32, LINUX_AARCH_64);
    public static EnumSet<OSType> OSX_TYPES = EnumSet.of(OSX_AARCH_64, OSX_X86_64);
    public static EnumSet<OSType> WINDOWS_TYPES = EnumSet.of(WINDOWS_AARCH_64, WINDOWS_X86_32, WINDOWS_X86_64);

    /**
     * Indicates if this enumeration entry is a Windows type
     */
    public boolean isWindows()
    {
        return WINDOWS_TYPES.contains(this);
    }

    /**
     * Indicates if this enumeration entry is a Linux type
     */
    public boolean isLinux()
    {
        return LINUX_TYPES.contains(this);
    }

    public boolean isOsx()
    {
        return OSX_TYPES.contains(this);
    }

    /**
     * Detects the current host operating system and architecture
     */
    public static OSType getCurrentOSType()
    {
        String os = System.getProperty("os.name", "unknown").toLowerCase(Locale.ENGLISH);
        String arch = System.getProperty("os.arch");

        if(os.contains("win"))
        {
            if(arch.contains("amd64"))
            {
                return OSType.WINDOWS_X86_64;
            }
            else if(arch.contains("x86"))
            {
                return OSType.WINDOWS_X86_32;
            }
            else if(arch.contains("aarch64"))
            {
                return OSType.WINDOWS_AARCH_64;
            }
        }

        if(os.contains("mac") || os.contains("darwin") || os.contains("osx"))
        {
            if(arch.contains("amd64"))
            {
                return OSType.OSX_X86_64;
            }
            else if(arch.contains("aarch64"))
            {
                return OSType.OSX_AARCH_64;
            }
        }

        if(os.contains("nux") || os.contains("nix") || os.contains("aix"))
        {
            if(arch.contains("amd64"))
            {
                return OSType.LINUX_X86_64;
            }
            else if(arch.contains("x86"))
            {
                return OSType.LINUX_X86_32;
            }
            else if(arch.contains("aarch64"))
            {
                return OSType.LINUX_AARCH_64;
            }
            else if(arch.contains("arm"))
            {
                return OSType.LINUX_ARM_32;
            }
        }

        return OSType.UNKNOWN;
    }
}
