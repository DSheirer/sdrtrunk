package module.decode.state;

public enum State
{ 
	IDLE( "IDLE" ) 
	{
        @Override
        public boolean canChangeTo( State state )
        {
	        return true;
        }
    }, 
	CALL( "CALL" ) 
	{
        @Override
        public boolean canChangeTo( State state )
        {
	        return state == CALL ||
	        	   state == CONTROL ||
	        	   state == DATA ||
	        	   state == ENCRYPTED ||
	        	   state == FADE;
        }
    }, 
	DATA( "DATA" ) 
	{
        @Override
        public boolean canChangeTo( State state )
        {
	        return state == CALL ||
	        	   state == CONTROL ||
		           state == DATA ||
		           state == State.ENCRYPTED ||
		           state == FADE;
        }
    },
    ENCRYPTED( "ENCRYPTED" )
    {
        @Override
        public boolean canChangeTo( State state )
        {
	        return state == FADE;
        }
    },
    CONTROL( "CONTROL" )
    {
		@Override
        public boolean canChangeTo( State state )
        {
            return true;
        }
    },
	FADE( "FADE" ) 
	{
        @Override
        public boolean canChangeTo( State state )
        {
	        return state != FADE; //All states except fade allowed
        }
    },
	END( "END" ) 
	{
        @Override
        public boolean canChangeTo( State state )
        {
	        return state != FADE;  //Fade is only disallowed state
        }
    };
	
	private String mDisplayValue;
	
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
