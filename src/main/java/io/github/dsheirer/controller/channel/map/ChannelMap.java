/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.controller.channel.map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.util.Callback;

import java.util.List;

public class ChannelMap
{
    private StringProperty mName = new SimpleStringProperty();
    private BooleanProperty mValidProperty = new SimpleBooleanProperty();
    private ObservableList<ChannelRange> mRanges = FXCollections.observableArrayList(ChannelRange.extractor());

    public ChannelMap()
    {
        this("New Channel Map");
    }

    public ChannelMap(String name)
    {
        mName.set(name);
        mRanges.addListener((ListChangeListener<ChannelRange>)c -> validate());
    }

    public StringProperty nameProperty()
    {
        return mName;
    }

    public BooleanProperty validProperty()
    {
        return mValidProperty;
    }

    @JsonIgnore
    public ObservableList<ChannelRange> getItems()
    {
        return mRanges;
    }

    public ChannelMap copyOf()
    {
        ChannelMap map = new ChannelMap(new String(mName.get()));

        for(ChannelRange range : mRanges)
        {
            map.addRange(range.copyOf());
        }

        return map;
    }

    @JsonIgnore
    public boolean isValid()
    {
        return mValidProperty.get();
    }

    public String toString()
    {
        return mName.get();
    }

    @JacksonXmlProperty(isAttribute = false, localName = "range")
    public List<ChannelRange> getRanges()
    {
        return mRanges;
    }

    public void setRanges(List<ChannelRange> ranges)
    {
        mRanges.setAll(ranges);
    }

    @JacksonXmlProperty(isAttribute = true, localName = "name")
    public String getName()
    {
        return mName.get();
    }

    public void setName(String name)
    {
        mName.set(name);
    }

    public long getFrequency(int channelNumber)
    {
        for(ChannelRange range : mRanges)
        {
            if(range.hasChannel(channelNumber))
            {
                return range.getFrequency(channelNumber);
            }
        }

        return 0;
    }

    /**
     * Validates each of the channel ranges for overlap
     */
    private void validate()
    {
        mValidProperty.set(true);

        for(ChannelRange channelRange: mRanges)
        {
            channelRange.setOverlapping(false);
        }

        for(int x = 0; x < mRanges.size(); x++)
        {
            if(!mRanges.get(x).isValid())
            {
                mValidProperty.set(false);
            }

            for(int y = x + 1; y < mRanges.size(); y++)
            {
                if(mRanges.get(x).overlaps(mRanges.get(y)))
                {
                    mRanges.get(x).setOverlapping(true);
                    mRanges.get(y).setOverlapping(true);
                    mValidProperty.set(false);
                }
            }
        }
    }


    public void addRange(ChannelRange range)
    {
        mRanges.add(range);
    }

    public void removeRange(ChannelRange range)
    {
        mRanges.remove(range);
    }


    /**
     * Creates an observable property extractor for use with observable lists to detect changes internal to this object.
     */
    public static Callback<ChannelMap,Observable[]> extractor()
    {
        return (ChannelMap c) -> new Observable[] {c.nameProperty(), c.validProperty(), c.getItems()};
    }
}
