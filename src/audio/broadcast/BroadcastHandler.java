/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
 *
 ******************************************************************************/
package audio.broadcast;

import audio.AudioPacket;
import audio.convert.IAudioConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class BroadcastHandler
{
    private final static Logger mLog = LoggerFactory.getLogger( BroadcastHandler.class );

    protected BroadcastConfiguration mBroadcastConfiguration;
    protected IAudioConverter mAudioConverter;
    private List<Listener<BroadcastState>> mBroadcastStateListeners = new CopyOnWriteArrayList<>();
    private BroadcastState mBroadcastState = BroadcastState.READY;

    /**
     * Abstract audio broadcast handler.  Protocol-specific implementations will manage a connection with the remote
     * broadcast audio server, convert audio using the audio converter argument and dispatch the converted audio to
     * the remote server.
     *
     * @param broadcastConfiguration for configuring the handler
     * @param audioConverter to convert audio packets to the desired output format
     */
    public BroadcastHandler(BroadcastConfiguration broadcastConfiguration, IAudioConverter audioConverter)
    {
        mBroadcastConfiguration = broadcastConfiguration;
        mAudioConverter = audioConverter;
    }

    public BroadcastConfiguration getBroadcastConfiguration()
    {
        return mBroadcastConfiguration;
    }

    /**
     * Audio converter
     */
    protected IAudioConverter getAudioConverter()
    {
        return mAudioConverter;
    }

    /**
     * Processes and broadcasts the audio packets
     */
    public abstract void broadcast(List<AudioPacket> audioPackets);

    /**
     * Disconnects from the remote server and disables the broadcaster.
     */
    public abstract void disconnect();

    /**
     * Pauses or unpauses the broadcast
     */
    public void setPaused(boolean paused)
    {
        if(!getBroadcastState().isErrorState())
        {
            if(paused)
            {
                setBroadcastState(BroadcastState.PAUSED);
            }
            else
            {
                setBroadcastState(BroadcastState.READY);
            }
        }
    }

    /**
     * Registers the listener to receive broadcast state changes
     */
    public void addListener(Listener<BroadcastState> listener)
    {
        mBroadcastStateListeners.add(listener);
    }

    /**
     * Removes the listener from receiving broadcast state changes
     */
    public void removeListener(Listener<BroadcastState> listener)
    {
        mBroadcastStateListeners.remove(listener);
    }

    /**
     * Sets the state of the broadcast connection
     */
    protected void setBroadcastState(BroadcastState state)
    {
        if(mBroadcastState != state)
        {
            mLog.debug("Changing State to: " + state);
            mBroadcastState = state;

            for(Listener<BroadcastState> listener: mBroadcastStateListeners)
            {
                listener.receive(state);
            }
        }
    }

    /**
     * Current state of the broadcast connection
     */
    public BroadcastState getBroadcastState()
    {
        return mBroadcastState;
    }

    public boolean connected()
    {
        return getBroadcastState() == BroadcastState.CONNECTED;
    }
}
