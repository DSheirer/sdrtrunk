/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens web pages in the browser and folders or files in the desktop's file explorer or viewer.
 *
 * Uses java.awt.Desktop where it supports the action, otherwise the desktop's own opener: xdg-open on Linux and
 * other Unix desktops, open on macOS.  Java's Desktop doesn't support browsing on some Linux desktops, for example
 * KDE Plasma, where xdg-open works.  Calls can block, so call them off the JavaFX thread.
 */
public final class DesktopLauncher
{
    private static final Logger mLog = LoggerFactory.getLogger(DesktopLauncher.class);
    private static final long OPENER_TIMEOUT_SECONDS = 10;

    private DesktopLauncher()
    {
    }

    /**
     * Opens the web page in the browser.
     * @param url to open
     * @throws IOException when neither Java's Desktop nor the desktop's opener could open it
     */
    public static void browse(String url) throws IOException
    {
        URI uri = URI.create(url);
        IOException desktopError = null;

        if(isSupported(Desktop.Action.BROWSE))
        {
            try
            {
                Desktop.getDesktop().browse(uri);
                return;
            }
            catch(IOException | RuntimeException e)
            {
                desktopError = e instanceof IOException ioe ? ioe : new IOException(e);
            }
        }

        runOpener(uri.toString(), desktopError);
    }

    /**
     * Opens the folder in the file explorer, or the file in its viewer.
     * @param file or folder to open
     * @throws IOException when neither Java's Desktop nor the desktop's opener could open it
     */
    public static void open(File file) throws IOException
    {
        IOException desktopError = null;

        if(isSupported(Desktop.Action.OPEN))
        {
            try
            {
                Desktop.getDesktop().open(file);
                return;
            }
            catch(IOException | RuntimeException e)
            {
                desktopError = e instanceof IOException ioe ? ioe : new IOException(e);
            }
        }

        runOpener(file.getAbsolutePath(), desktopError);
    }

    private static boolean isSupported(Desktop.Action action)
    {
        try
        {
            return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(action);
        }
        catch(RuntimeException e)
        {
            return false;
        }
    }

    /**
     * Desktop opener command for this operating system, or null when there isn't one to fall back to.
     */
    static String getOpener(String osName)
    {
        String os = osName != null ? osName.toLowerCase(Locale.ROOT) : "";

        if(os.startsWith("windows"))
        {
            //Java's Desktop supports both actions on Windows
            return null;
        }
        else if(os.startsWith("mac"))
        {
            return "open";
        }

        return "xdg-open";
    }

    /**
     * Runs the desktop's opener with the target.
     * @param target URL or path
     * @param desktopError from Java's Desktop, if it was tried, for the exception when the opener also fails
     */
    private static void runOpener(String target, IOException desktopError) throws IOException
    {
        String opener = getOpener(System.getProperty("os.name"));

        if(opener == null)
        {
            throw desktopError != null ? desktopError :
                new IOException("This desktop doesn't support opening [" + target + "]");
        }

        try
        {
            Process process = new ProcessBuilder(List.of(opener, target))
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();

            //The opener normally hands off to the browser or file explorer and exits.  One that doesn't exit in time
            //is assumed to be showing the target.
            if(process.waitFor(OPENER_TIMEOUT_SECONDS, TimeUnit.SECONDS) && process.exitValue() != 0)
            {
                throw new IOException(opener + " couldn't open [" + target + "] - exit code " + process.exitValue());
            }

            if(desktopError != null)
            {
                mLog.debug("Opened [" + target + "] with " + opener + " after Java's Desktop failed - " +
                    desktopError.getMessage());
            }
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while opening [" + target + "]", e);
        }
        catch(IOException e)
        {
            if(desktopError != null)
            {
                e.addSuppressed(desktopError);
            }

            throw e;
        }
    }
}
