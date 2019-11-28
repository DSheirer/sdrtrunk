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
    public static final int INVALID_ID = -1;
    private Preferences mPreferences = Preferences.userNodeForPackage(RadioReferencePreference.class);
    private static final String STORE_CREDENTIALS = "store.credentials";
    private static final String USER_NAME = "user.name";
    private static final String PASSWORD = "user.authorization";
    private static final String SHOW_PASSWORD = "show.password";
    private static final String PREFERRED_COUNTRY_ID = "preferred.country";
    private static final String PREFERRED_STATE_ID = "preferred.state";
    private static final String PREFERRED_COUNTY_ID = "preferred.county";
    private static final String PREFERRED_SYSTEM_ID = "preferred.system";
    private static final String PREFERRED_AGENCY_ID = "preferred.agency";

    private String mUserName;
    private String mPassword;
    private Boolean mStoreCredentials;
    private Boolean mShowPassword;
    private int mPreferredCountryId = INVALID_ID;
    private int mPreferredStateId = INVALID_ID;
    private int mPreferredCountyId = INVALID_ID;
    private int mPreferredAgencyId = INVALID_ID;
    private int mPreferredSystemId = INVALID_ID;

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
    public void removeStoredCredentials()
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

    /**
     * Indicates if the login dialog should show or mask the user password
     */
    public boolean getShowPassword()
    {
        if(mShowPassword == null)
        {
            mShowPassword = mPreferences.getBoolean(SHOW_PASSWORD, false);
        }

        return mShowPassword;
    }

    /**
     * Sets the show password preference
     */
    public void setShowPassword(boolean show)
    {
        mShowPassword = show;
        mPreferences.putBoolean(SHOW_PASSWORD, show);
    }

    /**
     * Preferred country to use with the service
     */
    public int getPreferredCountryId()
    {
        if(mPreferredCountryId < 0)
        {
            mPreferredCountryId = mPreferences.getInt(PREFERRED_COUNTRY_ID, INVALID_ID);

        }

        return mPreferredCountryId;
    }

    public void setPreferredCountryId(int countryId)
    {
        mPreferredCountryId = countryId;
        mPreferences.putInt(PREFERRED_COUNTRY_ID, countryId);
    }

    /**
     * Preferred state to use with the service
     */
    public int getPreferredStateId()
    {
        if(mPreferredStateId < 0)
        {
            mPreferredStateId = mPreferences.getInt(PREFERRED_STATE_ID, INVALID_ID);
        }

        return mPreferredStateId;
    }

    public void setPreferredStateId(int state)
    {
        mPreferredStateId = state;
        mPreferences.putInt(PREFERRED_STATE_ID, mPreferredStateId);
    }

    /**
     * Preferred county to use with the service
     */
    public int getPreferredCountyId()
    {
        if(mPreferredCountyId < 0)
        {
            mPreferredCountyId = mPreferences.getInt(PREFERRED_COUNTY_ID, INVALID_ID);
        }

        return mPreferredCountyId;
    }

    public void setPreferredCountyId(int county)
    {
        mPreferredCountyId = county;
        mPreferences.putInt(PREFERRED_COUNTY_ID, mPreferredCountyId);
    }

    /**
     * Preferred system to use with the service
     */
    public int getPreferredSystemId()
    {
        if(mPreferredSystemId < 0)
        {
            mPreferredSystemId = mPreferences.getInt(PREFERRED_COUNTY_ID, INVALID_ID);
        }

        return mPreferredSystemId;
    }

    public void setPreferredSystemId(int systemId)
    {
        mPreferredSystemId = systemId;
        mPreferences.putInt(PREFERRED_SYSTEM_ID, mPreferredSystemId);
    }

    /**
     * Preferred agency to use with the service
     */
    public int getPreferredAgencyId()
    {
        if(mPreferredAgencyId < 0)
        {
            mPreferredAgencyId = mPreferences.getInt(PREFERRED_AGENCY_ID, INVALID_ID);
        }

        return mPreferredAgencyId;
    }

    public void setPreferredAgencyId(int agencyId)
    {
        mPreferredAgencyId = agencyId;
        mPreferences.putInt(PREFERRED_AGENCY_ID, mPreferredAgencyId);
    }
}
