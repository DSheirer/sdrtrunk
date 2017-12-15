package ua.in.smartjava.audio;

import ua.in.smartjava.sample.Listener;

public interface IAudioPacketProvider
{
	public void setAudioPacketListener( Listener<AudioPacket> listener );
	public void removeAudioPacketListener();
}
