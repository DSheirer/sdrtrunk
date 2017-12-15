package ua.in.smartjava.source.tuner.frequency;

import ua.in.smartjava.source.SourceException;

public interface IFrequencyChangeProcessor
{
	public void frequencyChanged( FrequencyChangeEvent event ) throws SourceException;
}
