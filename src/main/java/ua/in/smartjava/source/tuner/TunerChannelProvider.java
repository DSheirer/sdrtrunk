package ua.in.smartjava.source.tuner;

import java.util.concurrent.RejectedExecutionException;

import ua.in.smartjava.source.SourceException;

public interface TunerChannelProvider
{
	/**
	 * Returns a tuner frequency ua.in.smartjava.channel ua.in.smartjava.source, tuned to the correct frequency
	 * 
	 * @param frequency - desired frequency
	 * 
	 * @return - ua.in.smartjava.source for 48k ua.in.smartjava.sample rate
	 */
	public abstract TunerChannelSource getChannel( TunerChannel channel ) 
			throws RejectedExecutionException, SourceException;

	/**
	 * Releases the tuned ua.in.smartjava.channel resources
	 * 
	 * @param channel - previously obtained tuner ua.in.smartjava.channel
	 */
	public abstract void releaseChannel( TunerChannelSource source );
	
	
}
