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

package io.github.dsheirer.settings.radioreference;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.gui.playlist.radioreference.Level;
import io.github.dsheirer.settings.AbstractSettings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.rrapi.type.AuthorizationInformation;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.service.radioreference.RadioReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Settings for the display of channel decode events
 */
public class RadioReferenceSettings extends AbstractSettings
{
    public static final boolean DEFAULT_SHOW_CHANNEL_EDITOR = true;
    public static final boolean DEFAULT_ENCRYPTED_TALKGROUP_DO_NOT_MONITOR = true;
    public static final AliasListUpdateInterval DEFAULT_ALIAS_LIST_UPDATE_INTERVAL = AliasListUpdateInterval.DAILY;
    public static final boolean DEFAULT_CREATE_AND_SHOW_CHANNEL_EDITOR = true;
    private final static Logger mLog = LoggerFactory.getLogger(RadioReferenceSettings.class);
    public static final int INVALID_ID = -1;

    private String mUserName;
    private String mPassword;
    private Boolean mStoreCredentials;
    private Boolean mShowPassword;
    private Boolean mShowChannelEditorNational;
    private Boolean mShowChannelEditorState;
    private Boolean mShowChannelEditorCounty;
    private Boolean mEncryptedTalkgroupImport;
    private Boolean mCreateAndShowChannelEditor;
    private AliasListUpdateInterval mAliasListUpdateInterval;
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
     * @param updateListener to receive notifications when settings are updated.
     */
    public RadioReferenceSettings(Listener<SettingsType> updateListener)
    {
        super(updateListener);
        loadSettings();
    }

    @Override
    public SettingsType getSettingsType()
    {
        return SettingsType.RADIO_REFERENCE;
    }

    private void loadSettings()
    {
        mStoreCredentials = Setting.RADIO_REFERENCE_STORE_CREDENTIALS.getBoolean(true);
    }

    /**
     * Clears user credentials and resets username and password to null
     */
    public void removeStoredCredentials()
    {
        setStoreCredentials(false);
        setUserName(null);
        setPassword(null);
        notifySettingsUpdated();
    }

    /**
     * Indicates if there are any non-null values stored for user name or password
     * @return true if there are values stored
     */
    public boolean hasStoredCredentials()
    {
        return Setting.RADIO_REFERENCE_USER_NAME.get(null) != null || Setting.RADIO_REFERENCE_PASSWORD.get(null) != null;
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
            mUserName = Setting.RADIO_REFERENCE_USER_NAME.get(null);
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
            Setting.RADIO_REFERENCE_USER_NAME.remove();
        }
        else
        {
            Setting.RADIO_REFERENCE_USER_NAME.put(mUserName);
        }

        notifySettingsUpdated();
    }

    /**
     * Password
     * @return password
     */
    public String getPassword()
    {
        if(mPassword == null)
        {
            mPassword = Setting.RADIO_REFERENCE_PASSWORD.get(null);
        }

        return mPassword;
    }

    /**
     * Sets the settings persisted password.
     * @param password to store or null to remove any stored value
     */
    public void setPassword(String password)
    {
        mPassword = password;

        if(mPassword == null)
        {
            Setting.RADIO_REFERENCE_PASSWORD.remove();
        }
        else
        {
            Setting.RADIO_REFERENCE_PASSWORD.put(mPassword);
        }

        notifySettingsUpdated();
    }

    /**
     * Indicates if the credentials should be persisted in the settings store.
     *
     * @return true if credentials should be stored.
     */
    public boolean isStoreCredentials()
    {
        if(mStoreCredentials == null)
        {
            mStoreCredentials = Setting.RADIO_REFERENCE_STORE_CREDENTIALS.getBoolean(true);
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
        Setting.RADIO_REFERENCE_STORE_CREDENTIALS.putBoolean(store);
        notifySettingsUpdated();
    }

    /**
     * Indicates if the login dialog should show or mask the user password
     */
    public boolean getShowPassword()
    {
        if(mShowPassword == null)
        {
            mShowPassword = Setting.RADIO_REFERENCE_SHOW_PASSWORD.getBoolean(false);
        }

        return mShowPassword;
    }

    /**
     * Sets the show password setting
     */
    public void setShowPassword(boolean show)
    {
        mShowPassword = show;
        Setting.RADIO_REFERENCE_SHOW_PASSWORD.putBoolean(show);
        notifySettingsUpdated();
    }

    /**
     * Preferred country to use with the service
     */
    public int getPreferredCountryId()
    {
        if(mPreferredCountryId < 0)
        {
            mPreferredCountryId = Setting.RADIO_REFERENCE_PREFERRED_COUNTRY.getInt(INVALID_ID);

        }

        return mPreferredCountryId;
    }

    public void setPreferredCountryId(int countryId)
    {
        mPreferredCountryId = countryId;
        Setting.RADIO_REFERENCE_PREFERRED_COUNTRY.putInt(countryId);
        notifySettingsUpdated();
    }

    /**
     * Preferred state to use with the service
     */
    public int getPreferredStateId()
    {
        if(mPreferredStateId < 0)
        {
            mPreferredStateId = Setting.RADIO_REFERENCE_PREFERRED_STATE.getInt(INVALID_ID);
        }

        return mPreferredStateId;
    }

    public void setPreferredStateId(int state)
    {
        mPreferredStateId = state;
        Setting.RADIO_REFERENCE_PREFERRED_STATE.putInt(mPreferredStateId);
        notifySettingsUpdated();
    }

    /**
     * Preferred county to use with the service
     */
    public int getPreferredCountyId()
    {
        if(mPreferredCountyId < 0)
        {
            mPreferredCountyId = Setting.RADIO_REFERENCE_PREFERRED_COUNTY.getInt(INVALID_ID);
        }

        return mPreferredCountyId;
    }

    public void setPreferredCountyId(int county)
    {
        mPreferredCountyId = county;
        Setting.RADIO_REFERENCE_PREFERRED_COUNTY.putInt(mPreferredCountyId);
        notifySettingsUpdated();
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
                    mPreferredSystemIdState = Setting.RADIO_REFERENCE_PREFERRED_SYSTEM_STATE.getInt(INVALID_ID);
                }

                return mPreferredSystemIdState;
            case COUNTY:
            default:
                if(mPreferredSystemIdCounty < 0)
                {
                    mPreferredSystemIdCounty = Setting.RADIO_REFERENCE_PREFERRED_SYSTEM_COUNTY.getInt(INVALID_ID);
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
                Setting.RADIO_REFERENCE_PREFERRED_SYSTEM_STATE.putInt(mPreferredSystemIdState);
                notifySettingsUpdated();
                break;
            case COUNTY:
            default:
                mPreferredSystemIdCounty = systemId;
                Setting.RADIO_REFERENCE_PREFERRED_SYSTEM_COUNTY.putInt(mPreferredSystemIdCounty);
                notifySettingsUpdated();
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
                    mPreferredAgencyIdNational = Setting.RADIO_REFERENCE_PREFERRED_AGENCY_NATIONAL.getInt(INVALID_ID);
                }

                return mPreferredAgencyIdNational;
            case STATE:
                if(mPreferredAgencyIdState < 0)
                {
                    mPreferredAgencyIdState = Setting.RADIO_REFERENCE_PREFERRED_AGENCY_STATE.getInt(INVALID_ID);
                }

                return mPreferredAgencyIdState;
            case COUNTY:
            default:
                if(mPreferredAgencyIdCounty < 0)
                {
                    mPreferredAgencyIdCounty = Setting.RADIO_REFERENCE_PREFERRED_AGENCY_COUNTY.getInt(INVALID_ID);
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
                Setting.RADIO_REFERENCE_PREFERRED_AGENCY_NATIONAL.putInt(mPreferredAgencyIdNational);
                notifySettingsUpdated();
                break;
            case STATE:
                mPreferredAgencyIdState = agencyId;
                Setting.RADIO_REFERENCE_PREFERRED_AGENCY_STATE.putInt(mPreferredAgencyIdState);
                notifySettingsUpdated();
                break;
            case COUNTY:
            default:
                mPreferredAgencyIdCounty = agencyId;
                Setting.RADIO_REFERENCE_PREFERRED_AGENCY_COUNTY.putInt(mPreferredAgencyIdCounty);
                notifySettingsUpdated();
                break;
        }
    }

    /**
     * Setting for showing a channel editor after creating a national agency channel
     */
    public boolean getShowChannelEditor(Level level)
    {
        switch(level)
        {
            case NATIONAL:
                if(mShowChannelEditorNational == null)
                {
                    mShowChannelEditorNational = Setting.RADIO_REFERENCE_SHOW_CHANNEL_EDITOR_NATIONAL.getBoolean(DEFAULT_SHOW_CHANNEL_EDITOR);
                }

                return mShowChannelEditorNational;
            case STATE:
                if(mShowChannelEditorState == null)
                {
                    mShowChannelEditorState = Setting.RADIO_REFERENCE_SHOW_CHANNEL_EDITOR_STATE.getBoolean(DEFAULT_SHOW_CHANNEL_EDITOR);
                }

                return mShowChannelEditorState;
            case COUNTY:
            default:
                if(mShowChannelEditorCounty == null)
                {
                    mShowChannelEditorCounty = Setting.RADIO_REFERENCE_SHOW_CHANNEL_EDITOR_COUNTY.getBoolean(DEFAULT_SHOW_CHANNEL_EDITOR);
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
                Setting.RADIO_REFERENCE_SHOW_CHANNEL_EDITOR_NATIONAL.putBoolean(mShowChannelEditorNational);
                notifySettingsUpdated();
                break;
            case STATE:
                mShowChannelEditorState = show;
                Setting.RADIO_REFERENCE_SHOW_CHANNEL_EDITOR_STATE.putBoolean(mShowChannelEditorState);
                notifySettingsUpdated();
                break;
            case COUNTY:
            default:
                mShowChannelEditorCounty = show;
                Setting.RADIO_REFERENCE_SHOW_CHANNEL_EDITOR_COUNTY.putBoolean(mShowChannelEditorCounty);
                notifySettingsUpdated();
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
            mEncryptedTalkgroupImport = Setting.RADIO_REFERENCE_ENCRYPTED_TALKGROUP_DO_NOT_MONITOR.getBoolean(DEFAULT_ENCRYPTED_TALKGROUP_DO_NOT_MONITOR);
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
        Setting.RADIO_REFERENCE_ENCRYPTED_TALKGROUP_DO_NOT_MONITOR.putBoolean(doNotMonitor);
        notifySettingsUpdated();
    }


    /**
     * How often alias lists that track a Radio Reference system check the system for changes.
     */
    public AliasListUpdateInterval getAliasListUpdateInterval()
    {
        if(mAliasListUpdateInterval == null)
        {
            String name = Setting.RADIO_REFERENCE_ALIAS_LIST_UPDATE_INTERVAL.get(DEFAULT_ALIAS_LIST_UPDATE_INTERVAL.name());

            try
            {
                mAliasListUpdateInterval = AliasListUpdateInterval.valueOf(name);
            }
            catch(IllegalArgumentException iae)
            {
                mAliasListUpdateInterval = AliasListUpdateInterval.DAILY;
            }
        }

        return mAliasListUpdateInterval;
    }

    /**
     * Sets how often alias lists that track a Radio Reference system check the system for changes.
     */
    public void setAliasListUpdateInterval(AliasListUpdateInterval interval)
    {
        if(interval != null)
        {
            mAliasListUpdateInterval = interval;
            Setting.RADIO_REFERENCE_ALIAS_LIST_UPDATE_INTERVAL.put(interval.name());
            notifySettingsUpdated();
        }
    }

    /**
     * Indicates if should show channel editor after trunked channel configuration create
     */
    public boolean isCreateAndShowChannelEditor()
    {
        if(mCreateAndShowChannelEditor == null)
        {
            mCreateAndShowChannelEditor = Setting.RADIO_REFERENCE_CREATE_AND_SHOW_CHANNEL_EDITOR.getBoolean(DEFAULT_CREATE_AND_SHOW_CHANNEL_EDITOR);
        }

        return mCreateAndShowChannelEditor;
    }

    /**
     * Sets setting to show channel editor after channel configuration create
     */
    public void setCreateAndShowChannelEditor(boolean show)
    {
        mCreateAndShowChannelEditor = show;
        Setting.RADIO_REFERENCE_CREATE_AND_SHOW_CHANNEL_EDITOR.putBoolean(show);
        notifySettingsUpdated();
    }

    /**
     * User's location, for ranking radio reference results by distance.
     * @param latitude in decimal degrees, -90 to 90
     * @param longitude in decimal degrees, -180 to 180
     * @param label describing the location, e.g. the city and zip code that it was looked up from, or null when the
     * coordinates were entered by hand
     */
    public record Location(double latitude, double longitude, String label)
    {
        /**
         * Constructs an instance
         * @throws IllegalArgumentException if the coordinates are out of range
         */
        public Location
        {
            if(!isValid(latitude, longitude))
            {
                throw new IllegalArgumentException("Invalid location: " + latitude + ", " + longitude);
            }

            label = label == null || label.isBlank() ? null : label.trim();
        }

        /**
         * Indicates if the coordinates are within range
         * @param latitude in decimal degrees
         * @param longitude in decimal degrees
         * @return true if valid
         */
        public static boolean isValid(double latitude, double longitude)
        {
            return Double.isFinite(latitude) && Double.isFinite(longitude) && Math.abs(latitude) <= 90.0 &&
                Math.abs(longitude) <= 180.0;
        }

        /**
         * Coordinates for display
         * @return text such as "43.0481, -76.1474"
         */
        public String coordinates()
        {
            return String.format(Locale.ROOT, "%.4f, %.4f", latitude, longitude);
        }

        /**
         * Location for display
         * @return text such as "Syracuse, New York 13202 (43.0481, -76.1474)", or the coordinates when there's no label
         */
        public String describe()
        {
            return label != null ? label + " (" + coordinates() + ")" : coordinates();
        }
    }

    /**
     * User's location, for ranking radio reference results by distance.  Can be called from any thread.
     * @return location, or null if the user hasn't set one
     */
    public Location getLocation()
    {
        String value = Setting.RADIO_REFERENCE_LOCATION.get(null);

        if(value != null)
        {
            String[] parts = value.split(",");

            if(parts.length == 2)
            {
                try
                {
                    return new Location(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
                        Setting.RADIO_REFERENCE_LOCATION_LABEL.get(null));
                }
                catch(IllegalArgumentException e)
                {
                    mLog.warn("Ignoring invalid stored radio reference location [" + value + "]");
                }
            }
        }

        return null;
    }

    /**
     * Sets the user's location
     * @param location to store, or null to clear the location
     */
    public void setLocation(Location location)
    {
        if(location != null)
        {
            //Stored as one value so that a reader never sees the latitude of one location with the longitude of another
            Setting.RADIO_REFERENCE_LOCATION.put(location.latitude() + "," + location.longitude());

            if(location.label() != null)
            {
                Setting.RADIO_REFERENCE_LOCATION_LABEL.put(location.label());
            }
            else
            {
                Setting.RADIO_REFERENCE_LOCATION_LABEL.remove();
            }
        }
        else
        {
            Setting.RADIO_REFERENCE_LOCATION.remove();
            Setting.RADIO_REFERENCE_LOCATION_LABEL.remove();
        }

        notifySettingsUpdated();
    }
}
