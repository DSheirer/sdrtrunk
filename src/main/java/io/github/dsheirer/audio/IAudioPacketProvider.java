package io.github.dsheirer.audio;

import io.github.dsheirer.sample.Listener;

public interface IAudioPacketProvider
{
	public void setAudioPacketListener( Listener<AudioPacket> listener );
	public void removeAudioPacketListener();
}
