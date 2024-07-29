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

package io.github.dsheirer.service.radioreference;

import com.google.common.annotations.VisibleForTesting;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.rrapi.RadioReferenceException;
import io.github.dsheirer.rrapi.RadioReferenceService;
import io.github.dsheirer.rrapi.response.Fault;
import io.github.dsheirer.rrapi.type.AuthorizationInformation;
import io.github.dsheirer.rrapi.type.UserInfo;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service interface to radioreference.com data API with caching for Flavor, Mode, Type and Tag values.
 */
public class RadioReference
{
    private static final Logger mLog = LoggerFactory.getLogger(RadioReference.class);

    public static final String SDRTRUNK_APP_KEY = "88969092";
    private RadioReferenceService mRadioReferenceService;
    private UserPreferences mUserPreferences;
    private AuthorizationInformation mAuthorizationInformation;
    private StringProperty mUserName = new SimpleStringProperty();
    private StringProperty mPassword = new SimpleStringProperty();
    private StringProperty mAccountExpiresProperty = new SimpleStringProperty();
    private BooleanProperty mAvailable = new SimpleBooleanProperty();
    private BooleanProperty mPremiumAccount = new SimpleBooleanProperty();
    private LoginStatus mLoginStatus = LoginStatus.UNKNOWN;

    /**
     * Enum to determine "validity" of a RR Login
     */
    public enum LoginStatus
    {
        UNKNOWN,
        INVALID_LOGIN,
        EXPIRED_PREMIUM,
        VALID_PREMIUM;
    }

    /**
     * Constructs an instance of the radio reference service
     * @param userPreferences for user credentials and other settings
     */
    public RadioReference(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
    }

    /**
     * Externally monitored and controlled status indicator for the service logged-on state
     */
    public BooleanProperty availableProperty()
    {
        return mAvailable;
    }

    /**
     * Externally monitored and controlled status indicator for a Premium RR Account
     */
    public BooleanProperty premiumAccountProperty()
    {
        return mPremiumAccount;
    }

    /**
     * Current login state / validity
     */
    public LoginStatus getLoginStatus()
    {
        return mLoginStatus;
    }

    /**
     * User name for radio reference account
     */
    public StringProperty userNameProperty()
    {
        return mUserName;
    }

    /**
     * User name for radio reference account
     */
    public StringProperty passwordProperty()
    {
        return mPassword;
    }

    /**
     * Read only user name for radio reference account
     */
    public StringProperty accountExpiresProperty()
    {
        return mAccountExpiresProperty;
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
     * Tests the connection to radio reference service using the provided credentials.
     * @param userName for radio reference account
     * @param password for radio reference account
     * @return The state and "validity" of the account
     * @throws RadioReferenceException if there are any issues with the connection
     */
    public static LoginStatus testConnectionWithExp(String userName, String password) throws  RadioReferenceException
    {

        AuthorizationInformation credentials = new AuthorizationInformation(SDRTRUNK_APP_KEY, userName, password);

        try
        {
            RadioReferenceService service = new RadioReferenceService(credentials);
            UserInfo ui = service.getUserInfo();

            if(ui == null)
            {
                throw new RadioReferenceException("The Radio Reference service is not providing user account details");
            }

            mLog.info("Radio Reference Test Connection Result - user [" + userName + "] expiration [" + ui.getExpirationDate() + "]");

            return CheckExpDate(ui.getExpirationDate());
        }
        catch (RadioReferenceException rre)
        {
            mLog.error("Radio Reference Test Connection Failed - user [" + userName + "] error [" + rre.getMessage() + "]");

            if (rre.hasFault())
            {
                Fault fault = rre.getFault();

                if (fault.getFaultCode() != null && fault.getFaultCode().contentEquals("AUTH"))
                {
                    return LoginStatus.INVALID_LOGIN;
                }
                else
                {
                    throw rre;
                }
            }
            else
            {
                throw rre;
            }
        }
    }

    /**
     * Determines the account status (Premium) based on the given expiration date
     * @param RRExpirationString The Premium Expiration string returned from RR
     * @return Account Login Status
     */
    @VisibleForTesting
    protected static LoginStatus CheckExpDate(String RRExpirationString)
    {
        try
        {
            SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy", Locale.ENGLISH);
            Date RRExpDate = formatter.parse(RRExpirationString);

            // We want to err on the side of the user getting an error with an invalid subscription,
            // rather than the user being prevented from using their valid, but shortly expiring, subscription.

            // Get the current Date and subtract two days (due to no >=, only > ),
            // as I am not sure how timezones work for RR and the end user.

            Calendar c = Calendar.getInstance();
            c.setTime(new Date());
            c.add(Calendar.DAY_OF_MONTH, -2);

            if(RRExpDate.after(c.getTime()))
            {
                return LoginStatus.VALID_PREMIUM;
            }
            else
            {
                return LoginStatus.EXPIRED_PREMIUM;
            }
        }
        catch (ParseException e)
        {
            // This doesn't have to be a date; it can also be a string
            // If we get anything we can't parse, assume it's a string and valid
            // Accounts that have never had a valid subscription return a year of 1969

            // See https://wiki.radioreference.com/index.php/RadioReference.com_Web_Service3.1#Versions :
            // Version 5 - Issued a fix for the getUserData method which returned a subscription expiration
            // date in 1969 for admins and feed providers. Now returns "Never - Feed Provider" and "Never - Admin"
            // in string format.
            return LoginStatus.VALID_PREMIUM;
        }
    }

    /**
     * Login with credentials, and update properties based on the login status
     */
    private void login()
    {
        try
        {
            UserInfo userInfo = getService().getUserInfo();

            if(userInfo != null)
            {
                accountExpiresProperty().setValue(userInfo.getExpirationDate());
                mLoginStatus = CheckExpDate(userInfo.getExpirationDate());
            }
            else
            {
                accountExpiresProperty().setValue(null);
                mLoginStatus = LoginStatus.UNKNOWN;
            }

            if(mLoginStatus == LoginStatus.VALID_PREMIUM)
            {
                availableProperty().set(true);
                premiumAccountProperty().set(true);
            }
            else if (mLoginStatus == LoginStatus.EXPIRED_PREMIUM)
            {
                availableProperty().set(true);
                premiumAccountProperty().set(false);
            }
            else
            {
                availableProperty().set(false);
                premiumAccountProperty().set(false);
            }

            if(userInfo != null)
            {
                mLog.info("Radio Reference Account - User:" + userInfo.getUserName() +
                        " Expires:" + userInfo.getExpirationDate() +
                        " Login Status:" + mLoginStatus +
                        " Premium Access Enabled:" + premiumAccountProperty().get());
            }
            else
            {
                mLog.info("Radio Reference API did not provide a response from getUserData method call - premium access is disabled");
            }
        }
        catch(RadioReferenceException rre)
        {
            accountExpiresProperty().setValue(null);
            mLoginStatus = LoginStatus.UNKNOWN;
            availableProperty().set(false);
            premiumAccountProperty().set(false);
            mLog.error("Error checking radio reference premium account status.  Disabling premium access.  Error: " + rre.getMessage());
        }
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
        return mAuthorizationInformation;
    }

    /**
     * Applies the credentials and attempts to login to the radio reference service
     * @param authorizationInformation
     */
    public void setAuthorizationInformation(AuthorizationInformation authorizationInformation)
    {
        mAuthorizationInformation = authorizationInformation;
        mUserName.setValue(authorizationInformation.getUserName());
        mPassword.setValue(authorizationInformation.getPassword());

        //Set the service to null so that the next call to getService() will recreate it
        if(mRadioReferenceService != null)
        {
            mRadioReferenceService = null;
        }

        login();
    }

    public static void main(String[] args)
    {
        UserPreferences userPreferences = new UserPreferences();
        RadioReference radioReference = new RadioReference(userPreferences);

        AuthorizationInformation credentials = userPreferences.getRadioReferencePreference().getAuthorizationInformation();

        if(credentials == null)
        {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Username: ");
            String username = scanner.next();
            System.out.print("Password: ");
            String password = scanner.next();
            credentials = getAuthorizatonInformation(username, password);
        }

        radioReference.setAuthorizationInformation(credentials);

        if(radioReference.availableProperty().get())
        {
            try
            {
                UserInfo userInfo = radioReference.getService().getUserInfo();
                System.out.println("User Name: " + userInfo.getUserName() + " Account Expires:" + userInfo.getExpirationDate());
            }
            catch(RadioReferenceException rre)
            {
                mLog.error("Error", rre);
            }
        }
    }
}
