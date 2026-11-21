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

package io.github.dsheirer.web;

/**
 * State of the web server, for the settings dialog and the log.
 * @param state of the server
 * @param url that clients use, or null when it isn't running
 * @param error why it isn't running, or null
 */
public record WebServerStatus(State state, String url, String error)
{
    public enum State
    {
        /**
         * Disabled in the settings
         */
        STOPPED,
        /**
         * Listening for requests
         */
        RUNNING,
        /**
         * Enabled but couldn't start, for example because the port is in use
         */
        FAILED
    }

    public static WebServerStatus stopped()
    {
        return new WebServerStatus(State.STOPPED, null, null);
    }
}
