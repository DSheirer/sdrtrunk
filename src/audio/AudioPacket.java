package audio;

import sample.real.RealBuffer;
import audio.metadata.AudioMetadata;

/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2016 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
public class AudioPacket
{
	private Type mType;
	private RealBuffer mAudioData;
	private AudioMetadata mAudioMetadata;
	
	public AudioPacket( Type type, AudioMetadata metadata )
	{
		mType = type;
		mAudioMetadata = metadata;
	}
	
	public AudioPacket( float[] audio, AudioMetadata metadata )
	{
		this( Type.AUDIO, metadata );

		mAudioData = new RealBuffer( audio );
	}
	
	public boolean hasAudioMetadata()
	{
		return mAudioMetadata != null;
	}
	
	public AudioMetadata getAudioMetadata()
	{
		return mAudioMetadata;
	}
	
	public void setMetadata( AudioMetadata metadata )
	{
		mAudioMetadata = metadata;
	}
	
	public Type getType()
	{
		return mType;
	}
	
	public RealBuffer getAudioBuffer()
	{
		return mAudioData;
	}
	
	public boolean hasAudioBuffer()
	{
		return mAudioData != null;
	}
	
	public enum Type
	{
		AUDIO,
		END;
	}
}
