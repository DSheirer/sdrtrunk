/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.spectrum.converter;

import io.github.dsheirer.spectrum.DFTResultsListener;
import io.github.dsheirer.spectrum.DFTResultsProvider;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class DFTResultsConverter implements DFTResultsListener, DFTResultsProvider
{
	private final List<DFTResultsListener> mListeners = new CopyOnWriteArrayList<>();
	
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
	public void addListener(DFTResultsListener listener)
    {
		mListeners.add(listener);
    }

	@Override
	public void removeListener(DFTResultsListener listener)
    {
		mListeners.remove(listener);
    }

	protected void dispatch(float[] results)
	{
		for(DFTResultsListener listener: mListeners)
		{
			listener.receive(results);
		}
	}
}
