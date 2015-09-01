/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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
package module.decode.tait;

import java.util.Collections;
import java.util.List;

import message.Message;
import filter.Filter;
import filter.FilterElement;

public class Tait1200MessageFilter extends Filter<Message>
{
	
	public Tait1200MessageFilter()
	{
		super( "Tait-1200 Message Filter" );
	}
	
	@Override
    public boolean passes( Message message )
    {
		return mEnabled && canProcess( message );
    }

	@Override
    public boolean canProcess( Message message )
    {
	    return message instanceof Tait1200GPSMessage;
    }

	@Override
    public List<FilterElement<?>> getFilterElements()
    {
	    return Collections.EMPTY_LIST;
    }
}
