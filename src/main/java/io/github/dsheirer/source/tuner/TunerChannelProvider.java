package io.github.dsheirer.source.tuner;

import io.github.dsheirer.source.SourceException;

import java.util.concurrent.RejectedExecutionException;

public interface TunerChannelProvider
{
	/**
	 * Returns a tuner frequency channel source, tuned to the correct frequency
	 * 
	 * @param frequency - desired frequency
	 * 
	 * @return - source for 48k sample rate
	 */
	public abstract TunerChannelSource getChannel( TunerChannel channel ) 
			throws RejectedExecutionException, SourceException;

	/**
	 * Releases the tuned channel resources
	 * 
	 * @param channel - previously obtained tuner channel
	 */
	public abstract void releaseChannel( TunerChannelSource source );
	
	
}
