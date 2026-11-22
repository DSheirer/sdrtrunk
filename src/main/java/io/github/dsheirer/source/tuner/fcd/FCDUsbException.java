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

package io.github.dsheirer.source.tuner.fcd;

import io.github.dsheirer.libusb.LibUsb;

/**
 * USB transfer error while communicating with a FUNcube Dongle.  Unchecked, like the usb4java exception it replaces,
 * since the FCD command methods are called from many places that handle any exception.
 */
public class FCDUsbException extends RuntimeException
{
    private final int mErrorCode;

    /**
     * Constructs an instance
     * @param message describing the failed operation
     * @param errorCode libusb error code, or 0 when the transfer succeeded but returned unexpected data
     */
    public FCDUsbException(String message, int errorCode)
    {
        super(errorCode < 0 ? message + " - " + LibUsb.errorName(errorCode) : message);
        mErrorCode = errorCode;
    }

    /**
     * libusb error code, such as -7 for LIBUSB_ERROR_TIMEOUT, or 0
     */
    public int getErrorCode()
    {
        return mErrorCode;
    }
}
