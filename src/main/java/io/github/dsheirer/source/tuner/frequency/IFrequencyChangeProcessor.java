package io.github.dsheirer.source.tuner.frequency;

import io.github.dsheirer.source.SourceException;

public interface IFrequencyChangeProcessor
{
	public void frequencyChanged( FrequencyChangeEvent event ) throws SourceException;
}
