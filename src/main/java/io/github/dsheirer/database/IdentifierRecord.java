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
package io.github.dsheirer.database;

import io.github.dsheirer.database.call.PatchedTalkgroup;
import io.github.dsheirer.protocol.Protocol;

import java.util.List;

/**
 * Base class for a stored record (call or decode event) with the channel details and the TO and FROM identifiers.
 *
 * The TO and FROM identifiers are stored as the formatted identifier, the numeric value, the identifier form and the
 * fully qualified address, together with the alias list name of the channel, so that the aliases can be looked up in
 * the current alias list when the record is displayed and so that records can be searched by alias.
 */
public abstract class IdentifierRecord implements DatabaseRecord
{
    public static final String FORM_TALKGROUP = "TALKGROUP";
    public static final String FORM_PATCH_GROUP = "PATCH_GROUP";
    public static final String FORM_RADIO = "RADIO";

    private long mId;
    private long mStartTime;
    private long mEndTime;
    private String mProtocol;
    private String mSystem;
    private String mSite;
    private String mChannel;
    private long mFrequency;
    private int mTimeslot;
    private String mToId;
    private Long mToValue;
    private String mToForm;
    private String mToFullyQualified;
    private String mFromId;
    private Long mFromValue;
    private String mFromForm;
    private String mFromFullyQualified;
    private String mAliasList;
    private List<PatchedTalkgroup> mPatchedTalkgroups = List.of();

    /**
     * Copies the fields of this base class to the target, for creating a snapshot.
     */
    protected void copyTo(IdentifierRecord target)
    {
        target.mId = mId;
        target.mStartTime = mStartTime;
        target.mEndTime = mEndTime;
        target.mProtocol = mProtocol;
        target.mSystem = mSystem;
        target.mSite = mSite;
        target.mChannel = mChannel;
        target.mFrequency = mFrequency;
        target.mTimeslot = mTimeslot;
        target.mToId = mToId;
        target.mToValue = mToValue;
        target.mToForm = mToForm;
        target.mToFullyQualified = mToFullyQualified;
        target.mFromId = mFromId;
        target.mFromValue = mFromValue;
        target.mFromForm = mFromForm;
        target.mFromFullyQualified = mFromFullyQualified;
        target.mAliasList = mAliasList;
        target.mPatchedTalkgroups = mPatchedTalkgroups;
    }

    /**
     * Database row identifier, or 0 if this record hasn't been inserted yet.
     */
    public long getId()
    {
        return mId;
    }

    public void setId(long id)
    {
        mId = id;
    }

    /**
     * Start time, milliseconds since epoch
     */
    public long getStartTime()
    {
        return mStartTime;
    }

    public void setStartTime(long startTime)
    {
        mStartTime = startTime;
    }

    /**
     * End time, milliseconds since epoch, or 0 when there is no end time (yet).
     */
    public long getEndTime()
    {
        return mEndTime;
    }

    public void setEndTime(long endTime)
    {
        mEndTime = endTime;
    }

    /**
     * Protocol enumeration entry name (e.g. APCO25)
     */
    public String getProtocol()
    {
        return mProtocol;
    }

    public void setProtocol(String protocol)
    {
        mProtocol = protocol;
    }

    /**
     * Protocol, or UNKNOWN if the stored value isn't recognized.
     */
    public Protocol getProtocolValue()
    {
        if(mProtocol != null)
        {
            try
            {
                return Protocol.valueOf(mProtocol);
            }
            catch(IllegalArgumentException iae)
            {
                //Fall through
            }
        }

        return Protocol.UNKNOWN;
    }

    public String getSystem()
    {
        return mSystem;
    }

    public void setSystem(String system)
    {
        mSystem = system;
    }

    public String getSite()
    {
        return mSite;
    }

    public void setSite(String site)
    {
        mSite = site;
    }

    /**
     * Channel name from the channel configuration
     */
    public String getChannel()
    {
        return mChannel;
    }

    public void setChannel(String channel)
    {
        mChannel = channel;
    }

    /**
     * Channel frequency in Hertz, or 0 if unknown.
     */
    public long getFrequency()
    {
        return mFrequency;
    }

    public void setFrequency(long frequency)
    {
        mFrequency = frequency;
    }

    public int getTimeslot()
    {
        return mTimeslot;
    }

    public void setTimeslot(int timeslot)
    {
        mTimeslot = timeslot;
    }

    /**
     * Formatted TO identifier (talkgroup, patch group or radio)
     */
    public String getToId()
    {
        return mToId;
    }

    public void setToId(String toId)
    {
        mToId = toId;
    }

    /**
     * Numeric TO identifier value, or null.  For a patch group, this is the patch group talkgroup.
     */
    public Long getToValue()
    {
        return mToValue;
    }

    public void setToValue(Long toValue)
    {
        mToValue = toValue;
    }

    /**
     * Indicates if the TO identifier is a talkgroup or patch group that can be used as a hold talkgroup.
     */
    public boolean hasTalkgroup()
    {
        return mToValue != null && mToValue > 0 && isToTalkgroup();
    }

    /**
     * Indicates if the TO identifier is a talkgroup or patch group (as opposed to a radio, for a private call).
     */
    public boolean isToTalkgroup()
    {
        return FORM_TALKGROUP.equals(mToForm) || FORM_PATCH_GROUP.equals(mToForm);
    }

    /**
     * Indicates if the TO identifier is a radio
     */
    public boolean isToRadio()
    {
        return FORM_RADIO.equals(mToForm);
    }

    /**
     * Identifier form (Form enumeration name, e.g. TALKGROUP, PATCH_GROUP or RADIO) of the TO identifier, or null.
     */
    public String getToForm()
    {
        return mToForm;
    }

    public void setToForm(String toForm)
    {
        mToForm = toForm;
    }

    /**
     * Fully qualified address (wacn.system.id) of a fully qualified (roaming) TO identifier, or null.
     */
    public String getToFullyQualified()
    {
        return mToFullyQualified;
    }

    public void setToFullyQualified(String toFullyQualified)
    {
        mToFullyQualified = toFullyQualified;
    }

    public String getFromId()
    {
        return mFromId;
    }

    public void setFromId(String fromId)
    {
        mFromId = fromId;
    }

    public Long getFromValue()
    {
        return mFromValue;
    }

    public void setFromValue(Long fromValue)
    {
        mFromValue = fromValue;
    }

    /**
     * Identifier form (Form enumeration name, e.g. RADIO) of the FROM identifier, or null.
     */
    public String getFromForm()
    {
        return mFromForm;
    }

    public void setFromForm(String fromForm)
    {
        mFromForm = fromForm;
    }

    /**
     * Indicates if the FROM identifier is a radio
     */
    public boolean isFromRadio()
    {
        return FORM_RADIO.equals(mFromForm);
    }

    /**
     * Fully qualified address (wacn.system.id) of a fully qualified (roaming) FROM radio, or null.
     */
    public String getFromFullyQualified()
    {
        return mFromFullyQualified;
    }

    public void setFromFullyQualified(String fromFullyQualified)
    {
        mFromFullyQualified = fromFullyQualified;
    }

    /**
     * Name of the alias list that was assigned to the channel, or null.  Aliases for the TO and FROM identifiers are
     * looked up in the current version of this alias list when the record is displayed.
     */
    public String getAliasList()
    {
        return mAliasList;
    }

    public void setAliasList(String aliasList)
    {
        mAliasList = aliasList;
    }

    /**
     * Talkgroups that are patched into the TO patch group, or an empty list.  Searching for a talkgroup finds the
     * records that it participated in through a patch.
     */
    public List<PatchedTalkgroup> getPatchedTalkgroups()
    {
        return mPatchedTalkgroups;
    }

    public void setPatchedTalkgroups(List<PatchedTalkgroup> patchedTalkgroups)
    {
        mPatchedTalkgroups = patchedTalkgroups == null ? List.of() : List.copyOf(patchedTalkgroups);
    }
}
