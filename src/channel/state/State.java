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
package channel.state;

import java.util.EnumSet;

/**
 * Details the set of states for a channel and the allowable transition states
 */
public enum State
{ 
	IDLE( "IDLE" ) 
	{
        @Override
        public boolean canChangeTo( State state )
        {
	        return state != TEARDOWN && 
	        	   state != RESET;
        }
    }, 
	CALL( "CALL" ) 
	{
        @Override
        public boolean canChangeTo( State state )
        {
	        return state == CONTROL ||
	        	   state == DATA ||
	        	   state == ENCRYPTED ||
	        	   state == FADE ||
	        	   state == TEARDOWN;
        }
    }, 
	DATA( "DATA" ) 
	{
        @Override
        public boolean canChangeTo( State state )
        {
	        return state == CALL ||
	        	   state == CONTROL ||
		           state == ENCRYPTED ||
		           state == FADE ||
		           state == TEARDOWN;
        }
    },
    ENCRYPTED( "ENCRYPTED" )
    {
        @Override
        public boolean canChangeTo( State state )
        {
        	return state == FADE || state == TEARDOWN;
        }
    },
    CONTROL( "CONTROL" )
    {
		@Override
        public boolean canChangeTo( State state )
        {
            return state == IDLE || 
            	   state == FADE;
        }
    },
	FADE( "FADE" ) 
	{
        @Override
        public boolean canChangeTo( State state )
        {
	        return state != FADE && 
	        	   state != RESET; 
	    }
    },
	RESET( "RESET" ) 
	{
		@Override
		public boolean canChangeTo(State state)
		{
			return state == IDLE;
		}
	},
	
	TEARDOWN( "TEARDOWN" ) 
	{
		@Override
		public boolean canChangeTo(State state) 
		{
			return state == RESET;
		}
	};
	
	private String mDisplayValue;
	
	public static final EnumSet<State> CALL_STATES = EnumSet.of( CALL, CONTROL, DATA, ENCRYPTED );
	public static final EnumSet<State> IDLE_STATES = EnumSet.of( IDLE, FADE );
	
	private State( String displayValue )
	{
		mDisplayValue = displayValue;
	}
	
	public abstract boolean canChangeTo( State state );
	
	public String getDisplayValue()
	{
		return mDisplayValue;
	}
}