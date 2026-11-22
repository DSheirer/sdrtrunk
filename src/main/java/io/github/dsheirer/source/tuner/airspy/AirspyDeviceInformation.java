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

package io.github.dsheirer.source.tuner.airspy;

import io.github.dsheirer.airspy.PartIdSerialNumber;

/**
 * Airspy device information, read from the device by libairspy.
 */
public class AirspyDeviceInformation
{
	private final String mBoardName;
	private final String mFirmwareVersion;
	private final PartIdSerialNumber mPartIdSerialNumber;

	/**
	 * Constructs an instance
	 * @param boardName from the device
	 * @param firmwareVersion string from the device
	 * @param partIdSerialNumber from the device
	 */
	public AirspyDeviceInformation(String boardName, String firmwareVersion, PartIdSerialNumber partIdSerialNumber)
	{
		mBoardName = boardName;
		mFirmwareVersion = firmwareVersion;
		mPartIdSerialNumber = partIdSerialNumber;
	}

	public String getBoardName()
	{
		return mBoardName;
	}

	/**
	 * Firmware version, without the "AirSpy NOS " prefix and truncated at the first line break.
	 */
	public String getVersion()
	{
		if(mFirmwareVersion == null)
		{
			return "Unknown";
		}

		String version = mFirmwareVersion;
		int lineBreak = version.indexOf('\n');

		if(lineBreak >= 0)
		{
			version = version.substring(0, lineBreak);
		}

		return version.replace("AirSpy NOS ", "").trim();
	}

	/**
	 * Serial number, formatted as the identifier that tuner configurations are saved under.
	 */
	public String getSerialNumber()
	{
		long serial = mPartIdSerialNumber.serialNumber();
		return String.format("%08X-%08X", serial >>> 32, serial & 0xFFFFFFFFL);
	}

	public String getPartNumber()
	{
		return String.format("%08X%08X", mPartIdSerialNumber.partId1(), mPartIdSerialNumber.partId2());
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("Airspy Device Information\n");
		sb.append("Board: ");
		sb.append(getBoardName());
		sb.append("\nPart Number: ");
		sb.append(getPartNumber());
		sb.append("\nSerial Number: ");
		sb.append(getSerialNumber());
		sb.append("\nFirmware: ");
		sb.append(getVersion());

		return sb.toString();
	}
}
