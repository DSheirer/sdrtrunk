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

package io.github.dsheirer.preference.identifier;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.preference.IntegerFormat;
import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.identifier.talkgroup.APCO25TalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.FleetsyncTalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.LtrNetTalkgroupFormatter;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Specifies and applies user preferences for formatting identifiers
 */
public class TalkgroupFormatPreference extends Preference
{
    private final static Logger mLog = LoggerFactory.getLogger(TalkgroupFormatPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(TalkgroupFormatPreference.class);

    public static final String TALKGROUP_FORMAT_PROPERTY = "talkgroup.format.";
    public static final String TALKGROUP_FIXED_WIDTH_PROPERTY = "talkgroup.fixed.width.";

    private Map<Protocol,IntegerFormat> mTalkgroupFormatProtocolMap = new HashMap<>();
    private Map<Protocol,Boolean> mTalkgroupFixedWidthProtocolMap = new HashMap<>();

    /**
     * Constructs an instance of identifier formatting preference.
     *
     * @param updateListener to be notified when this preference is updated
     */
    public TalkgroupFormatPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
        loadProperties();
    }

    /**
     * Formats an identifier
     *
     * @param identifier to format
     * @return formatted string representing the identifier or null if the identifier is null
     */
    public String format(Identifier identifier)
    {
        if(identifier == null)
        {
            return null;
        }

        switch(identifier.getForm())
        {
            case TALKGROUP:
                if(identifier instanceof TalkgroupIdentifier)
                {
                    return formatTalkgroupIdentifier((TalkgroupIdentifier)identifier);
                }
                break;
            case PATCH_GROUP:
                if(identifier instanceof PatchGroupIdentifier)
                {
                    return formatPatchGroupIdentifier((PatchGroupIdentifier)identifier);
                }
                break;
        }

        return identifier.toString();
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.IDENTIFIER;
    }

    /**
     * Loads persisted properties on startup
     */
    private void loadProperties()
    {
        for(Protocol protocol : Protocol.TALKGROUP_PROTOCOLS)
        {
            mTalkgroupFixedWidthProtocolMap.put(protocol, mPreferences.getBoolean(getTalkgroupFixedWidthProperty(protocol),
                getDefaultFixedWidth(protocol)));

            try
            {
                String format = mPreferences.get(getTalkgroupFormatProperty(protocol), getDefaultFormat(protocol).name());
                mTalkgroupFormatProtocolMap.put(protocol, IntegerFormat.valueOf(format));
            }
            catch(Exception e)
            {
                //If there is a problem parsing the enum entry, default to DECIMAL
                mTalkgroupFormatProtocolMap.put(protocol, getDefaultFormat(protocol));
            }
        }
    }

    /**
     * Default format for each protocol
     */
    private IntegerFormat getDefaultFormat(Protocol protocol)
    {
        switch(protocol)
        {
            case APCO25:
                return IntegerFormat.DECIMAL;
            case FLEETSYNC:
                return IntegerFormat.FORMATTED;
            case IPV4:
                return IntegerFormat.DECIMAL;
            case LOJACK:
                return IntegerFormat.DECIMAL;
            case LTR:
                return IntegerFormat.FORMATTED;
            case LTR_NET:
                return IntegerFormat.FORMATTED;
            case LTR_STANDARD:
                return IntegerFormat.FORMATTED;
            case MDC1200:
                return IntegerFormat.DECIMAL;
            case MPT1327:
                return IntegerFormat.FORMATTED;
            case PASSPORT:
                return IntegerFormat.DECIMAL;
            case TAIT1200:
                return IntegerFormat.DECIMAL;
            case UNKNOWN:
            default:
                return IntegerFormat.DECIMAL;
        }
    }

    private boolean getDefaultFixedWidth(Protocol protocol)
    {
        switch(protocol)
        {
            case APCO25:
                return true;
            case FLEETSYNC:
                return true;
            case IPV4:
                return true;
            case LOJACK:
                return true;
            case LTR:
                return true;
            case LTR_NET:
                return true;
            case LTR_STANDARD:
                return true;
            case MDC1200:
                return false;
            case MPT1327:
                return true;
            case PASSPORT:
                return false;
            case TAIT1200:
                return false;
            case UNKNOWN:
            default:
                return true;
        }
    }

    /**
     * Property identifier for talkgroup prepad for a specific protocol
     */
    private String getTalkgroupFixedWidthProperty(Protocol protocol)
    {
        return TALKGROUP_FIXED_WIDTH_PROPERTY + protocol.name();
    }

    /**
     * Property identifier for talkgroup format for a specific protocol
     */
    private String getTalkgroupFormatProperty(Protocol protocol)
    {
        return TALKGROUP_FORMAT_PROPERTY + protocol.name();
    }

    /**
     * User preference for formatting talkgroups for the specified protocol
     *
     * @param protocol specified
     * @return format
     */
    public IntegerFormat getTalkgroupFormat(Protocol protocol)
    {
        if(mTalkgroupFormatProtocolMap.containsKey(protocol))
        {
            return mTalkgroupFormatProtocolMap.get(protocol);
        }

        return IntegerFormat.DECIMAL;
    }

    /**
     * Sets the formatting for talkgroup identifiers by protocol
     *
     * @param protocol for the talkgroup
     * @param talkgroupFormat
     */
    public void setTalkgroupFormat(Protocol protocol, IntegerFormat talkgroupFormat)
    {
        IntegerFormat existing = mTalkgroupFormatProtocolMap.get(protocol);

        if(existing == null || existing != talkgroupFormat)
        {
            mTalkgroupFormatProtocolMap.put(protocol, talkgroupFormat);
            mPreferences.put(getTalkgroupFormatProperty(protocol), talkgroupFormat.name());
            notifyPreferenceUpdated();
        }
    }

    /**
     * Sets the fixed-width for talkgroup identifiers by protocol
     *
     * @param protocol for the talkgroup
     * @param fixedWidth
     */
    public void setTalkgroupFormat(Protocol protocol, boolean fixedWidth)
    {
        mTalkgroupFixedWidthProtocolMap.put(protocol, fixedWidth);
        mPreferences.putBoolean(getTalkgroupFixedWidthProperty(protocol), fixedWidth);
        notifyPreferenceUpdated();
    }

    /**
     * Indicates if talkgroups for the specified protocol should be prepadded to a fixed length using '0' padding characters
     *
     * @param protocol to prepad
     * @return true if the talkgroups should be fixed length/prepadded.
     */
    public boolean isTalkgroupFixedWidth(Protocol protocol)
    {
        Boolean fixedWidth = mTalkgroupFixedWidthProtocolMap.get(protocol);

        if(fixedWidth == null)
        {
            fixedWidth = false;
        }

        return fixedWidth;
    }

    /**
     * Sets the talkgroup for the specified protocol to the fixed width argument.
     *
     * @param protocol to set
     * @param fixedWidth true for prepadded (0) fixed width values or false for no prepadding.
     */
    public void setTalkgroupFixedWidth(Protocol protocol, boolean fixedWidth)
    {
        if(isTalkgroupFixedWidth(protocol) != fixedWidth)
        {
            mTalkgroupFixedWidthProtocolMap.put(protocol, fixedWidth);
            mPreferences.putBoolean(getTalkgroupFixedWidthProperty(protocol), fixedWidth);
            notifyPreferenceUpdated();
        }
    }

    /**
     * Formats the identifier according to user specified preferences for number format and length.
     *
     * @param talkgroupIdentifier to format
     * @return formatted talkgroups
     */
    private String formatTalkgroupIdentifier(TalkgroupIdentifier talkgroupIdentifier)
    {
        switch(talkgroupIdentifier.getProtocol())
        {
            case APCO25:
                return APCO25TalkgroupFormatter.format(talkgroupIdentifier, getTalkgroupFormat(Protocol.APCO25),
                    isTalkgroupFixedWidth(Protocol.APCO25));
            case FLEETSYNC:
                return FleetsyncTalkgroupFormatter.format(talkgroupIdentifier, getTalkgroupFormat(Protocol.FLEETSYNC),
                    isTalkgroupFixedWidth(Protocol.FLEETSYNC));
            case LTR_NET:
                return LtrNetTalkgroupFormatter.format(talkgroupIdentifier, getTalkgroupFormat(Protocol.LTR_NET),
                    isTalkgroupFixedWidth(Protocol.LTR_NET));
            default:
                return talkgroupIdentifier.toString();
        }
    }

    /**
     * Formats the identifier according to user specified preferences for number format and length.
     *
     * @param patchGroupIdentifier to format
     * @return formatted talkgroups
     */
    private String formatPatchGroupIdentifier(PatchGroupIdentifier patchGroupIdentifier)
    {
        switch(patchGroupIdentifier.getProtocol())
        {
            case APCO25:
                return APCO25TalkgroupFormatter.format(patchGroupIdentifier, getTalkgroupFormat(Protocol.APCO25),
                    isTalkgroupFixedWidth(Protocol.APCO25));
            default:
                return patchGroupIdentifier.toString();
        }
    }
}
