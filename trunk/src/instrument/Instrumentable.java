/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package instrument;

import instrument.tap.Tap;

import java.util.List;

/**
 * Interface for classes that want to expose internal data, streams or events 
 * to external listeners.
 */
public interface Instrumentable
{
	/**
	 * Returns the list of available tap points.  Use the addTap() and
	 * removeTap() to register a tap on the instrumentable class and then add
	 * your TapListener(s) to the registered tap.
	 */
	public List<Tap> getTaps();

	/**
	 * Registers a tap on the instrumentable object.
	 * 
	 * @param tap - one of the tap(s) obtained from getTaps() from the 
	 * instrumentable source
	 */
	public void addTap( Tap tap );

	/**
	 * Unregisters a tap on the instrumentable object, if the tap is currently
	 * registered.
	 * 
	 * @param tap - one of the tap(s) obtained from getTaps() from the 
	 * instrumentable source
	 */
	public void removeTap( Tap tap );
}
