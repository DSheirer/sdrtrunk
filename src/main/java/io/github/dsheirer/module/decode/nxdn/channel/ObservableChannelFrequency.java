/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
 * ****************************************************************************
 */

package io.github.dsheirer.module.decode.nxdn.channel;

import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.util.Callback;

/**
 * JavaFX wrapper for the channel frequency object to make the fields observable.
 */
public class ObservableChannelFrequency
{
    private IntegerProperty mChannel = new SimpleIntegerProperty();
    private LongProperty mDownlink = new SimpleLongProperty();
    private LongProperty mUplink = new SimpleLongProperty();

    /**
     * Constructs an instance
     * @param channelFrequency object with configuration details
     */
    public ObservableChannelFrequency(ChannelFrequency channelFrequency)
    {
        mChannel.set(channelFrequency.getChannel());
        mDownlink.set(channelFrequency.getDownlink());
        mUplink.set(channelFrequency.getUplink());
    }

    /**
     * Constructs an instance with no values.
     */
    public ObservableChannelFrequency()
    {
    }

    /**
     * Creates a ChannelFrequency instance from this observable wrapper.
     */
    public ChannelFrequency getChannel()
    {
        return new ChannelFrequency(mChannel.get(), mDownlink.get(), mUplink.get());
    }

    /**
     * Channel number
     */
    public IntegerProperty channelProperty()
    {
        return mChannel;
    }

    /**
     * Downlink frequency in Hertz
     * @return
     */
    public LongProperty downlinkProperty()
    {
        return mDownlink;
    }

    /**
     * Uplink frequency in Hertz
     */
    public LongProperty uplinkProperty()
    {
        return mUplink;
    }

    /**
     * Creates an observable property extractor for use with observable lists to detect changes internal to this object.
     */
    public static Callback<ObservableChannelFrequency, Observable[]> extractor()
    {
        return (ObservableChannelFrequency ocf) -> new Observable[] {ocf.channelProperty(), ocf.downlinkProperty(),
                ocf.uplinkProperty()};
    }
}
