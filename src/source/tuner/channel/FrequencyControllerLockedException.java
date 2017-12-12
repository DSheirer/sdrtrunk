/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package source.tuner.channel;

import source.SourceException;

public class FrequencyControllerLockedException extends SourceException
{
    private static final long serialVersionUID = 1L;

    /**
     * Indicates an attempt to adjust frequency or sample rate on a locked frequency controller.
     */
    public FrequencyControllerLockedException()
	{
    	super( "Frequency controller is locked -- frequency and sample rate cannot be changed until the controller" +
            " is unlocked by the channel manager" );
	}
}
