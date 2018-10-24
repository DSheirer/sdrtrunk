/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

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
 * RF Sub-System status broadcast
 */
public class RFSSStatusBroadcast extends OSPMessage implements FrequencyBandReceiver
{
    private static final int[] LOCATION_REGISTRATION_AREA = {16, 17, 18, 19, 20, 21, 22, 23};
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

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public RFSSStatusBroadcast(DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
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
        if(isActiveNetworkConnectionToRfssControllerSite())
        {
            sb.append(" ACTIVE NETWORK CONNECTION");
        }
        sb.append(" SERVICE OPTIONS:").append(getServiceOptions());
        return sb.toString();
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