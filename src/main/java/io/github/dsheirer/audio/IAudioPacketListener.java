package io.github.dsheirer.audio;

import io.github.dsheirer.sample.Listener;

public interface IAudioPacketListener
{
	public Listener<AudioPacket> getAudioPacketListener();
}
