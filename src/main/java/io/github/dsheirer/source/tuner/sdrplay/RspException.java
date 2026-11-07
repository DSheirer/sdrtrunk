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

package io.github.dsheirer.source.tuner.sdrplay;

/**
 * Error while controlling an SDRplay RSP device.  Wraps errors reported by the jsdrplay library and the SDRplay API,
 * and errors for a device that is not started.
 */
public class RspException extends Exception
{
    /**
     * Constructs an instance
     * @param message describing the error
     */
    public RspException(String message)
    {
        super(message);
    }

    /**
     * Constructs an instance
     * @param message describing the error
     * @param cause of the error
     */
    public RspException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
