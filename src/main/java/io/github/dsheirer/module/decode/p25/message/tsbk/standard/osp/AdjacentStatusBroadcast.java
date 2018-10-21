package io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25Channel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.node.APCO25Lra;
import io.github.dsheirer.identifier.integer.node.APCO25Rfss;
import io.github.dsheirer.identifier.integer.node.APCO25Site;
import io.github.dsheirer.identifier.integer.node.APCO25System;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.tsbk.OSPMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Adjacent status broadcast - neighbor sites
 */
public class AdjacentStatusBroadcast extends OSPMessage implements FrequencyBandReceiver
{
    private static final int[] LOCATION_REGISTRATION_AREA = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int CONVENTIONAL_CHANNEL_FLAG = 24;
    private static final int SITE_FAILURE_FLAG = 25;
    private static final int VALID_INFORMATION_FLAG = 26;
    private static final int ACTIVE_NETWORK_CONNECTION_TO_RFSS_CONTROLLER_FLAG = 27;
    private static final int[] SYSTEM = {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] RFSS = {40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] SITE = {48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] FREQUENCY_BAND = {56, 57, 58, 59};
    private static final int[] CHANNEL_NUMBER = {60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] SYSTEM_SERVICE_CLASS = {72, 73, 74, 75, 76, 77, 78, 79};

    private IIdentifier mLocationRegistrationArea;
    private IIdentifier mSystem;
    private IIdentifier mSite;
    private IIdentifier mRfss;
    private IAPCO25Channel mChannel;
    private ServiceOptions mServiceOptions;
    private List<IIdentifier> mIdentifiers;
    private List<String> mSiteFlags;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public AdjacentStatusBroadcast(DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" LRA:").append(getLocationRegistrationArea());
        sb.append(" SYSTEM:").append(getSystem());
        sb.append(" RFSS:").append(getRfss());
        sb.append(" SITE:").append(getSite());
        sb.append(" FLAGS ").append(getSiteFlags());
        sb.append(" SERVICES ").append(getServiceOptions());
        return sb.toString();
    }

    public List<String> getSiteFlags()
    {
        if(mSiteFlags == null)
        {
            mSiteFlags = new ArrayList<>();

            if(isConventionalChannel())
            {
                mSiteFlags.add("CONVENTIONAL CHANNEL");
            }

            if(isFailedConditionSite())
            {
                mSiteFlags.add("FAILURE CONDITION");
            }

            if(isValidSiteInformation())
            {
                mSiteFlags.add("VALID INFORMATION");
            }

            if(isActiveNetworkConnectionToRfssControllerSite())
            {
                mSiteFlags.add("ACTIVE RFSS CONNECTION");
            }
        }

        return mSiteFlags;
    }

    /**
     * Indicates if the channel is a conventional repeater channel
     */
    public boolean isConventionalChannel()
    {
        return getMessage().get(CONVENTIONAL_CHANNEL_FLAG);
    }

    /**
     * Indicates if the site is in a failure condition
     */
    public boolean isFailedConditionSite()
    {
        return getMessage().get(SITE_FAILURE_FLAG);
    }

    /**
     * Indicates if the site informaiton is valid
     */
    public boolean isValidSiteInformation()
    {
        return getMessage().get(VALID_INFORMATION_FLAG);
    }

    /**
     * Indicates if the site has an active network connection to the RFSS controller
     */
    public boolean isActiveNetworkConnectionToRfssControllerSite()
    {
        return getMessage().get(ACTIVE_NETWORK_CONNECTION_TO_RFSS_CONTROLLER_FLAG);
    }

    public IIdentifier getLocationRegistrationArea()
    {
        if(mLocationRegistrationArea == null)
        {
            mLocationRegistrationArea = APCO25Lra.create(getMessage().getInt(LOCATION_REGISTRATION_AREA));
        }

        return mLocationRegistrationArea;
    }

    public IIdentifier getSystem()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(getMessage().getInt(SYSTEM));
        }

        return mSystem;
    }

    public IIdentifier getSite()
    {
        if(mSite == null)
        {
            mSite = APCO25Site.create(getMessage().getInt(SITE));
        }

        return mSite;
    }

    public IIdentifier getRfss()
    {
        if(mRfss == null)
        {
            mRfss = APCO25Rfss.create(getMessage().getInt(RFSS));
        }

        return mRfss;
    }

    public IAPCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND), getMessage().getInt(CHANNEL_NUMBER));
        }

        return mChannel;
    }

    public ServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new ServiceOptions(getMessage().getInt(SYSTEM_SERVICE_CLASS));
        }

        return mServiceOptions;
    }

    @Override
    public List<IIdentifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getLocationRegistrationArea());
            mIdentifiers.add(getSystem());
            mIdentifiers.add(getSite());
            mIdentifiers.add(getRfss());
        }

        return mIdentifiers;
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }
}
