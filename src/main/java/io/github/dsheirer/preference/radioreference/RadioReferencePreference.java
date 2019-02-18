/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.preference.radioreference;

import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

/**
 * User preferences for the display of channel decode events
 */
public class RadioReferencePreference extends Preference
{
    private final static Logger mLog = LoggerFactory.getLogger(RadioReferencePreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(RadioReferencePreference.class);
    private static final String STORE_CREDENTIALS = "store.credentials";
    private static final String USER_NAME = "user.name";
    private static final String PASSWORD = "user.authorization";

    private String mUserName;
    private String mPassword;
    private Boolean mStoreCredentials;

    /**
     * Constructs an instance.
     * @param updateListener to receive notifications when preferences are updated.
     */
    public RadioReferencePreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
        loadSettings();
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.RADIO_REFERENCE;
    }

    private void loadSettings()
    {
        mStoreCredentials = mPreferences.getBoolean(STORE_CREDENTIALS, true);
    }

    /**
     * Clears user credentials and resets username and password to null
     */
    public void removeStoreCredentials()
    {
        setStoreCredentials(false);
        setUserName(null);
        setPassword(null);
        notifyPreferenceUpdated();
    }

    /**
     * Indicates if there are any non-null values stored for user name or password
     * @return true if there are values stored
     */
    public boolean hasStoredCredentials()
    {
        return mPreferences.get(USER_NAME, null) != null || mPreferences.get(PASSWORD, null) != null;
    }

    /**
     * User name
     */
    public String getUserName()
    {
        if(mUserName == null)
        {
            mUserName = mPreferences.get(USER_NAME, null);
        }

        return mUserName;
    }

    /**
     * Sets user name
     * @param username to store or null to remove any stored value
     */
    public void setUserName(String username)
    {
        mUserName = username;

        if(mUserName == null)
        {
            mPreferences.remove(USER_NAME);
        }
        else
        {
            mPreferences.put(USER_NAME, mUserName);
        }

        notifyPreferenceUpdated();
    }

    /**
     * Password
     * @return password
     */
    public String getPassword()
    {
        if(mPassword == null)
        {
            mPassword = mPreferences.get(PASSWORD, null);
        }

        return mPassword;
    }

    /**
     * Sets the preferences persisted password.
     * @param password to store or null to remove any stored value
     */
    public void setPassword(String password)
    {
        mPassword = password;

        if(mPassword == null)
        {
            mPreferences.remove(PASSWORD);
        }
        else
        {
            mPreferences.put(PASSWORD, mPassword);
        }

        notifyPreferenceUpdated();
    }

    /**
     * Indicates if the credentials should be persisted in the preferences store.
     *
     * @return true if credentials should be stored.
     */
    public boolean isStoreCredentials()
    {
        if(mStoreCredentials == null)
        {
            mStoreCredentials = mPreferences.getBoolean(STORE_CREDENTIALS, true);
        }

        return mStoreCredentials;
    }

    /**
     * Set the store credentials option
     * @param store true to store credentials
     */
    public void setStoreCredentials(boolean store)
    {
        mStoreCredentials = store;
        mPreferences.putBoolean(STORE_CREDENTIALS, store);
        notifyPreferenceUpdated();
    }
}
