/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

/**
 * SDRplay checked exception.
 */
public class SDRPlayException extends Exception
{
    private Status mStatus = Status.UNKNOWN;

    /**
     * Creates an exception
     * @param message for the exception
     */
    public SDRPlayException(String message)
    {
        super(message);
    }

    /**
     * Creates an operation exception
     * @param message for the exception
     * @param status of the operation
     */
    public SDRPlayException(String message, Status status)
    {
        super(message + " Status:" + status);
        mStatus = status;
    }

    /**
     * Creates an exception
     * @param message for the exception
     * @param throwable nested exception
     */
    public SDRPlayException(String message, Throwable throwable)
    {
        super(message, throwable);
    }

    public Status getStatus()
    {
        return mStatus;
    }
}
