package io.github.dsheirer.source.tuner.frequency;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrequencyChangeProcessorWrapper 
			implements IFrequencyChangeListener, Listener<FrequencyChangeEvent>
{
	private final static Logger mLog = LoggerFactory.getLogger( FrequencyChangeProcessorWrapper.class );

	private IFrequencyChangeProcessor mProcessor;

	/**
	 * Provides IFrequencyChangeListener wrapper for an IFrequencyChangeProcessor
	 */
	public FrequencyChangeProcessorWrapper( IFrequencyChangeProcessor handler )
	{
		mProcessor = handler;
	}
	
	@Override
	public Listener<FrequencyChangeEvent> getFrequencyChangeListener()
	{
		return this;
	}

	@Override
	public void receive( FrequencyChangeEvent event )
	{
		if( mProcessor != null )
		{
			try
			{
				mProcessor.frequencyChanged( event );
			}
			catch( SourceException se )
			{
				mLog.error("Error sending frequency change event to embedded processor", se);
			}
		}
	}
}
