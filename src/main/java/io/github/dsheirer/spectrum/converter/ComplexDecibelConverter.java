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


import org.apache.commons.math3.util.FastMath;

/**
 * Converts complex DFT output to scaled dB values with a maximum amplitude of 0 dB and all values scaled to the
 * minimum dB value which is:
 * 
 * Max: 0dB
 * Min: 20 * log10( 1.0 / ( 2.0 ^ ( bit depth - 1) ) = scaling factor
 * 
 * Notes:
 * 
 * The magnitude of each DFT output bin must be scaled by the number of DFT points to normalize the output to a range
 * of ( 0 to 1.0 ).  DFT bin magnitude can be calculated as:
 * 
 * 1) sqrt( bin[ x ] * bin[ x ] + bin[ x + 1 ] * bin[ x + 1] ) / DFTSize or,
 * 2) ( bin[ x ] * bin[ x ] + bin[ x + 1 ] * bin[ x + 1] ) / ( DFTSize * DFTSize )
 * 
 * Convert each scaled DFT bin magnitude to decibels using this formula:
 * 
 * 20 * log10( scaledBinValue )
 * 
 * Max Value: 20 * log10( 1.0 ) = 0 dB
 * 
 * When plotting, these dB bin values should be scaled according to the maximum dynamic range supported by the source.
 * Maximum dynamic range for bit depth is calculated as:
 * 
 * 20 * log10( 1 / ( 2 ^ ( bit depth - 1 ) )
 * 
 * A source that provides 12-bit samples can produce values in the range of ( 0 to 4096 ) or -2048 to 2047.  The
 * smallest observable value would then be 1 / 2048.
 * 
 * So, the smallest decibel value of a source producing 12 bit samples is:
 * 
 * 20 * log10( 1 / 2048 ) = -66.23 dB
 * 
 * and the dynamic range is:  (-66.23 to 0.0 dB)
 */
public class ComplexDecibelConverter extends DFTResultsConverter
{
	/**
	 * Converts the output of the JTransforms FloatFFT_1D.complexForward() calculation into the power spectrum in
	 * decibels, normalized to the sample bit depth.
	 */
	public ComplexDecibelConverter()
	{
	}

	public static float[] convert(float[] results)
	{
		int halfResults = results.length / 2;
		float dftBinSizeScalor = 1.0f / (float) halfResults;
		float[] processed = new float[halfResults];
		int middle = processed.length / 2;

		float temp, decibels;
		for(int x = 0; x < results.length; x += 2)
		{
			//Calculate the magnitude squared (power) value from each bin's real and imaginary value and scale it to the
			// DFT bin size squared. Convert the scaled value to decibels.
			temp = ((results[x] * results[x]) + (results[x + 1] * results[x + 1]));

			if(temp == 0)
			{
				decibels = -196.0f;
			}
			else
			{
				decibels = 10.0f * (float)FastMath.log10(temp * dftBinSizeScalor);
			}

			// We have to swap the upper and lower halves of the JTransforms DFT results for correct display
			int index = x / 2;

			if(index >= middle)
			{
				processed[index - middle] = decibels;
			}
			else
			{
				processed[index + middle] = decibels;
			}
		}

		return processed;
	}

	@Override
	public void receive(float[] results)
    {
		dispatch(convert(results));
    }
}
