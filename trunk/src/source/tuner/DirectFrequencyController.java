package source.tuner;


public interface DirectFrequencyController
{
	public void setListener( FrequencyChangeListener listener );
	
	public long getFrequencyCorrection();
}
