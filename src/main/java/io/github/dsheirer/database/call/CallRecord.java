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
package io.github.dsheirer.database.call;

import io.github.dsheirer.protocol.Protocol;

import java.util.List;

/**
 * A call audio event as stored in the calls table.
 *
 * Instances are mutable while they are being built, but a record that has been handed to the database writer or to
 * the user interface is a snapshot that is never changed afterwards.  Use {@link #copy()} to create a new snapshot.
 */
public class CallRecord
{
    public static final String FORM_TALKGROUP = "TALKGROUP";
    public static final String FORM_PATCH_GROUP = "PATCH_GROUP";
    public static final String FORM_RADIO = "RADIO";

    private long mId;
    private long mCallKey;
    private long mStartTime;
    private long mEndTime;
    private CallState mCallState = CallState.ACTIVE;
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
    private String mTalkerAlias;
    private boolean mEncrypted;
    private boolean mDuplicate;
    private int mPriority;
    private boolean mAliasRecord;
    private String mRecordFormat;
    private String mFilePath;
    private long mFileSize;
    private RecordingState mRecordingState = RecordingState.NONE;

    /**
     * Creates a copy (snapshot) of this record.
     */
    public CallRecord copy()
    {
        CallRecord copy = new CallRecord();
        copy.mId = mId;
        copy.mCallKey = mCallKey;
        copy.mStartTime = mStartTime;
        copy.mEndTime = mEndTime;
        copy.mCallState = mCallState;
        copy.mProtocol = mProtocol;
        copy.mSystem = mSystem;
        copy.mSite = mSite;
        copy.mChannel = mChannel;
        copy.mFrequency = mFrequency;
        copy.mTimeslot = mTimeslot;
        copy.mToId = mToId;
        copy.mToValue = mToValue;
        copy.mToForm = mToForm;
        copy.mToFullyQualified = mToFullyQualified;
        copy.mFromId = mFromId;
        copy.mFromValue = mFromValue;
        copy.mFromForm = mFromForm;
        copy.mFromFullyQualified = mFromFullyQualified;
        copy.mAliasList = mAliasList;
        copy.mPatchedTalkgroups = mPatchedTalkgroups;
        copy.mTalkerAlias = mTalkerAlias;
        copy.mEncrypted = mEncrypted;
        copy.mDuplicate = mDuplicate;
        copy.mPriority = mPriority;
        copy.mAliasRecord = mAliasRecord;
        copy.mRecordFormat = mRecordFormat;
        copy.mFilePath = mFilePath;
        copy.mFileSize = mFileSize;
        copy.mRecordingState = mRecordingState;
        return copy;
    }

    /**
     * Database row identifier, or 0 if this call hasn't been inserted yet.
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
     * In-memory key that identifies a live call across snapshots, or 0 for records loaded from the database.
     */
    public long getCallKey()
    {
        return mCallKey;
    }

    public void setCallKey(long callKey)
    {
        mCallKey = callKey;
    }

    /**
     * Call start time, milliseconds since epoch
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
     * Call end time, milliseconds since epoch, or 0 while the call is active.
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
     * Call duration in milliseconds.  For an active call this is the elapsed time up to now.
     */
    public long getDuration()
    {
        if(mEndTime > 0)
        {
            return Math.max(0, mEndTime - mStartTime);
        }

        return isActive() ? Math.max(0, System.currentTimeMillis() - mStartTime) : 0;
    }

    public CallState getCallState()
    {
        return mCallState;
    }

    public void setCallState(CallState callState)
    {
        mCallState = callState;
    }

    /**
     * Indicates if the call is in progress.
     */
    public boolean isActive()
    {
        return mCallState == CallState.ACTIVE;
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
     * Indicates if the TO identifier of this call is a talkgroup or patch group that can be used as a hold talkgroup.
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
     * Call type (group, patch, private or telephone), derived from the TO and FROM identifier forms.
     */
    public CallType getCallType()
    {
        return CallType.from(mToForm, mFromForm);
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
     * Fully qualified address (wacn.system.id) of a fully qualified (roaming) TO talkgroup, or null.
     */
    public String getToFullyQualified()
    {
        return mToFullyQualified;
    }

    public void setToFullyQualified(String toFullyQualified)
    {
        mToFullyQualified = toFullyQualified;
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
     * Talkgroups that are patched into the TO patch group of the call, or an empty list.  Searching for a talkgroup
     * finds the calls that it participated in through a patch.
     */
    public List<PatchedTalkgroup> getPatchedTalkgroups()
    {
        return mPatchedTalkgroups;
    }

    public void setPatchedTalkgroups(List<PatchedTalkgroup> patchedTalkgroups)
    {
        mPatchedTalkgroups = patchedTalkgroups == null ? List.of() : List.copyOf(patchedTalkgroups);
    }

    /**
     * Name of the alias list that was assigned to the channel for this call, or null.  Aliases for the TO and FROM
     * identifiers are looked up in the current version of this alias list when the call is displayed.
     */
    public String getAliasList()
    {
        return mAliasList;
    }

    public void setAliasList(String aliasList)
    {
        mAliasList = aliasList;
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

    public String getTalkerAlias()
    {
        return mTalkerAlias;
    }

    public void setTalkerAlias(String talkerAlias)
    {
        mTalkerAlias = talkerAlias;
    }

    public boolean isEncrypted()
    {
        return mEncrypted;
    }

    public void setEncrypted(boolean encrypted)
    {
        mEncrypted = encrypted;
    }

    public boolean isDuplicate()
    {
        return mDuplicate;
    }

    public void setDuplicate(boolean duplicate)
    {
        mDuplicate = duplicate;
    }

    /**
     * Alias playback priority
     */
    public int getPriority()
    {
        return mPriority;
    }

    public void setPriority(int priority)
    {
        mPriority = priority;
    }

    /**
     * Indicates if an alias of the call has the record option, as opposed to the call being recorded only because
     * all calls are recorded.  Do Not Monitor calls with an alias record option are kept by the retention rule.
     */
    public boolean isAliasRecord()
    {
        return mAliasRecord;
    }

    public void setAliasRecord(boolean aliasRecord)
    {
        mAliasRecord = aliasRecord;
    }

    /**
     * Recording format (MP3 or WAVE), or null when the call isn't recorded.
     */
    public String getRecordFormat()
    {
        return mRecordFormat;
    }

    public void setRecordFormat(String recordFormat)
    {
        mRecordFormat = recordFormat;
    }

    /**
     * Absolute path of the audio recording file, or null.
     */
    public String getFilePath()
    {
        return mFilePath;
    }

    public void setFilePath(String filePath)
    {
        mFilePath = filePath;
    }

    /**
     * Recording file size in bytes
     */
    public long getFileSize()
    {
        return mFileSize;
    }

    public void setFileSize(long fileSize)
    {
        mFileSize = fileSize;
    }

    public RecordingState getRecordingState()
    {
        return mRecordingState;
    }

    public void setRecordingState(RecordingState recordingState)
    {
        mRecordingState = recordingState;
    }

    /**
     * Indicates if this call has a completed audio recording that can be played.
     */
    public boolean isPlayable()
    {
        return mRecordingState == RecordingState.RECORDED && mFilePath != null;
    }

    /**
     * Key used to detect a call that already exists in a target database during an append transfer.  The recording
     * file path identifies a call uniquely.  Calls without a recording use their start time, channel and TO.
     */
    public String getTransferKey()
    {
        if(mFilePath != null && !mFilePath.isEmpty())
        {
            return "F:" + mFilePath;
        }

        return "C:" + mStartTime + "|" + mChannel + "|" + mTimeslot + "|" + mToId + "|" + mFromId;
    }

    @Override
    public String toString()
    {
        return "Call " + mId + " " + mProtocol + " TO:" + mToId + " FROM:" + mFromId + " " + mCallState + " " +
            mRecordingState + (mFilePath != null ? " " + mFilePath : "");
    }
}
