package source.tuner.frequency;

import source.SourceException;

public interface IFrequencyChangeProcessor
{
	public void frequencyChanged( FrequencyChangeEvent event ) throws SourceException;
}
