package ua.in.smartjava.dsp.gain;

public interface GainController
{
	public abstract void increase();

	public abstract void decrease();
	
	public abstract void reset();
}