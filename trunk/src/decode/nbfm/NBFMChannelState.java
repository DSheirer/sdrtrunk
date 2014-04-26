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
package decode.nbfm;

import alias.AliasList;
import audio.SquelchListener;
import audio.SquelchListener.SquelchState;
import controller.channel.ProcessingChain;
import controller.state.AuxChannelState;
import controller.state.ChannelState;

/**
 * Conventional channel state - this class doesn't do anything at the moment,
 * it is a place holder for future expansion for the conventional decoder
 */
public class NBFMChannelState extends ChannelState
{
	public NBFMChannelState( ProcessingChain channel, AliasList aliasList )
	{
		super( channel, aliasList );
	}
	
	/**
	 * Make the ConventionalChannelState always unsquelched
	 */
	public void addListener( SquelchListener listener )
	{
		super.addListener( listener );
		
		super.setSquelchState( SquelchState.UNSQUELCH );
	}
	
	@Override
	public void setSquelchState( SquelchState state )
	{
		//do nothing ... we want the squelch state always unsquelched (for now)
	}
	@Override
    public String getActivitySummary()
    {
		StringBuilder sb = new StringBuilder();
		
		sb.append( "Activity Summary\n" );
		sb.append( "\tDecoder:\tNBFM\n\n" );
		
		for( AuxChannelState state: mAuxChannelStates )
		{
			sb.append( state.getActivitySummary() );
			sb.append( "\n\n" );
		}
		
		return sb.toString();
    }
}
