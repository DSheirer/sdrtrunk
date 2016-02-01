package module.decode.state;

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
	
	public static final EnumSet<State> CALL_STATES = 
			EnumSet.of( CALL, CONTROL, DATA, ENCRYPTED );
	
	public static final EnumSet<State> IDLE_STATES = 
			EnumSet.of( IDLE, FADE );
	
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
