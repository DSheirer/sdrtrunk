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

package io.github.dsheirer.module.decode.p25.reference;

import java.util.ArrayList;
import java.util.List;

/**
 * APCO-25 Services enumeration
 */
public enum Service
{
	EXTENDED_SERVICES(0x800000),
	EXTENDED_SERVICES_EXTENSION(0x400000),
	
	/* NORMAL SERVICES */
	NETWORK_ACTIVE(0x200000),
	RESERVED_4(0x100000),
	GROUP_VOICE(0x080000),
	INDIVIDUAL_VOICE(0x040000),
	PSTN_TO_UNIT_VOICE(0x020000),
	UNIT_TO_PSTN_VOICE(0x010000),
	RESERVED_9(0x008000),
	GROUP_DATA(0x004000),
	INDIVIDUAL_DATA(0x002000),
	RESERVED_12(0x001000),
	UNIT_REGISTRATION(0x000800),
	GROUP_AFFILIATION(0x000400),
	GROUP_AFFILIATION_QUERY(0x000200),
	AUTHENTICATION(0x000100),
	ENCRYPTION_SETTINGS(0x000080),
	USER_STATUS(0x000040),
	USER_MESSAGE(0x000020),
	UNIT_STATUS(0x000010),
	USER_STATUS_QUERY(0x000008),
	UNIT_STATUS_QUERY(0x000004),
	CALL_ALERT(0x000002),
	EMERGENCY_ALARM(0x000001),
	UNKNOWN(0x0);

	private int mCode;

	/**
	 * Constructs an instance
	 * @param code that is a bitmap of service entry values
	 */
	Service(int code)
	{
		mCode = code;
	}

	/**
	 * Bitmap code
	 */
	public int getCode()
	{
		return mCode;
	}

	/**
	 * Indicates if the flag for the specified service is set in the bitmap.
	 * @param service to check
	 * @param serviceBitmap containing set flag values.
	 * @return true if the service is set in the bitmap.
	 */
	public static boolean isSupported(Service service, int serviceBitmap)
	{
		return ( service.getCode() & serviceBitmap ) == service.getCode();
	}

	/**
	 * List of service indicated by the bitmap.
	 */
	public static List<Service> getServices(int serviceBitmap)
	{
		List<Service> services = new ArrayList<>();

		for(Service service : values())
		{
			if(isSupported(service, serviceBitmap))
			{
				if(service != Service.UNKNOWN)
				{
					services.add(service);
				}
			}
		}
	
		return services;
	}
}
