package io.github.dsheirer.source.tuner.frequency;

public class FrequencyLimitException extends Exception
{
    private static final long serialVersionUID = 1L;

    private long mFrequencyLimit;
    private long mRequestedFrequency;

    /**
     * Indicates that the requested frequency is outside of the tunable bounds
     * of the tuner.  
     * @param message
     * @param frequency
     */
    public FrequencyLimitException( long requestedFrequency,
    								long frequencyLimit )
	{
    	super( "Requested frequency [" + requestedFrequency + 
    			"] exceeds the limit [" + frequencyLimit + "].  Tuned "
    					+ "frequency was set to the limit value" );
    	
    	mRequestedFrequency = requestedFrequency;
    	mFrequencyLimit = frequencyLimit;
	}
    
    public long getRequestedFrequency()
    {
    	return mRequestedFrequency;
    }
    
    public long getFrequencyLimit()
    {
    	return mFrequencyLimit;
    }
}
