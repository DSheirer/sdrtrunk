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
package decode.lj1200;

import message.Message;
import controller.state.AuxChannelState;
import controller.state.ChannelState;
import controller.state.ChannelState.State;

public class LJ1200ChannelState extends AuxChannelState
{
	private LJ1200ActivitySummary mActivitySummary;

	public LJ1200ChannelState( ChannelState parentState )
	{
		super( parentState );
		
		mActivitySummary = 
				new LJ1200ActivitySummary( parentState.getAliasList() );
	}
	
	@Override
	public void reset()
	{
	}
	
	@Override
    public void fade()
    {
    }
	
	@Override
    public void receive( Message message )
    {
		if( message instanceof LJ1200Message )
		{
			mActivitySummary.receive( message );

			/* Set the state to CALL on the parent channel state, so that the
			 * parent state timer can track turning off any updates we apply */
			mParentChannelState.setState( State.CALL );
		}
    }
	
	@Override
    public String getActivitySummary()
    {
		return mActivitySummary.getSummary();
    }
}
