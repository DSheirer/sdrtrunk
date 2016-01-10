package source.tuner.frequency;

import sample.Listener;

public interface IFrequencyChangeProvider
{
	public void setFrequencyChangeListener( Listener<FrequencyChangeEvent> listener );
	public void removeFrequencyChangeListener();
}
