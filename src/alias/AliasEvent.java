/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
package alias;

public class AliasEvent
{
	private Alias mAlias;
	private Event mEvent;

	/**
	 * AliasEvent - event describing any changes to an alias
	 * @param alias - alias that changed
	 * @param event - change event
	 */
	public AliasEvent( Alias alias, Event event )
	{
		mAlias = alias;
		mEvent = event;
	}
	
	public Alias getAlias()
	{
		return mAlias;
	}
	
	public Event getEvent()
	{
		return mEvent;
	}
	
	/**
	 * Channel events to describe the specific event
	 */
	public enum Event
	{
		ADD,
		CHANGE,
		DELETE;
	}
}
