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
package io.github.dsheirer.module.log;


public enum EventLogType
{
    /**
     * Legacy logging type - do not use - superceded by bitstream recorder
     */
    BINARY_MESSAGE( "Binary Messages", "_binary_messages" ),

    DECODED_MESSAGE( "Decoded Messages", "_decoded_messages" ),
    TRAFFIC_DECODED_MESSAGE( "Traffic Channel Decoded Messages", "_decoded_messages" ),
    CALL_EVENT( "Call Events", "_call_events" ),
    TRAFFIC_CALL_EVENT( "Traffic Channel Call Events", "_call_events" );

    private String mDisplayString;
    private String mFileSuffix;
    
    EventLogType( String displayString, String fileSuffix )
    {
        mDisplayString = displayString;
        mFileSuffix = fileSuffix;
    }
    
    public String getDisplayString()
    {
        return mDisplayString;
    }
    
    public String getFileSuffix()
    {
    	return mFileSuffix;
    }

    @Override
    public String toString()
    {
    	return mDisplayString;
    }
}
