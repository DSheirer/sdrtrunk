/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
package io.github.dsheirer.alias;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.dsheirer.alias.action.AliasAction;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.alias.id.record.Record;
import io.github.dsheirer.alias.id.talkgroup.StreamAsTalkgroup;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alias provides an aliasing (e.g. name, color, etc) container that is linked to multiple alias identifiers and
 * provides a corresponding set of actions related to the alias.
 */
@JacksonXmlRootElement(localName = "alias")
public class Alias
{
    private final static Logger mLog = LoggerFactory.getLogger(Alias.class);

    private BooleanProperty mRecordable = new SimpleBooleanProperty();
    private BooleanProperty mStreamable = new SimpleBooleanProperty();
    private BooleanProperty mOverlap = new SimpleBooleanProperty();
    private IntegerProperty mColor = new SimpleIntegerProperty();
    private IntegerProperty mPriority = new SimpleIntegerProperty(Priority.DEFAULT_PRIORITY);
    private IntegerProperty mNonAudioIdentifierCount = new SimpleIntegerProperty();
    private StringProperty mAliasListName = new SimpleStringProperty();
    private StringProperty mGroup = new SimpleStringProperty();
    private StringProperty mIconName = new SimpleStringProperty();
    private StringProperty mName = new SimpleStringProperty();
    private ObservableList<AliasID> mAliasIDs = FXCollections.observableArrayList();
    private ObservableList<AliasAction> mAliasActions = FXCollections.observableArrayList();
    private ObjectProperty<StreamAsTalkgroup> mStreamTalkgroupAlias = new SimpleObjectProperty<>();

    /**
     * Constructs an instance and sets the specified name.
     * @param name for the alias
     */
    public Alias(String name)
    {
        mName.set(name);

        //Bind the non-audio identifier count property to the size of a filtered list of alias identifiers
        mNonAudioIdentifierCount.bind(Bindings.size(new FilteredList<>(mAliasIDs, aliasID -> {
            return aliasID != null && !aliasID.isAudioIdentifier();
        })));
    }

    /**
     * Constructs an instance
     */
    public Alias()
    {
        this(null);
    }

    /**
     * Count of non-audio identifiers for this alias
     */
    @JsonIgnore
    public IntegerProperty nonAudioIdentifierCountProperty()
    {
        return mNonAudioIdentifierCount;
    }

    /**
     * Audio playback priority
     */
    @JsonIgnore
    public IntegerProperty priorityProperty()
    {
        return mPriority;
    }

    /**
     * Indicates if one or more of the identifiers for this alias overlap with identifiers from another alias
     * within the same alias list.
     */
    @JsonIgnore
    public BooleanProperty overlapProperty()
    {
        return mOverlap;
    }

    /**
     * Recordable property
     */
    @JsonIgnore
    public BooleanProperty recordableProperty()
    {
        return mRecordable;
    }

    /**
     * Streamable property
     */
    @JsonIgnore
    public BooleanProperty streamableProperty()
    {
        return mStreamable;
    }

    /**
     * Alias list name property
     */
    @JsonIgnore
    public StringProperty aliasListNameProperty()
    {
        return mAliasListName;
    }

    /**
     * Group property
     */
    @JsonIgnore
    public StringProperty groupProperty()
    {
        return mGroup;
    }

    /**
     * Alias name property
     * @return
     */
    @JsonIgnore
    public StringProperty nameProperty()
    {
        return mName;
    }

    /**
     * Alias color value property
     */
    @JsonIgnore
    public IntegerProperty colorProperty()
    {
        return mColor;
    }

    @JsonIgnore
    public ObjectProperty streamTalkgroupAliasProperty()
    {
        return mStreamTalkgroupAlias;
    }

    /**
     * Icon name property
     */
    @JsonIgnore
    public StringProperty iconNameProperty()
    {
        return mIconName;
    }

    /**
     * Alias identifiers list property
     */
    @JsonIgnore
    public ObservableList<AliasID> aliasIds()
    {
        return mAliasIDs;
    }

    /**
     * Alias actions list property
     */
    @JsonIgnore
    public ObservableList<AliasAction> aliasActions()
    {
        return mAliasActions;
    }

    /**
     * Uses name value for string representation
     */
    public String toString()
    {
        return getName();
    }

    /**
     * Alias name
     */
    @JacksonXmlProperty(isAttribute = true, localName = "name")
    public String getName()
    {
        return mName.get();
    }

    public void setName(String name)
    {
        mName.set(name);
    }

    /**
     * Alias name
     */
    @JacksonXmlProperty(isAttribute = true, localName = "stream_talkgroup_alias")
    public StreamAsTalkgroup getStreamTalkgroupAlias()
    {
        return mStreamTalkgroupAlias.get();
    }

    /**
     * Sets the stream as talkgroup value that will be used in the TO field for streamed audio calls.
     * @param streamTalkgroupAlias with a talkgroup value.
     */
    public void setStreamTalkgroupAlias(StreamAsTalkgroup streamTalkgroupAlias)
    {
        mStreamTalkgroupAlias.set(streamTalkgroupAlias);
    }

    /**
     * Alias list name for the alias list that this alias belongs to
     */
    @JacksonXmlProperty(isAttribute = true, localName = "list")
    public String getAliasListName()
    {
        return mAliasListName.get();
    }

    public void setAliasListName(String aliasListName)
    {
        mAliasListName.set(aliasListName);
    }

    @JsonIgnore
    public boolean matchesAliasList(String aliasList)
    {
        boolean isEmptyArgument = aliasList == null || aliasList.isEmpty() || aliasList.contentEquals(AliasModel.NO_ALIAS_LIST);
        boolean isEmptyThis = getAliasListName() == null || getAliasListName().isEmpty();

        if(isEmptyArgument && isEmptyThis)
        {
            return true;
        }
        else if(!isEmptyArgument && !isEmptyThis && aliasList.contentEquals(getAliasListName()))
        {
            return true;
        }

        return false;
    }

    /**
     * Indicates if this alias has a specified alias list name
     */
    public boolean hasList()
    {
        return mAliasListName.get() != null;
    }

    /**
     * Grouping tag for this alias.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "group")
    public String getGroup()
    {
        return mGroup.get();
    }

    public void setGroup(String group)
    {
        mGroup.set(group);
    }

    /**
     * Indicates if this alias has a grouping tag
     */
    public boolean hasGroup()
    {
        return mGroup.get() != null;
    }

    /**
     * Color (RGBA) value to use for this alias
     */
    @JacksonXmlProperty(isAttribute = true, localName = "color")
    public int getColor()
    {
        return mColor.get();
    }

    @JsonIgnore
    public String getColorHex()
    {
        return String.format("#%06X", (0xFFFFFF & mColor.get()));
    }

    public void setColor(int color)
    {
        mColor.set(color);
    }

    /**
     * Display color for this alias.
     */
    @JsonIgnore
    public Color getDisplayColor()
    {
        return new Color(getColor());
    }

    /**
     * Icon name of the icon to use for this alias.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "iconName")
    public String getIconName()
    {
        return mIconName.get();
    }

    public void setIconName(String iconName)
    {
        mIconName.set(iconName);
    }

    /**
     * List of alias identifiers for this alias.
     */
    @JacksonXmlProperty(isAttribute = false, localName = "id")
    public List<AliasID> getAliasIdentifiers()
    {
        return mAliasIDs;
    }

    public void setAliasIdentifiers(List<AliasID> id)
    {
        mAliasIDs.clear();

        for(AliasID aliasID : id)
        {
            addAliasID(aliasID);
        }
    }

    /**
     * Adds an alias identifier to this alias and updates the recordable and streamable attributes.
     */
    public void addAliasID(AliasID id)
    {
        mAliasIDs.add(id);
        validate();
        updateOverlapBinding();
    }

    /**
     * Removes an alias identifier from this alias and updates the recordable and streamable attributes.
     */
    public void removeAliasID(AliasID id)
    {
        if(id != null)
        {
            mAliasIDs.remove(id);
            validate();
            updateOverlapBinding();
        }
    }

    /**
     * List of alias actions associated with this alias.
     */
    @JacksonXmlProperty(isAttribute = false, localName = "action")
    public List<AliasAction> getAliasActions()
    {
        return mAliasActions;
    }

    public void setAliasActions(List<AliasAction> actions)
    {
        mAliasActions.setAll(actions);
    }

    /**
     * Adds the specified alias action to this alias
     */
    public void addAliasAction(AliasAction action)
    {
        mAliasActions.add(action);
    }

    /**
     * Removes the specified alias action from this alias
     */
    public void removeAliasAction(AliasAction action)
    {
        mAliasActions.remove(action);
    }

    /**
     * Indicates if this alias has any associated
     * @return
     */
    public boolean hasActions()
    {
        return !mAliasActions.isEmpty();
    }


    /**
     * Updates the playback priority this alias
     */
    private void updatePriority()
    {
        for(AliasID aliasID: mAliasIDs)
        {
            if(aliasID instanceof Priority)
            {
                mPriority.set(((Priority)aliasID).getPriority());
                return;
            }
        }

        mPriority.set(Priority.DEFAULT_PRIORITY);
    }

    private void updateOverlapBinding()
    {
        mOverlap.unbind();

        BooleanBinding binding = new SimpleBooleanProperty(true).not();

        for(AliasID aliasID: mAliasIDs)
        {
            binding = binding.or(aliasID.overlapProperty());
        }

        mOverlap.bind(binding);
    }

    /**
     * Updates the recordable status for this alias
     */
    private void updateRecordable()
    {
        for(AliasID aliasID: mAliasIDs)
        {
            if(aliasID instanceof Record)
            {
                mRecordable.set(true);
                return;
            }
        }

        mRecordable.set(false);
    }

    /**
     * Updates the streamable status for this alias
     */
    private void updateStreamable()
    {
        for(AliasID aliasID: mAliasIDs)
        {
            if(aliasID instanceof BroadcastChannel)
            {
                mStreamable.set(true);
                return;
            }
        }

        mStreamable.set(false);
    }

    /**
     * Perform any validation/cleanup actions on this alias.
     */
    public void validate()
    {
        updatePriority();
        updateRecordable();
        updateStreamable();
    }

    /**
     * Returns the priority level of this alias, if defined, or the default priority
     */
    @JsonIgnore
    public int getPlaybackPriority()
    {
        for(AliasID id : mAliasIDs)
        {
            if(id.getType() == AliasIDType.PRIORITY)
            {
                return ((Priority)id).getPriority();
            }
        }

        return Priority.DEFAULT_PRIORITY;
    }

    public boolean hasCallPriority()
    {
        for(AliasID id : mAliasIDs)
        {
            if(id.getType() == AliasIDType.PRIORITY)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Sets or updates the call priority
     */
    public void setCallPriority(int priority)
    {
        AliasID priorityID = null;

        for(AliasID aliasID: mAliasIDs)
        {
            if(aliasID instanceof Priority)
            {
                priorityID = aliasID;
            }
        }

        removeAliasID(priorityID);

        //Don't add a priority alias id if the priority is MAX_PRIORITY or DEFAULT_PRIORITY since the getCallPriority()
        //will return that as the default value
        if(priority == Priority.DO_NOT_MONITOR || (Priority.MIN_PRIORITY <= priority && priority < Priority.MAX_PRIORITY))
        {
            addAliasID(new Priority(priority));
        }
    }

    /**
     * Inspects the alias for a non-recordable alias id.  Default is true;
     */
    @JsonIgnore
    public boolean isRecordable()
    {
        return mRecordable.get();
    }

    /**
     * Sets or removes the record alias ID for this alias.
     */
    public void setRecordable(boolean recordable)
    {
        AliasID recordID = null;

        for(AliasID aliasID: mAliasIDs)
        {
            if(aliasID instanceof Record)
            {
                recordID = aliasID;
            }
        }

        removeAliasID(recordID);

        if(recordable)
        {
            addAliasID(new Record());
        }
    }

    /**
     * Inspects the alias for broadcast channel/streamable alias ids.  Default is false;
     */
    @JsonIgnore
    public boolean isStreamable()
    {
        return mStreamable.get();
    }

    /**
     * List of broadcast channels specified for this alias.
     */
    @JsonIgnore
    public Set<BroadcastChannel> getBroadcastChannels()
    {
        Set<BroadcastChannel> broadcastChannels = new TreeSet<>();

        for(AliasID id : getAliasIdentifiers())
        {
            if(id.getType() == AliasIDType.BROADCAST_CHANNEL)
            {
                broadcastChannels.add((BroadcastChannel)id);
            }
        }

        return broadcastChannels;
    }

    /**
     * Indicates if this alias contains a broadcast channel alias id with the channel name.
     */
    public boolean hasBroadcastChannel(String channel)
    {
        if(channel == null || channel.isEmpty())
        {
            return false;
        }

        for(AliasID id : getAliasIdentifiers())
        {
            if(id instanceof BroadcastChannel)
            {
                BroadcastChannel broadcastChannel = (BroadcastChannel)id;

                if(broadcastChannel.getChannelName() != null && broadcastChannel.getChannelName().contentEquals(channel))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Removes any broadcast channel(s) that match the argument
     */
    public void removeBroadcastChannel(String channel)
    {
        if(channel == null || channel.isEmpty())
        {
            return;
        }

        List<AliasID> toRemove = new ArrayList<>();

        for(AliasID aliasID: getAliasIdentifiers())
        {
            if(aliasID instanceof BroadcastChannel &&
                ((BroadcastChannel)aliasID).getChannelName().contentEquals(channel))
            {
                toRemove.add(aliasID);
            }
        }

        for(AliasID aliasID: toRemove)
        {
            removeAliasID(aliasID);
        }
    }

    /**
     * Removes (clears) all broadcast channels from this alias
     */
    public void removeAllBroadcastChannels()
    {
        List<AliasID> toRemove = new ArrayList<>();
        for(AliasID aliasID: mAliasIDs)
        {
            if(aliasID instanceof BroadcastChannel)
            {
                toRemove.add(aliasID);
            }
        }

        for(AliasID aliasID: toRemove)
        {
            removeAliasID(aliasID);
        }
    }

    /**
     * Removes all non audio identifiers to prepare for replacing them from an editor.
     */
    public void removeNonAudioIdentifiers()
    {
        Iterator<AliasID> it = mAliasIDs.iterator();

        while(it.hasNext())
        {
            if(!it.next().isAudioIdentifier())
            {
                it.remove();
            }
        }
    }

    /**
     * Removes all alias actions from this alias
     */
    public void removeAllActions()
    {
        mAliasActions.clear();
    }

    /**
     * Creates an observable property extractor for use with observable lists to detect changes internal to this object.
     */
    public static Callback<Alias,Observable[]> extractor()
    {
        return (Alias a) -> new Observable[] {a.recordableProperty(), a.streamableProperty(), a.colorProperty(),
            a.aliasListNameProperty(), a.groupProperty(), a.iconNameProperty(), a.nameProperty(), a.aliasIds(),
            a.aliasActions(), a.nonAudioIdentifierCountProperty(), a.overlapProperty(), a.priorityProperty(),
                a.streamTalkgroupAliasProperty()};
    }
}
