/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
 *
 ******************************************************************************/
package external.radioreference;

import com.radioreference.api.soap2.AuthInfo;
import com.radioreference.api.soap2.RRWsdlLocator;
import com.radioreference.api.soap2.RRWsdlPortType;
import com.radioreference.api.soap2.UserFeedBroadcast;
import com.radioreference.api.soap2.UserInfo;
import org.apache.axis.AxisFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RadioReferenceService
{
    private final static Logger mLog = LoggerFactory.getLogger( RadioReferenceService.class );
    private final SimpleDateFormat SDF = new SimpleDateFormat("MM-dd-yyyy");

    public static final String SDRTRUNK_APP_KEY = "88969092";
    public static final String RADIO_REFERENCE_API_VERSION = "14";
    public static final String SOAP_RPC_RESPONSE_STYLE = "rpc";

    private RRWsdlLocator mService = new RRWsdlLocator();
    private RRWsdlPortType mClient;
    private Long mAccountExpiration;

    private AuthInfo mUserAuthorization;

    /**
     * Creates a SOAP web service client for interaction with the radioreference.com server.
     *
     * @param username for a premium subscriber
     * @param password for a premium subscriber
     * @throws ServiceException if the client can't be created
     * @throws IllegalArgumentException if either the username or password is null or empty
     */
    public RadioReferenceService(String username, String password) throws ServiceException
    {
        mClient = mService.getRRWsdlPort();

        setAuthorizationInformation(username, password);
    }

    /**
     * Sets new premium subscriber user credentials to authenticate to the server.
     * @param username for a premium subscriber on radioreference.com
     * @param password for a premium subscriber
     */
    public void setAuthorizationInformation(String username, String password) throws IllegalArgumentException
    {
        if(username == null || username.isEmpty() || password == null || password.isEmpty())
        {
            throw new IllegalArgumentException("Username [" + username + "] and/or password [" + password +
                "] cannot be null or empty");
        }

        mUserAuthorization = new AuthInfo(username, password, SDRTRUNK_APP_KEY, RADIO_REFERENCE_API_VERSION,
            SOAP_RPC_RESPONSE_STYLE);
    }

    /**
     * Indicates if the service has a non-null user authorization
     */
    public void checkCredentials() throws IllegalArgumentException
    {
        if(mUserAuthorization == null)
        {
            throw new IllegalStateException("Authorization information (username/password) required");
        }
    }

    /**
     * Account expiration date
     *
     * @return expiration date in millis since epoch
     * @throws AxisFault for remote server errors
     * @throws IllegalStateException for null or empty login credentials
     * @throws RemoteException for web service client errors
     */
    public long getAccountExpirationDate() throws IllegalStateException, RemoteException
    {
        checkCredentials();

        if(mAccountExpiration == null)
        {
            UserInfo userInfo = mClient.getUserData(mUserAuthorization);

            if(userInfo != null)
            {
                String expiration = userInfo.getSubExpireDate();

                if(expiration != null)
                {
                    try
                    {
                        mAccountExpiration = SDF.parse(expiration).getTime();
                    }
                    catch(Exception e)
                    {
                        mLog.error("Couldn't parse account expiration date from web service response: " + expiration, e);
                    }
                }
            }
        }

        return mAccountExpiration;
    }

    /**
     * Indicates if the radio reference user account is active (ie not expired)
     *
     * @throws IllegalStateException for null or empty login credentials
     * @throws RemoteException for web service client errors
     */
    public boolean isAccountActive() throws IllegalStateException, RemoteException
    {
        Long expiration = getAccountExpirationDate();

        if(expiration != null)
        {
            return System.currentTimeMillis() < expiration;
        }

        return false;
    }

    public UserFeedBroadcast[] getUserFeedBroadcasts() throws IllegalArgumentException, RemoteException
    {
        checkCredentials();

        return mClient.getUserFeedBroadcasts(mUserAuthorization);
    }


    public static void main(String[] args)
    {
        try
        {
            RadioReferenceService service = new RadioReferenceService("dsheirer", "1dodgeram");

            Long expiration = service.getAccountExpirationDate();

            mLog.debug("Expiration Date:" + (expiration != null ? new Date(expiration).toString() : "unknown"));

            mLog.debug("Is Active: " + service.isAccountActive());

            UserFeedBroadcast[] feeds = service.getUserFeedBroadcasts();

            for(UserFeedBroadcast feed: feeds)
            {
                mLog.debug("Feed ID: " + feed.getFeedId() + " Desc:" + feed.getDescr());
            }
        }
        catch(AxisFault af)
        {
            mLog.debug("Hey, I caught an axis fault!");
            mLog.debug("Reason:" + af.getFaultReason());
        }
        catch(RemoteException re)
        {
            re.printStackTrace();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        mLog.debug("Finished!");
    }
}
