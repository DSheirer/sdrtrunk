package ua.in.smartjava.audio;

import ua.in.smartjava.sample.Listener;

public interface IAudioPacketListener
{
	public Listener<AudioPacket> getAudioPacketListener();
}
