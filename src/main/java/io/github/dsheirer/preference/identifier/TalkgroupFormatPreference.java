/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.preference.identifier;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.identifier.talkgroup.APCO25TalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.AnalogTalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.DMRTalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.FleetsyncTalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.LTRTalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.MDC1200TalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.MPT1327TalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.PassportTalkgroupFormatter;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specifies and applies user preferences for formatting identifiers
 */
public class TalkgroupFormatPreference extends Preference
{
    private final static Logger mLog = LoggerFactory.getLogger(TalkgroupFormatPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(TalkgroupFormatPreference.class);

    public static final String TALKGROUP_FORMAT_PROPERTY = "talkgroup.format.";
    public static final String TALKGROUP_FIXED_WIDTH_PROPERTY = "talkgroup.fixed.width.";

    private Map<Protocol,IntegerFormat> mTalkgroupFormatProtocolMap = new EnumMap<>(Protocol.class);
    private Map<Protocol,Boolean> mTalkgroupFixedWidthProtocolMap = new EnumMap<>(Protocol.class);

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
            case RADIO:
                if(identifier instanceof RadioIdentifier)
                {
                    return formatRadioIdentifier((RadioIdentifier)identifier);
                }
                break;
        }

        return identifier.toString();
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.TALKGROUP_FORMAT;
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
    public static IntegerFormat getDefaultFormat(Protocol protocol)
    {
        switch(protocol)
        {
            case FLEETSYNC:
            case LTR:
            case LTR_NET:
            case MPT1327:
                return IntegerFormat.FORMATTED;
            case AM:
            case APCO25:
            case DMR:
            case MDC1200:
            case NBFM:
            case PASSPORT:
            case UNKNOWN:
            default:
                return IntegerFormat.DECIMAL;
        }
    }

    public static Set<IntegerFormat> getFormats(Protocol protocol)
    {
        switch(protocol)
        {
            case FLEETSYNC:
            case LTR:
            case LTR_NET:
            case MPT1327:
                return IntegerFormat.DECIMAL_FORMATTED;
            case AM:
            case APCO25:
            case DMR:
            case MDC1200:
            case NBFM:
            case PASSPORT:
            case UNKNOWN:
            default:
                return IntegerFormat.DECIMAL_HEXADECIMAL;
        }
    }

    private boolean getDefaultFixedWidth(Protocol protocol)
    {
        switch(protocol)
        {
            case MDC1200:
            case PASSPORT:
                return false;
            case AM:
            case APCO25:
            case DMR:
            case FLEETSYNC:
            case LTR:
            case LTR_NET:
            case MPT1327:
            case NBFM:
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
        IntegerFormat format = null;

        if(mTalkgroupFormatProtocolMap.containsKey(protocol))
        {
            format = mTalkgroupFormatProtocolMap.get(protocol);
        }

        if(format == null || !getFormats(protocol).contains(format))
        {
            format = getDefaultFormat(protocol);
        }

        return format;
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
            case AM:
                return AnalogTalkgroupFormatter.format(talkgroupIdentifier, getTalkgroupFormat(Protocol.AM),
                        isTalkgroupFixedWidth(Protocol.AM));
            case APCO25:
                return APCO25TalkgroupFormatter.format(talkgroupIdentifier, getTalkgroupFormat(Protocol.APCO25),
                    isTalkgroupFixedWidth(Protocol.APCO25));
            case DMR:
                return DMRTalkgroupFormatter.format(talkgroupIdentifier, getTalkgroupFormat(Protocol.DMR),
                    isTalkgroupFixedWidth(Protocol.DMR));
            case FLEETSYNC:
                return FleetsyncTalkgroupFormatter.format(talkgroupIdentifier, getTalkgroupFormat(Protocol.FLEETSYNC),
                    isTalkgroupFixedWidth(Protocol.FLEETSYNC));
            case LTR:
                return LTRTalkgroupFormatter.format(talkgroupIdentifier, getTalkgroupFormat(Protocol.LTR),
                    isTalkgroupFixedWidth(Protocol.LTR));
            case MDC1200:
                return MDC1200TalkgroupFormatter.format(talkgroupIdentifier, getTalkgroupFormat(Protocol.MDC1200),
                    isTalkgroupFixedWidth(Protocol.MDC1200));
            case MPT1327:
                return MPT1327TalkgroupFormatter.format(talkgroupIdentifier, getTalkgroupFormat(Protocol.MPT1327),
                    isTalkgroupFixedWidth(Protocol.MPT1327));
            case NBFM:
                return AnalogTalkgroupFormatter.format(talkgroupIdentifier, getTalkgroupFormat(Protocol.NBFM),
                        isTalkgroupFixedWidth(Protocol.NBFM));
            case PASSPORT:
                return PassportTalkgroupFormatter.format(talkgroupIdentifier, getTalkgroupFormat(Protocol.PASSPORT),
                    isTalkgroupFixedWidth(Protocol.PASSPORT));
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

    /**
     * Formats the identifier according to user specified preferences for number format and length.
     *
     * @param radioIdentifier to format
     * @return formatted radio ID
     */
    private String formatRadioIdentifier(RadioIdentifier radioIdentifier)
    {
        switch(radioIdentifier.getProtocol())
        {
            case APCO25:
                return APCO25TalkgroupFormatter.format(radioIdentifier, getTalkgroupFormat(Protocol.APCO25),
                    isTalkgroupFixedWidth(Protocol.APCO25));
            case DMR:
                return DMRTalkgroupFormatter.format(radioIdentifier, getTalkgroupFormat(Protocol.DMR),
                    isTalkgroupFixedWidth(Protocol.DMR));
            case PASSPORT:
                return PassportTalkgroupFormatter.format(radioIdentifier, getTalkgroupFormat(Protocol.PASSPORT),
                    isTalkgroupFixedWidth(Protocol.PASSPORT));
            default:
                return radioIdentifier.toString();
        }
    }
}
