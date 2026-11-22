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

package io.github.dsheirer.service.radioreference;

import com.google.common.annotations.VisibleForTesting;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.radioreference.RadioReferencePreference;
import io.github.dsheirer.rrapi.RadioReferenceException;
import io.github.dsheirer.rrapi.RadioReferenceService;
import io.github.dsheirer.rrapi.type.AuthorizationInformation;
import io.github.dsheirer.rrapi.type.CountryInfo;
import io.github.dsheirer.rrapi.type.County;
import io.github.dsheirer.rrapi.type.CountyInfo;
import io.github.dsheirer.rrapi.type.Flavor;
import io.github.dsheirer.rrapi.type.State;
import io.github.dsheirer.rrapi.type.StateInfo;
import io.github.dsheirer.rrapi.type.UserInfo;
import io.github.dsheirer.rrapi.type.ZipInfo;
import io.github.dsheirer.util.ThreadPool;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import javafx.application.Platform;
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
    private final AtomicLong mLoginRequest = new AtomicLong();
    private final RadioReferenceFrequencyIndex mFrequencyIndex;
    private StringProperty mUserName = new SimpleStringProperty();
    private StringProperty mPassword = new SimpleStringProperty();
    private StringProperty mAccountExpiresProperty = new SimpleStringProperty();
    private BooleanProperty mAvailable = new SimpleBooleanProperty();
    private BooleanProperty mPremiumAccount = new SimpleBooleanProperty();
    private volatile LoginStatus mLoginStatus = LoginStatus.UNKNOWN;

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
        mFrequencyIndex = new RadioReferenceFrequencyIndex(this, userPreferences);
    }

    /**
     * User preferences, for the location, credentials and talkgroup formats
     */
    public UserPreferences getUserPreferences()
    {
        return mUserPreferences;
    }

    /**
     * Index of the radio reference frequencies near the user's location.  Idle until started.
     */
    public RadioReferenceFrequencyIndex getFrequencyIndex()
    {
        return mFrequencyIndex;
    }

    /**
     * Logs in with the credentials stored in the user preferences, if there are any and no login has been made yet.
     * Logs in on a background thread and updates the properties on the FX thread, so the JavaFX toolkit must be
     * running.
     */
    public void loginWithStoredCredentials()
    {
        RadioReferencePreference preference = mUserPreferences.getRadioReferencePreference();

        if(mLoginRequest.get() == 0 && preference.hasStoredCredentials())
        {
            AuthorizationInformation credentials = preference.getAuthorizationInformation();

            if(credentials != null)
            {
                setAuthorizationInformation(credentials);
            }
        }
    }

    /**
     * Externally monitored and controlled status indicator for the service logged-on state.  Updated on the FX thread.
     */
    public BooleanProperty availableProperty()
    {
        return mAvailable;
    }

    /**
     * Externally monitored and controlled status indicator for a Premium RR Account.  Updated on the FX thread.
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
    public synchronized RadioReferenceService getService() throws RadioReferenceException
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
     * Location to rank radio reference results by distance from the user: the location set in the preferences, or
     * else the center of the preferred county.  Calls the web service for the preferred county, so don't call this
     * on the FX thread.
     * @return location, or null if the user hasn't set a location and there's no preferred county with a location, or
     * the account can't access the service
     * @throws RadioReferenceException if the preferred county lookup fails
     */
    public UserLocation getUserLocation() throws RadioReferenceException
    {
        RadioReferencePreference preference = mUserPreferences.getRadioReferencePreference();
        RadioReferencePreference.Location location = preference.getLocation();

        if(location != null)
        {
            return new UserLocation(location.latitude(), location.longitude(), location.label(),
                UserLocation.Source.USER);
        }

        int countyId = preference.getPreferredCountyId();

        if(countyId > 0 && premiumAccountProperty().get())
        {
            CountyInfo countyInfo = getService().getCountyInfo(countyId);

            if(countyInfo != null && countyInfo.getCoverage().hasLocation())
            {
                return new UserLocation(countyInfo.getLatitude(), countyInfo.getLongitude(), countyInfo.getName(),
                    UserLocation.Source.PREFERRED_COUNTY);
            }
        }

        return null;
    }

    /**
     * Looks up the location of a United States zip code
     * @param zipcode five digit zip code
     * @return location with a label naming the city, state and zip code
     * @throws RadioReferenceException if the lookup fails or the service has no location for the zip code
     */
    public RadioReferencePreference.Location lookupZipcode(int zipcode) throws RadioReferenceException
    {
        RadioReferenceService service = getService();
        ZipInfo zipInfo = service.getZipcodeInfo(zipcode);
        String zip = String.format("%05d", zipcode);

        if(zipInfo != null)
        {
            try
            {
                double latitude = Double.parseDouble(zipInfo.getLatitude().trim());
                double longitude = Double.parseDouble(zipInfo.getLongitude().trim());

                //The service returns zeros for some unknown zip codes
                if(RadioReferencePreference.Location.isValid(latitude, longitude) && (latitude != 0.0 || longitude != 0.0))
                {
                    String state = null;

                    try
                    {
                        StateInfo stateInfo = service.getStateInfo(zipInfo.getStateId());
                        state = stateInfo != null ? stateInfo.getName() : null;
                    }
                    catch(RadioReferenceException rre)
                    {
                        //The state name is only for the label
                        mLog.debug("Unable to look up state [" + zipInfo.getStateId() + "] for zip code " + zip);
                    }

                    StringBuilder label = new StringBuilder();

                    if(zipInfo.getCity() != null && !zipInfo.getCity().isBlank())
                    {
                        label.append(zipInfo.getCity().trim()).append(state != null ? ", " : " ");
                    }

                    if(state != null)
                    {
                        label.append(state).append(" ");
                    }

                    label.append(zip);
                    return new RadioReferencePreference.Location(latitude, longitude, label.toString());
                }
            }
            catch(NullPointerException | NumberFormatException e)
            {
                //Fall through - no usable coordinates
            }
        }

        throw new RadioReferenceException(RadioReferenceException.ErrorType.INVALID_REQUEST,
            "Radio Reference has no location for zip code " + zip);
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

            if(rre.getErrorType() == RadioReferenceException.ErrorType.AUTHENTICATION)
            {
                return LoginStatus.INVALID_LOGIN;
            }

            throw rre;
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
     * Logs in with the current credentials on the calling thread, then updates the login status and the properties on
     * the FX thread, unless a newer login request has started in the meantime.
     * @param request number of this login request
     * @param result to complete with the login status once the properties are updated
     */
    private void login(long request, CompletableFuture<LoginStatus> result)
    {
        UserInfo userInfo = null;
        LoginStatus loginStatus;

        try
        {
            userInfo = getService().getUserInfo();
            loginStatus = userInfo != null ? CheckExpDate(userInfo.getExpirationDate()) : LoginStatus.UNKNOWN;

            if(userInfo != null)
            {
                mLog.info("Radio Reference Account - User:" + userInfo.getUserName() +
                        " Expires:" + userInfo.getExpirationDate() +
                        " Login Status:" + loginStatus +
                        " Premium Access Enabled:" + (loginStatus == LoginStatus.VALID_PREMIUM));
            }
            else
            {
                mLog.info("Radio Reference API did not provide a response from getUserData method call - premium access is disabled");
            }
        }
        catch(RadioReferenceException rre)
        {
            loginStatus = rre.getErrorType() == RadioReferenceException.ErrorType.AUTHENTICATION ?
                LoginStatus.INVALID_LOGIN : LoginStatus.UNKNOWN;
            mLog.error("Error checking radio reference premium account status.  Disabling premium access.  Error: " + rre.getMessage());
        }
        catch(RuntimeException re)
        {
            //Always update the properties and complete the result, whatever the error
            loginStatus = LoginStatus.UNKNOWN;
            mLog.error("Error checking radio reference premium account status.  Disabling premium access.", re);
        }

        final String expirationDate = userInfo != null ? userInfo.getExpirationDate() : null;
        final LoginStatus status = loginStatus;

        Platform.runLater(() -> {
            //A newer login request supersedes this one
            if(mLoginRequest.get() == request)
            {
                mLoginStatus = status;
                accountExpiresProperty().setValue(expirationDate);
                availableProperty().set(status == LoginStatus.VALID_PREMIUM || status == LoginStatus.EXPIRED_PREMIUM);
                premiumAccountProperty().set(status == LoginStatus.VALID_PREMIUM);
            }

            result.complete(status);
        });
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
    private synchronized AuthorizationInformation getAuthorizationInformation()
    {
        return mAuthorizationInformation;
    }

    /**
     * Applies the credentials and logs in to the radio reference service in the background.  The properties are
     * updated on the FX thread, so the JavaFX toolkit must be running.  Can be called from any thread.
     * @param authorizationInformation credentials
     * @return future that completes with the login status, on the FX thread, once the properties are updated
     */
    public CompletableFuture<LoginStatus> setAuthorizationInformation(AuthorizationInformation authorizationInformation)
    {
        synchronized(this)
        {
            mAuthorizationInformation = authorizationInformation;
            //Set the service to null so that the next call to getService() will recreate it with the new credentials
            mRadioReferenceService = null;
        }

        long request = mLoginRequest.incrementAndGet();
        CompletableFuture<LoginStatus> result = new CompletableFuture<>();

        Platform.runLater(() -> {
            mUserName.setValue(authorizationInformation.getUserName());
            mPassword.setValue(authorizationInformation.getPassword());
        });

        ThreadPool.CACHED.execute(() -> login(request, result));
        return result;
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

        //Login updates the properties on the FX thread
        Platform.startup(() -> {});
        radioReference.setAuthorizationInformation(credentials).join();

        if(radioReference.availableProperty().get())
        {
            try
            {
                UserInfo userInfo = radioReference.getService().getUserInfo();
                System.out.println("User Name: " + userInfo.getUserName() + " Account Expires:" + userInfo.getExpirationDate());
                RadioReferenceService service = radioReference.getService();

                CountryInfo usInfo = service.getCountryInfo(1);
                for(State state : usInfo.getStates())
                {
                    StateInfo stateInfo = service.getStateInfo(state.getStateId());

                    System.out.println("State: " + stateInfo.getName());

                    for(io.github.dsheirer.rrapi.type.System system: stateInfo.getSystems())
                    {
                        int flavor = system.getFlavorId();

                        if(flavor == 36 || flavor == 44 || flavor == 47)
                        {
                            Flavor f = service.getFlavor(flavor);
                            System.out.println("\t" + system.getName() + " Flavor: " + f.getName());
                        }
                    }

                    List<County> counties = stateInfo.getCounties();
                    for(County county : counties)
                    {
                        CountyInfo countyInfo = service.getCountyInfo(county.getCountyId());

                        for(io.github.dsheirer.rrapi.type.System system: countyInfo.getSystems())
                        {
                            int flavor = system.getFlavorId();

                            if(flavor == 36 || flavor == 44 || flavor == 47)
                            {
                                Flavor f = service.getFlavor(flavor);
                                System.out.println("\tCounty: " + county.getName() + " System: " + system.getName() + " Flavor: " + f.getName());
                            }
                        }
                    }
                }
            }
            catch(RadioReferenceException rre)
            {
                mLog.error("Error", rre);
            }
        }

        Platform.exit();
    }
}
