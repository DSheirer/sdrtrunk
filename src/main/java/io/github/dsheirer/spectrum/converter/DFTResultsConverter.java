package io.github.dsheirer.spectrum.converter;

import io.github.dsheirer.spectrum.DFTResultsListener;
import io.github.dsheirer.spectrum.DFTResultsProvider;

import java.util.concurrent.CopyOnWriteArrayList;

public abstract class DFTResultsConverter 
			implements DFTResultsListener, DFTResultsProvider
{
	private CopyOnWriteArrayList<DFTResultsListener> mListeners = 
						new CopyOnWriteArrayList<DFTResultsListener>();
	
	/**
	 * DFT Results Converter - for converting the output of the JTransforms
	 * FFT library real and complex forward results
	 */
	public DFTResultsConverter()
	{
	}

	public void dispose()
	{
		mListeners.clear();
	}

	@Override
    public void addListener( DFTResultsListener listener )
    {
		mListeners.add( listener );
    }

	@Override
    public void removeListener( DFTResultsListener listener )
    {
		mListeners.remove( listener );
    }

	protected void dispatch( float[] results )
	{
		for( DFTResultsListener listener: mListeners )
		{
			listener.receive( results );
		}
	}
}
