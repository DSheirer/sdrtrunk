/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.source.mixer;

import javax.sound.sampled.Mixer;

public class MixerChannelConfiguration
{
    private Mixer mMixer;
    private MixerChannel mMixerChannel;

    public MixerChannelConfiguration(Mixer mixer, MixerChannel channel)
    {
        mMixer = mixer;
        mMixerChannel = channel;
    }

    public Mixer getMixer()
    {
        return mMixer;
    }

    public MixerChannel getMixerChannel()
    {
        return mMixerChannel;
    }

    public boolean matches(String mixer, String channels)
    {
        return mixer != null &&
            channels != null &&
            mMixer.getMixerInfo().getName().contentEquals(mixer) &&
            mMixerChannel.name().contentEquals(channels);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(mMixer.getMixerInfo().getName());
        sb.append(" - ");
        sb.append(mMixerChannel.name());

        return sb.toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mMixer == null) ? 0 : mMixer.hashCode());
        result = prime * result
            + ((mMixerChannel == null) ? 0 : mMixerChannel.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
		if(this == obj)
		{
			return true;
		}
		if(obj == null)
		{
			return false;
		}
		if(getClass() != obj.getClass())
		{
			return false;
		}
        MixerChannelConfiguration other = (MixerChannelConfiguration)obj;
        if(mMixer == null)
        {
			if(other.mMixer != null)
			{
				return false;
			}
        }
        else if(!mMixer.equals(other.mMixer))
		{
			return false;
		}
		if(mMixerChannel != other.mMixerChannel)
		{
			return false;
		}
        return true;
    }
}
