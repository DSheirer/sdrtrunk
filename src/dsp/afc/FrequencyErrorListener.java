package dsp.afc;

import source.tuner.FrequencyChangeListener;

public interface FrequencyErrorListener
{
	public void setError( int error );
	
	public void addListener( FrequencyChangeListener listener );
}
