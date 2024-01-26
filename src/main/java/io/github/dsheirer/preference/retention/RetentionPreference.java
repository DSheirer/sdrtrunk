/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.preference.retention;

import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User preferences for (audio) retention and age-off
 */
public class RetentionPreference extends Preference
{
    private static final String PREFERENCE_KEY_AUDIO_RETENTION_POLICY = "audio.retention.policy";
    private static final String PREFERENCE_KEY_AGE_UNITS = "call.age.units";
    private static final String PREFERENCE_KEY_AGE_VALUE = "call.age.value";
    private static final String PREFERENCE_KEY_SIZE_UNITS = "call.size.units";
    private static final String PREFERENCE_KEY_MAX_CALL_DIRECTORY_SIZE = "audio.max.directory.size.gb";
    private static final long GIGABYTES = 1024l * 1024l * 1024l;
    private static final long MEGABYTES = 1024l * 1024l;
    private final static Logger mLog = LoggerFactory.getLogger(RetentionPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(RetentionPreference.class);
    private RetentionPolicy mRetentionPolicy;
    private AgeUnits mAgeUnits;
    private Integer mAgeValue;
    private SizeUnits mSizeUnits;
    private Integer mMaxDirectorySize;

    /**
     * Constructs this preference with an update listener
     * @param updateListener to receive notifications whenever these preferences change
     */
    public RetentionPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.RETENTION;
    }

    /**
     * Retention policy to use for age-off of call events and related audio recordings.
     * @return retention policy.
     */
    public RetentionPolicy getRetentionPolicy()
    {
        if(mRetentionPolicy == null)
        {
            String raw = mPreferences.get(PREFERENCE_KEY_AUDIO_RETENTION_POLICY, null);

            if(raw != null)
            {
                try
                {
                    mRetentionPolicy = RetentionPolicy.valueOf(raw);
                }
                catch(Exception e )
                {
                    //Do nothing ... use the default.
                }
            }

            if(mRetentionPolicy == null)
            {
                mRetentionPolicy = RetentionPolicy.AGE_AND_SIZE; //Default value
            }
        }

        return mRetentionPolicy;
    }

    /**
     * Sets the audio/call retention policy.
     * @param retentionPolicy to use
     */
    public void setRetentionPolicy(RetentionPolicy retentionPolicy)
    {
        mRetentionPolicy = retentionPolicy;
        mPreferences.put(PREFERENCE_KEY_AUDIO_RETENTION_POLICY, retentionPolicy.name());
        notifyPreferenceUpdated();
    }

    /**
     * Age units for call/audio age-off
     * @return age units.
     */
    public AgeUnits getAgeUnits()
    {
        if(mAgeUnits == null)
        {
            String raw = mPreferences.get(PREFERENCE_KEY_AGE_UNITS, null);

            if(raw != null)
            {
                try
                {
                    mAgeUnits = AgeUnits.valueOf(raw);
                }
                catch(Exception e )
                {
                    //Do nothing ... use the default.
                }
            }

            if(mAgeUnits == null)
            {
                mAgeUnits = AgeUnits.DAYS;  //Default value
            }
        }

        return mAgeUnits;
    }

    /**
     * Sets the age units.
     * @param ageUnits to set
     */
    public void setAgeUnits(AgeUnits ageUnits)
    {
        mAgeUnits = ageUnits;
        mPreferences.put(PREFERENCE_KEY_AGE_UNITS, mAgeUnits.name());
        notifyPreferenceUpdated();
    }

    /**
     * Age value
     * @return age value.
     */
    public int getAgeValue()
    {
        if(mAgeValue == null)
        {
            mAgeValue = mPreferences.getInt(PREFERENCE_KEY_AGE_VALUE, 30);
        }

        return mAgeValue;
    }

    /**
     * Sets the age value.
     * @param value to set
     */
    public void setAgeValue(int value)
    {
        mPreferences.putInt(PREFERENCE_KEY_AGE_VALUE, value);
        notifyPreferenceUpdated();
    }

    /**
     * Maximum directory size in gigabytes (GB)
     * @return size
     */
    public int getMaxDirectorySize()
    {
        if(mMaxDirectorySize == null)
        {
            mMaxDirectorySize = mPreferences.getInt(PREFERENCE_KEY_MAX_CALL_DIRECTORY_SIZE, 5);
        }

        return mMaxDirectorySize;
    }

    /**
     * Objective file system directory size.
     * Note: value is adjusted for byte-2-kilobyte (1000 vs 1024) conversion.
     * @return objective directory size.
     */
    public long getObjectiveDirectorySize()
    {
        if(getSizeUnits() == SizeUnits.GB)
        {
            return getMaxDirectorySize() * GIGABYTES;
        }
        else
        {
            return getMaxDirectorySize() * MEGABYTES;
        }
    }

    /**
     * Sets the maximum call directory size in gigabytes (GB)
     * @param sizeGb for the directory.
     */
    public void setMaxDirectorySize(int sizeGb)
    {
        mMaxDirectorySize = sizeGb;
        mPreferences.putInt(PREFERENCE_KEY_MAX_CALL_DIRECTORY_SIZE, sizeGb);
        notifyPreferenceUpdated();
    }

    /**
     * Size units for directory sizing.
     * @return size units or default of gigabytes.
     */
    public SizeUnits getSizeUnits()
    {
        if(mSizeUnits == null)
        {
            String raw = mPreferences.get(PREFERENCE_KEY_SIZE_UNITS, null);

            if(raw != null)
            {
                try
                {
                    mSizeUnits = SizeUnits.valueOf(raw);
                }
                catch(Exception e)
                {
                    //Do nothing.
                }
            }

            if(mSizeUnits == null)
            {
                mSizeUnits = SizeUnits.GB;  //default
            }
        }

        return mSizeUnits;
    }

    /**
     * Sets the size units for directory sizing.
     * @param sizeUnits to set
     */
    public void setSizeUnits(SizeUnits sizeUnits)
    {
        mSizeUnits = sizeUnits;
        mPreferences.put(PREFERENCE_KEY_SIZE_UNITS, mSizeUnits.name());
        notifyPreferenceUpdated();;
    }
}
