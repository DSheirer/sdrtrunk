/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.sdrplay.api;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper utility to load the SDRPlay API native library into the VM.
 *
 * Note: the jextract auto-generated code attempts to load the native library by simple library name without any path
 * information.  On Windows this can fail because the API library is not installed in the normal Windows library location
 * and for some reason the system library VM option with the specific location that is passed in as an argument on
 * the command line doesn't help the system loader to resolve the library path.  So, we preemptively load the library
 * here.
 */
public class SDRPlayLibraryHelper
{
    private static final Logger mLog = LoggerFactory.getLogger(SDRPlayLibraryHelper.class);
    private static final String SDRPLAY_API_LIBRARY_NAME = "sdrplay_api";
    private static final String SDRPLAY_API_PATH_LINUX = "/usr/local/lib/libsdrplay_api.so";
    private static final String SDRPLAY_API_PATH_MAC_OS = "/usr/local/lib/libsdrplay_api.dylib";
    private static final String SDRPLAY_API_PATH_MAC_OS_ALTERNATE = "/usr/local/lib/libsdrplay_api.so";
    private static final String SDRPLAY_API_PATH_WINDOWS = System.getenv("ProgramFiles") +
            "\\SDRplay\\API\\x64\\" + SDRPLAY_API_LIBRARY_NAME;

    public static final boolean LOADED;
    public static final boolean LOADED_FROM_PATH;
    public static final Path LIBRARY_PATH = Path.of(getSDRplayLibraryPath());

    static
    {
        boolean loaded = false;
        boolean loadedFromPath = false;

        try
        {
            System.loadLibrary(SDRPLAY_API_LIBRARY_NAME);
            mLog.info("SDRPLay API library loaded by name [" + SDRPLAY_API_LIBRARY_NAME + "]");
            loaded = true;
        }
        catch(Throwable t)
        {
            String libraryPath = getSDRplayLibraryPath();
            Path path = Path.of(libraryPath);
            boolean exists = Files.exists(path);

            if(exists)
            {
                try
                {
                    System.load(libraryPath);
                    mLog.info("SDRPLay API library loaded by path [" + libraryPath + "]");
                    loaded = true;
                    loadedFromPath = true;
                }
                catch(Throwable t2)
                {
                    mLog.info("SDRPlay API native library not found at " + libraryPath);
                }
            }
            else
            {
                mLog.info("SDRPlay API native library not found at: " + libraryPath);
            }
        }

        LOADED = loaded;
        LOADED_FROM_PATH = loadedFromPath;
    }

    /**
     * Identifies the java library path for the sdrplay api library at runtime.
     */
    public static String getSDRplayLibraryPath()
    {
        if(SystemUtils.IS_OS_WINDOWS)
        {
            return SDRPLAY_API_PATH_WINDOWS;
        }
        else if(SystemUtils.IS_OS_LINUX)
        {
            return SDRPLAY_API_PATH_LINUX;
        }
        else if(SystemUtils.IS_OS_MAC_OSX)
        {
            //API versions 3.14 and earlier used a (.so) extension and 3.15 and later use the (.dylib) extension
            if(Files.exists(Path.of(SDRPLAY_API_PATH_MAC_OS)))
            {
                return SDRPLAY_API_PATH_MAC_OS;
            }
            else
            {
                return SDRPLAY_API_PATH_MAC_OS_ALTERNATE;
            }
        }

        mLog.error("Unrecognized operating system.  Cannot identify sdrplay api library path");
        return "";
    }
}
