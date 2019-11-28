/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.service.radioreference;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.rrapi.RadioReferenceException;
import io.github.dsheirer.rrapi.RadioReferenceService;
import io.github.dsheirer.rrapi.type.AuthorizationInformation;
import io.github.dsheirer.rrapi.type.UserInfo;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.Scanner;

/**
 * Service interface to radioreference.com data API
 */
public class RadioReference
{
    public static final String SDRTRUNK_APP_KEY = "88969092";
    private RadioReferenceService mRadioReferenceService;
    private UserPreferences mUserPreferences;
    private Boolean mStoreCredentials;
    private String mUserName;
    private String mPassword;
    private AuthorizationInformation mAuthorizationInformation;
    private BooleanProperty mLoggedOn = new SimpleBooleanProperty();

    /**
     * Constructs an instance of the radio reference service
     * @param userPreferences for user credentials and other settings
     */
    public RadioReference(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;

        //Register to receive notifications of user preference changes
        MyEventBus.getEventBus().register(this);
    }

    /**
     * Externally monitored and controlled status indicator for the service logged-on state
     */
    public BooleanProperty loggedOnProperty()
    {
        return mLoggedOn;
    }

    /**
     * Provides access to the encapsulated radio reference service.
     *
     * Note: users of this service should call getService() EVERY time they want to make a call to the service.  Any
     * changes to the user credentials after the service has been accessed will cause the service to be recreated.
     *
     * @return service
     * @throws RadioReferenceException if there is an issue creating the service (ie login credentials missing)
     */
    public RadioReferenceService getService() throws RadioReferenceException
    {
        if(mRadioReferenceService == null)
        {
            AuthorizationInformation authorizationInformation = getAuthorizationInformation();

            if(authorizationInformation != null)
            {
                //Use the caching version of the service to cache frequently used values
                mRadioReferenceService = new CachingRadioReferenceService(authorizationInformation);
            }
        }

        if(mRadioReferenceService == null)
        {
            throw new RadioReferenceException("Please set username or password to non-null value");
        }

        return mRadioReferenceService;
    }



    /**
     * Tests the connection to radio reference service using the provided credentials.
     * @param userName for radio reference account
     * @param password for radio reference account
     * @return true if the test is successful
     * @throws RadioReferenceException if there are any issues with the connection
     */
    public static boolean testConnection(String userName, String password) throws RadioReferenceException
    {
        AuthorizationInformation credentials = new AuthorizationInformation(SDRTRUNK_APP_KEY, userName, password);
        RadioReferenceService service = new RadioReferenceService(credentials);
        service.getUserInfo();
        return true;
    }

    /**
     * Creates an authorization information instance with the application key and the provided credentials
     * @param userName to login
     * @param password to login
     * @return an instance
     */
    public static AuthorizationInformation getAuthorizatonInformation(String userName, String password)
    {
        return new AuthorizationInformation(SDRTRUNK_APP_KEY, userName, password);
    }

    /**
     * Authorization Information object for use with radio reference service
     * @return instance or null if the user credentials are not set
     */
    private AuthorizationInformation getAuthorizationInformation()
    {
        if(mAuthorizationInformation == null && hasCredentials())
        {
            mAuthorizationInformation = new AuthorizationInformation(SDRTRUNK_APP_KEY, getUserName(), getPassword());
        }

        return mAuthorizationInformation;
    }

    public void setAuthorizationInformation(AuthorizationInformation authorizationInformation)
    {
        mAuthorizationInformation = authorizationInformation;
        mUserName = authorizationInformation.getUserName();
        mPassword = authorizationInformation.getPassword();

        //Set the service to null so that the next call to getService() will recreate it
        if(mRadioReferenceService != null)
        {
            mRadioReferenceService = null;
        }
    }

    public boolean hasCredentials()
    {
        return getUserName() != null && getPassword() != null;
    }

    /**
     * Sets the preference to store (or not) user credentials.  If this value is false, any stored
     * username or password will be cleared from the user preferences.
     *
     * @param store true if the username and password should be stored in user preferences
     */
    public void setStoreCredentials(boolean store)
    {
        mStoreCredentials = store;

        if(mStoreCredentials)
        {
            mUserPreferences.getRadioReferencePreference().setStoreCredentials(mStoreCredentials);
        }
        else
        {
            mUserPreferences.getRadioReferencePreference().removeStoredCredentials();
        }
    }

    /**
     * Indicates if the user credentials (username and password) should be stored in user preferences
     */
    public boolean getStoreCredentials()
    {
        if(mStoreCredentials == null)
        {
            mStoreCredentials = mUserPreferences.getRadioReferencePreference().isStoreCredentials();
        }

        return mStoreCredentials;
    }

    /**
     * Sets the user name
     * @param userName for login to radio reference service
     */
    public void setUserName(String userName)
    {
        mUserName = userName;

        if(getStoreCredentials())
        {
            mUserPreferences.getRadioReferencePreference().setUserName(mUserName);
        }
    }

    /**
     * User name for radio reference service
     * @return user name
     */
    public String getUserName()
    {
        if(mUserName == null)
        {
            mUserName = mUserPreferences.getRadioReferencePreference().getUserName();
        }

        return mUserName;
    }

    /**
     * Sets the password for radio reference service
     * @param password for service
     */
    public void setPassword(String password)
    {
        mPassword = password;

        if(getStoreCredentials())
        {
            mUserPreferences.getRadioReferencePreference().setPassword(mPassword);
        }
    }

    /**
     * Password for radio reference service
     * @return password
     */
    public String getPassword()
    {
        if(mPassword == null)
        {
            mPassword = mUserPreferences.getRadioReferencePreference().getPassword();
        }

        return mPassword;
    }

    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.RADIO_REFERENCE)
        {
            //Clear all of the locally stored variables so that they are updated upon access from preferences
            mStoreCredentials = null;
            mUserName = null;
            mPassword = null;
            mRadioReferenceService = null;
        }
    }

    public static void main(String[] args)
    {
        RadioReference radioReference = new RadioReference(new UserPreferences());

        if(!radioReference.hasCredentials())
        {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Username: ");
            String username = scanner.next();
            System.out.print("Password: ");
            String password = scanner.next();

            radioReference.setStoreCredentials(true);
            radioReference.setUserName(username);
            radioReference.setPassword(password);
        }

        try
        {
            UserInfo userInfo = radioReference.getService().getUserInfo();
            System.out.println("User Name: " + userInfo.getUserName() + " Account Expires:" + userInfo.getExpirationDate());
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }
}
