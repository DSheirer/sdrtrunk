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

import io.github.dsheirer.gui.playlist.radioreference.Level;
import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.rrapi.type.AuthorizationInformation;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.service.radioreference.RadioReference;
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
    private static final String PREFERRED_SYSTEM_ID_STATE = "preferred.system.state";
    private static final String PREFERRED_SYSTEM_ID_COUNTY = "preferred.system.county";
    private static final String PREFERRED_AGENCY_ID_NATIONAL = "preferred.agency.national";
    private static final String PREFERRED_AGENCY_ID_STATE = "preferred.agency.state";
    private static final String PREFERRED_AGENCY_ID_COUNTY = "preferred.agency.county";
    private static final String SHOW_CHANNEL_EDITOR_NATIONAL = "create.and.show.editor.national";
    private static final String SHOW_CHANNEL_EDITOR_STATE = "create.and.show.editor.state";
    private static final String SHOW_CHANNEL_EDITOR_COUNTY = "create.and.show.editor.county";
    private static final String ENCRYPTED_TALKGROUP_DO_NOT_MONITOR = "encrypted.talkgroup.import.do.not.monitor";
    private static final String CREATE_AND_SHOW_CHANNEL_EDITOR = "create.and.show.channel.editor";

    private String mUserName;
    private String mPassword;
    private Boolean mStoreCredentials;
    private Boolean mShowPassword;
    private Boolean mShowChannelEditorNational;
    private Boolean mShowChannelEditorState;
    private Boolean mShowChannelEditorCounty;
    private Boolean mEncryptedTalkgroupImport;
    private Boolean mCreateAndShowChannelEditor;
    private int mPreferredCountryId = INVALID_ID;
    private int mPreferredStateId = INVALID_ID;
    private int mPreferredCountyId = INVALID_ID;
    private int mPreferredAgencyIdNational = INVALID_ID;
    private int mPreferredAgencyIdState = INVALID_ID;
    private int mPreferredAgencyIdCounty = INVALID_ID;
    private int mPreferredSystemIdState = INVALID_ID;
    private int mPreferredSystemIdCounty = INVALID_ID;

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
     * Creates an authorization information instance with stored login credentials when available.
     * @return information or null if there are no stored credentials.
     */
    public AuthorizationInformation getAuthorizationInformation()
    {
        if(hasStoredCredentials())
        {
            String username = getUserName();
            String password = getPassword();

            return RadioReference.getAuthorizatonInformation(username, password);
        }

        return null;
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
        notifyPreferenceUpdated();
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
        notifyPreferenceUpdated();
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
        notifyPreferenceUpdated();
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
        notifyPreferenceUpdated();
    }

    /**
     * Preferred system to use with the service
     */
    public int getPreferredSystemId(Level level)
    {
        switch(level)
        {
            case STATE:
                if(mPreferredSystemIdState < 0)
                {
                    mPreferredSystemIdState = mPreferences.getInt(PREFERRED_SYSTEM_ID_STATE, INVALID_ID);
                }

                return mPreferredSystemIdState;
            case COUNTY:
            default:
                if(mPreferredSystemIdCounty < 0)
                {
                    mPreferredSystemIdCounty = mPreferences.getInt(PREFERRED_SYSTEM_ID_COUNTY, INVALID_ID);
                }

                return mPreferredSystemIdCounty;
        }
    }

    public void setPreferredSystemId(int systemId, Level level)
    {
        switch(level)
        {
            case STATE:
                mPreferredSystemIdState = systemId;
                mPreferences.putInt(PREFERRED_SYSTEM_ID_STATE, mPreferredSystemIdState);
                notifyPreferenceUpdated();
                break;
            case COUNTY:
            default:
                mPreferredSystemIdCounty = systemId;
                mPreferences.putInt(PREFERRED_SYSTEM_ID_COUNTY, mPreferredSystemIdCounty);
                notifyPreferenceUpdated();
                break;
        }
    }

    /**
     * Preferred country agency to use with the service
     */
    public int getPreferredAgencyId(Level level)
    {
        switch(level)
        {
            case NATIONAL:
                if(mPreferredAgencyIdNational < 0)
                {
                    mPreferredAgencyIdNational = mPreferences.getInt(PREFERRED_AGENCY_ID_NATIONAL, INVALID_ID);
                }

                return mPreferredAgencyIdNational;
            case STATE:
                if(mPreferredAgencyIdState < 0)
                {
                    mPreferredAgencyIdState = mPreferences.getInt(PREFERRED_AGENCY_ID_STATE, INVALID_ID);
                }

                return mPreferredAgencyIdState;
            case COUNTY:
            default:
                if(mPreferredAgencyIdCounty < 0)
                {
                    mPreferredAgencyIdCounty = mPreferences.getInt(PREFERRED_AGENCY_ID_COUNTY, INVALID_ID);
                }

                return mPreferredAgencyIdCounty;
        }
    }

    public void setPreferredAgencyId(int agencyId, Level level)
    {
        switch(level)
        {
            case NATIONAL:
                mPreferredAgencyIdNational = agencyId;
                mPreferences.putInt(PREFERRED_AGENCY_ID_NATIONAL, mPreferredAgencyIdNational);
                notifyPreferenceUpdated();
                break;
            case STATE:
                mPreferredAgencyIdState = agencyId;
                mPreferences.putInt(PREFERRED_AGENCY_ID_STATE, mPreferredAgencyIdState);
                notifyPreferenceUpdated();
                break;
            case COUNTY:
            default:
                mPreferredAgencyIdCounty = agencyId;
                mPreferences.putInt(PREFERRED_AGENCY_ID_COUNTY, mPreferredAgencyIdCounty);
                notifyPreferenceUpdated();
                break;
        }
    }

    /**
     * Preference for showing a channel editor after creating a national agency channel
     */
    public boolean getShowChannelEditor(Level level)
    {
        switch(level)
        {
            case NATIONAL:
                if(mShowChannelEditorNational == null)
                {
                    mShowChannelEditorNational = mPreferences.getBoolean(SHOW_CHANNEL_EDITOR_NATIONAL, true);
                }

                return mShowChannelEditorNational;
            case STATE:
                if(mShowChannelEditorState == null)
                {
                    mShowChannelEditorState = mPreferences.getBoolean(SHOW_CHANNEL_EDITOR_STATE, true);
                }

                return mShowChannelEditorState;
            case COUNTY:
            default:
                if(mShowChannelEditorCounty == null)
                {
                    mShowChannelEditorCounty = mPreferences.getBoolean(SHOW_CHANNEL_EDITOR_COUNTY, true);
                }

                return mShowChannelEditorCounty;
        }
    }

    public void setShowChannelEditor(boolean show, Level level)
    {
        switch(level)
        {
            case NATIONAL:
                mShowChannelEditorNational = show;
                mPreferences.putBoolean(SHOW_CHANNEL_EDITOR_NATIONAL, mShowChannelEditorNational);
                notifyPreferenceUpdated();
                break;
            case STATE:
                mShowChannelEditorState = show;
                mPreferences.putBoolean(SHOW_CHANNEL_EDITOR_STATE, mShowChannelEditorState);
                notifyPreferenceUpdated();
                break;
            case COUNTY:
            default:
                mShowChannelEditorCounty = show;
                mPreferences.putBoolean(SHOW_CHANNEL_EDITOR_COUNTY, mShowChannelEditorCounty);
                notifyPreferenceUpdated();
                break;
        }
    }

    /**
     * Indicates if radio reference encrypted talgkroups should be imported as Do Not Monitor priority.
     * @return true if set to do not monitor
     */
    public boolean isEncryptedTalkgroupDoNotMonitor()
    {
        if(mEncryptedTalkgroupImport == null)
        {
            mEncryptedTalkgroupImport = mPreferences.getBoolean(ENCRYPTED_TALKGROUP_DO_NOT_MONITOR, true);
        }

        return mEncryptedTalkgroupImport;
    }

    /**
     * Set the import encrypted talkgroups as Do Not Monitor
     * @param doNotMonitor true if set to do not monitor
     */
    public void setEncryptedTalkgroupDoNotMonitor(boolean doNotMonitor)
    {
        mEncryptedTalkgroupImport = doNotMonitor;
        mPreferences.putBoolean(ENCRYPTED_TALKGROUP_DO_NOT_MONITOR, doNotMonitor);
        notifyPreferenceUpdated();
    }


    /**
     * Indicates if should show channel editor after trunked channel configuration create
     */
    public boolean isCreateAndShowChannelEditor()
    {
        if(mCreateAndShowChannelEditor == null)
        {
            mCreateAndShowChannelEditor = mPreferences.getBoolean(CREATE_AND_SHOW_CHANNEL_EDITOR, true);
        }

        return mCreateAndShowChannelEditor;
    }

    /**
     * Sets preference to show channel editor after channel configuration create
     */
    public void setCreateAndShowChannelEditor(boolean show)
    {
        mCreateAndShowChannelEditor = show;
        mPreferences.putBoolean(CREATE_AND_SHOW_CHANNEL_EDITOR, show);
        notifyPreferenceUpdated();
    }
}
