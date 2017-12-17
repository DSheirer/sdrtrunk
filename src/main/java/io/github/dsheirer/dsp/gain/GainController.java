package io.github.dsheirer.dsp.gain;

public interface GainController
{
	public abstract void increase();

	public abstract void decrease();
	
	public abstract void reset();
}