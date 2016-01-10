package source.tuner.frequency;

import sample.Listener;

public class FrequencyChangeProcessorWrapper 
			implements IFrequencyChangeListener, Listener<FrequencyChangeEvent>
{
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
			mProcessor.frequencyChanged( event );
		}
	}
}
