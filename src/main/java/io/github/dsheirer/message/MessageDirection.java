/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.message;

import java.util.EnumSet;

public enum MessageDirection
{
	ISW( "Repeater Input (ISW)" ), //Inbound Status Word
	OSW( "Repeater Output (OSW)" ); //Outbound Status Word
	
	private String mLabel;
	
	private MessageDirection( String label )
	{
		mLabel = label;
	}

	public static EnumSet<MessageDirection> ORDERED_VALUES = EnumSet.of(OSW, ISW);
	
	public String toString()
	{
		return mLabel;
	}
}
