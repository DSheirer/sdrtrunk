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

import io.github.dsheirer.database.IdentifierRecord;

/**
 * A call audio event as stored in the calls table.
 *
 * Instances are mutable while they are being built, but a record that has been handed to the database writer or to
 * the user interface is a snapshot that is never changed afterwards.  Use {@link #copy()} to create a new snapshot.
 */
public class CallRecord extends IdentifierRecord
{
    private long mCallKey;
    private CallState mCallState = CallState.ACTIVE;
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
        copyTo(copy);
        copy.mCallKey = mCallKey;
        copy.mCallState = mCallState;
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
     * Call duration in milliseconds.  For an active call this is the elapsed time up to now.
     */
    public long getDuration()
    {
        if(getEndTime() > 0)
        {
            return Math.max(0, getEndTime() - getStartTime());
        }

        return isActive() ? Math.max(0, System.currentTimeMillis() - getStartTime()) : 0;
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
     * Call type (group, patch, private or telephone), derived from the TO and FROM identifier forms.
     */
    public CallType getCallType()
    {
        return CallType.from(getToForm(), getFromForm());
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

        return "C:" + getStartTime() + "|" + getChannel() + "|" + getTimeslot() + "|" + getToId() + "|" + getFromId();
    }

    @Override
    public String toString()
    {
        return "Call " + getId() + " " + getProtocol() + " TO:" + getToId() + " FROM:" + getFromId() + " " + mCallState + " " +
            mRecordingState + (mFilePath != null ? " " + mFilePath : "");
    }
}
