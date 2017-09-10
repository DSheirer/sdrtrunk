/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package dsp.filter.channelizer.output;

public class ChannelOutputProcessorFactory
{
    /**
     * Factory for obtaining polyphase channelizer output processors to perform any post-processing
     * that may be required to perform frequency translation and/or resampling of channel results.
     */
    public static IPolyphaseChannelOutputProcessor getProcessor(int sampleRate, long frequency)
    {
        return null;
    }
}
