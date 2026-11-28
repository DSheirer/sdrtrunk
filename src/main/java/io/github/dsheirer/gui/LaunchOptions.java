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

package io.github.dsheirer.gui;

import io.github.dsheirer.settings.web.WebServerSettings;

/**
 * Command line options for the sdrtrunk launcher.
 *
 * @param help true to print the usage and exit
 * @param headless true to run without the user interface (the same as -Djava.awt.headless=true)
 * @param webEnabled turns the web server on or off for this run, or null for the stored setting
 * @param webBindAddress listen address for this run, or null for the stored setting
 * @param webPort listen port for this run, or null for the stored setting
 */
public record LaunchOptions(boolean help, boolean headless, Boolean webEnabled, String webBindAddress, Integer webPort)
{
    public static final String USAGE = """
        Usage: sdr-trunk [options]
          --headless          Run without the user interface
          --web               Turn on the web server for this run
          --no-web            Turn off the web server for this run
          --web-bind=ADDRESS  Web server listen address for this run, e.g. 127.0.0.1 or 0.0.0.0
          --web-port=PORT     Web server port for this run
          --help              Show this help
        The --web options don't change the stored settings.""";

    /**
     * Parses the command line arguments.
     * @throws IllegalArgumentException for an unknown option or a bad value
     */
    public static LaunchOptions parse(String[] args)
    {
        boolean help = false;
        boolean headless = false;
        Boolean webEnabled = null;
        String bind = null;
        Integer port = null;

        for(String arg: args == null ? new String[0] : args)
        {
            if(arg.equals("--help") || arg.equals("-h"))
            {
                help = true;
            }
            else if(arg.equals("--headless"))
            {
                headless = true;
            }
            else if(arg.equals("--web"))
            {
                webEnabled = true;
            }
            else if(arg.equals("--no-web"))
            {
                webEnabled = false;
            }
            else if(arg.startsWith("--web-bind="))
            {
                bind = arg.substring("--web-bind=".length()).trim();

                if(bind.isEmpty())
                {
                    throw new IllegalArgumentException("--web-bind needs an address");
                }
            }
            else if(arg.startsWith("--web-port="))
            {
                try
                {
                    port = Integer.parseInt(arg.substring("--web-port=".length()).trim());
                }
                catch(NumberFormatException nfe)
                {
                    port = -1;
                }

                if(port < WebServerSettings.MIN_PORT || port > WebServerSettings.MAX_PORT)
                {
                    throw new IllegalArgumentException("--web-port must be from " + WebServerSettings.MIN_PORT +
                        " to " + WebServerSettings.MAX_PORT);
                }
            }
            else
            {
                throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }

        return new LaunchOptions(help, headless, webEnabled, bind, port);
    }
}
