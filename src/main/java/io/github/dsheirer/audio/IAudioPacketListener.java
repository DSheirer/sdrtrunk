package io.github.dsheirer.audio;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableAudioPacket;

public interface IAudioPacketListener
{
	public Listener<ReusableAudioPacket> getAudioPacketListener();
}
