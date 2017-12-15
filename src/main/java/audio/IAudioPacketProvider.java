package audio;

import sample.Listener;

public interface IAudioPacketProvider
{
	public void setAudioPacketListener( Listener<AudioPacket> listener );
	public void removeAudioPacketListener();
}
