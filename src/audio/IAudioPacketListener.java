package audio;

import sample.Listener;

public interface IAudioPacketListener
{
	public Listener<AudioPacket> getAudioPacketListener();
}
