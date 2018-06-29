package io.github.dsheirer.audio;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableAudioPacket;

public interface IAudioPacketProvider
{
	public void setAudioPacketListener( Listener<ReusableAudioPacket> listener );
	public void removeAudioPacketListener();
}
