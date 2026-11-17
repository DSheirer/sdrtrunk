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

package io.github.dsheirer.alias;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;

/**
 * Persisted settings for an alias list.  Alias lists are otherwise identified only by the list name that each alias
 * carries, so settings that belong to the list itself, like tracking a Radio Reference system, are stored here and
 * saved in the playlist.
 *
 * Radio Reference tracking uses two timestamps from the Radio Reference system's last updated value: the last updated
 * value seen by the most recent check, and the last updated value that the aliases were synchronized to.  When the
 * seen value is newer than the synchronized value, the system changed on Radio Reference and the aliases haven't
 * been updated yet.
 */
public class AliasListConfiguration
{
    public static final int NO_SYSTEM = 0;

    private StringProperty mName = new SimpleStringProperty();
    private IntegerProperty mRadioReferenceSystemId = new SimpleIntegerProperty(NO_SYSTEM);
    private StringProperty mRadioReferenceSystemName = new SimpleStringProperty();
    private BooleanProperty mAutoUpdate = new SimpleBooleanProperty();
    private LongProperty mRadioReferenceLastUpdated = new SimpleLongProperty();
    private LongProperty mSynchronizedLastUpdated = new SimpleLongProperty();
    private LongProperty mLastChecked = new SimpleLongProperty();

    /**
     * Jackson constructor
     */
    public AliasListConfiguration()
    {
    }

    /**
     * Constructs an instance
     * @param name of the alias list
     */
    public AliasListConfiguration(String name)
    {
        setName(name);
    }

    /**
     * Extractor so that observable lists of configurations report changes to the persisted properties.
     */
    public static Callback<AliasListConfiguration,Observable[]> extractor()
    {
        return (AliasListConfiguration c) -> new Observable[] {c.mName, c.mRadioReferenceSystemId,
            c.mRadioReferenceSystemName, c.mAutoUpdate, c.mRadioReferenceLastUpdated, c.mSynchronizedLastUpdated,
            c.mLastChecked};
    }

    /**
     * Alias list name
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

    @JsonIgnore
    public StringProperty nameProperty()
    {
        return mName;
    }

    /**
     * Indicates if this alias list tracks a Radio Reference system.
     */
    @JsonIgnore
    public boolean isTrackingRadioReferenceSystem()
    {
        return getRadioReferenceSystemId() > NO_SYSTEM;
    }

    /**
     * Indicates if this alias list tracks the specified Radio Reference system.
     */
    public boolean isTracking(int systemId)
    {
        return systemId > NO_SYSTEM && getRadioReferenceSystemId() == systemId;
    }

    /**
     * Radio Reference system ID tracked by this alias list, or NO_SYSTEM (0).
     */
    @JacksonXmlProperty(isAttribute = true, localName = "rr_system_id")
    public int getRadioReferenceSystemId()
    {
        return mRadioReferenceSystemId.get();
    }

    public void setRadioReferenceSystemId(int systemId)
    {
        mRadioReferenceSystemId.set(systemId);
    }

    @JsonIgnore
    public IntegerProperty radioReferenceSystemIdProperty()
    {
        return mRadioReferenceSystemId;
    }

    /**
     * Radio Reference system name, for display.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "rr_system_name")
    public String getRadioReferenceSystemName()
    {
        return mRadioReferenceSystemName.get();
    }

    public void setRadioReferenceSystemName(String name)
    {
        mRadioReferenceSystemName.set(name);
    }

    @JsonIgnore
    public StringProperty radioReferenceSystemNameProperty()
    {
        return mRadioReferenceSystemName;
    }

    /**
     * Indicates if talkgroups are automatically added or updated as aliases when the system changes on Radio Reference.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "rr_auto_update")
    public boolean isAutoUpdate()
    {
        return mAutoUpdate.get();
    }

    public void setAutoUpdate(boolean autoUpdate)
    {
        mAutoUpdate.set(autoUpdate);
    }

    @JsonIgnore
    public BooleanProperty autoUpdateProperty()
    {
        return mAutoUpdate;
    }

    /**
     * Radio Reference last updated timestamp (ms since epoch) for the system, as seen by the most recent check.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "rr_last_updated")
    public long getRadioReferenceLastUpdated()
    {
        return mRadioReferenceLastUpdated.get();
    }

    public void setRadioReferenceLastUpdated(long timestamp)
    {
        mRadioReferenceLastUpdated.set(timestamp);
    }

    @JsonIgnore
    public LongProperty radioReferenceLastUpdatedProperty()
    {
        return mRadioReferenceLastUpdated;
    }

    /**
     * Radio Reference last updated timestamp (ms since epoch) that the aliases were last synchronized to.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "rr_synchronized")
    public long getSynchronizedLastUpdated()
    {
        return mSynchronizedLastUpdated.get();
    }

    public void setSynchronizedLastUpdated(long timestamp)
    {
        mSynchronizedLastUpdated.set(timestamp);
    }

    @JsonIgnore
    public LongProperty synchronizedLastUpdatedProperty()
    {
        return mSynchronizedLastUpdated;
    }

    /**
     * Time (ms since epoch) of the most recent successful check of the system on Radio Reference.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "rr_last_checked")
    public long getLastChecked()
    {
        return mLastChecked.get();
    }

    public void setLastChecked(long timestamp)
    {
        mLastChecked.set(timestamp);
    }

    @JsonIgnore
    public LongProperty lastCheckedProperty()
    {
        return mLastChecked;
    }

    /**
     * Indicates if the most recent check saw a system change on Radio Reference that the aliases don't include yet.
     */
    @JsonIgnore
    public boolean isUpdateAvailable()
    {
        return isTrackingRadioReferenceSystem() && getRadioReferenceLastUpdated() > getSynchronizedLastUpdated();
    }

    /**
     * Starts tracking the Radio Reference system.  Clears timestamps so that the next refresh synchronizes.
     */
    public void track(int systemId, String systemName)
    {
        setRadioReferenceSystemId(systemId);
        setRadioReferenceSystemName(systemName);
        setRadioReferenceLastUpdated(0);
        setSynchronizedLastUpdated(0);
        setLastChecked(0);
    }

    /**
     * Stops tracking any Radio Reference system.
     */
    public void stopTracking()
    {
        track(NO_SYSTEM, null);
        setAutoUpdate(false);
    }
}
