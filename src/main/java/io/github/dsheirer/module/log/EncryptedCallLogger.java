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
package io.github.dsheirer.module.log;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.alias.TalkerAliasIdentifier;
import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.identifier.encryption.EncryptionKey;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.identifier.location.LocationIdentifier;
import io.github.dsheirer.identifier.location.Point;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.radio.FullyQualifiedRadioIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.FullyQualifiedTalkgroupIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventListener;
import io.github.dsheirer.module.decode.p25.identifier.encryption.APCO25EncryptionKey;
import io.github.dsheirer.module.decode.p25.reference.Encryption;
import io.github.dsheirer.preference.TimestampFormat;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.QuoteMode;

/**
 * Specialized logger for encrypted radio communications.
 * Captures comprehensive metadata about encrypted calls including encryption algorithm,
 * key ID, participants, channel information, and timing details.
 */
public class EncryptedCallLogger extends EventLogger implements IDecodeEventListener, Listener<IDecodeEvent>
{
    private SimpleDateFormat mTimestampFormat = TimestampFormat.TIMESTAMP_COLONS.getFormatter();
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private DecimalFormat mFrequencyFormat = new DecimalFormat("0.000000");
    private AliasList mAliasList;
    private AliasModel mAliasModel;

    /**
     * All encrypted event types that this logger captures
     */
    private static final EnumSet<DecodeEventType> ENCRYPTED_EVENTS = EnumSet.of(
        DecodeEventType.CALL_ENCRYPTED,
        DecodeEventType.CALL_GROUP_ENCRYPTED,
        DecodeEventType.CALL_PATCH_GROUP_ENCRYPTED,
        DecodeEventType.CALL_INTERCONNECT_ENCRYPTED,
        DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED,
        DecodeEventType.DATA_CALL_ENCRYPTED
    );

    /**
     * CSV format for logging
     */
    private final CSVFormat mCsvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setQuoteMode(QuoteMode.ALL)
            .build();

    /**
     * Constructor
     * @param aliasModel for resolving aliases
     * @param logDirectory where log files are written
     * @param fileNameSuffix suffix for the log file name
     * @param frequency of the channel
     */
    public EncryptedCallLogger(AliasModel aliasModel, Path logDirectory, String fileNameSuffix, long frequency)
    {
        super(logDirectory, fileNameSuffix, frequency);
        mAliasModel = aliasModel;
    }

    @Override
    public void receive(IDecodeEvent decodeEvent)
    {
        // Only log encrypted events
        if(isEncryptedEvent(decodeEvent))
        {
            write(toCSV(decodeEvent));
        }
    }

    /**
     * Checks if the event is an encrypted event type
     * @param event to check
     * @return true if this is an encrypted event
     */
    private boolean isEncryptedEvent(IDecodeEvent event)
    {
        if(event == null || event.getEventType() == null)
        {
            return false;
        }

        // Check event type
        if(ENCRYPTED_EVENTS.contains(event.getEventType()))
        {
            return true;
        }

        // Also check for encryption key identifier as a fallback
        if(event.getIdentifierCollection() != null)
        {
            Identifier encryptionId = event.getIdentifierCollection().getEncryptionIdentifier();
            if(encryptionId instanceof EncryptionKeyIdentifier)
            {
                return ((EncryptionKeyIdentifier)encryptionId).isEncrypted();
            }
        }

        return false;
    }

    @Override
    public String getHeader()
    {
        return getCSVHeader();
    }

    @Override
    public Listener<IDecodeEvent> getDecodeEventListener()
    {
        return this;
    }

    @Override
    public void reset()
    {
    }

    /**
     * Returns the CSV header for the encrypted call log
     */
    public static String getCSVHeader()
    {
        return "DATE,TIMESTAMP,TIME_END,DURATION_MS,PROTOCOL,EVENT_TYPE," +
               "ENCRYPTION_ALGORITHM,ALGORITHM_ID,KEY_ID," +
               "FROM_ID,FROM_ID_RAW,FROM_ALIAS,FROM_WACN,FROM_SYSTEM,FROM_RADIO,FROM_IS_ROAMING," +
               "TO_ID,TO_ID_RAW,TO_ALIAS,TO_WACN,TO_SYSTEM,TO_TALKGROUP," +
               "TALKGROUP_RAW,PATCH_GROUP,PATCHED_TALKGROUPS," +
               "TALKER_ALIAS,ESN,UNIT_STATUS,USER_STATUS,TELEPHONE_NUMBER," +
               "LATITUDE,LONGITUDE," +
               "CHANNEL_NUMBER,FREQUENCY_MHZ,UPLINK_FREQ_MHZ,TIMESLOT,IS_TDMA,TIMESLOT_COUNT," +
               "WACN,SYSTEM_ID,SITE_ID,RFSS,NAC,LRA,DMR_NETWORK,DMR_SITE," +
               "SYSTEM_CONFIG,SITE_CONFIG,NETWORK_ID,DETAILS,EVENT_HASH";
    }

    /**
     * Converts a decode event to a CSV row with comprehensive encryption metadata
     * @param event the decode event
     * @return CSV formatted string
     */
    private String toCSV(IDecodeEvent event)
    {
        List<Object> cells = new ArrayList<>();
        IdentifierCollection identifiers = event.getIdentifierCollection();
        Protocol protocol = event.getProtocol();
        IChannelDescriptor descriptor = event.getChannelDescriptor();

        // DATE (separate from timestamp for easier filtering)
        cells.add(mDateFormat.format(new Date(event.getTimeStart())));

        // TIMESTAMP
        cells.add(mTimestampFormat.format(new Date(event.getTimeStart())));

        // TIME_END
        Long timeEnd = event.getTimeEnd();
        cells.add(timeEnd != null && timeEnd > 0 ? mTimestampFormat.format(new Date(timeEnd)) : "");

        // DURATION_MS
        cells.add(event.getDuration() > 0 ? event.getDuration() : "");

        // PROTOCOL
        cells.add(protocol != null ? protocol : "");

        // EVENT_TYPE
        cells.add(event.getEventType() != null ? event.getEventType().getLabel() : "");

        // Encryption details
        String algorithmName = "";
        String algorithmId = "";
        String keyId = "";

        if(identifiers != null)
        {
            Identifier encryptionId = identifiers.getEncryptionIdentifier();
            if(encryptionId instanceof EncryptionKeyIdentifier)
            {
                EncryptionKey encryptionKey = ((EncryptionKeyIdentifier)encryptionId).getValue();
                if(encryptionKey != null)
                {
                    algorithmId = String.valueOf(encryptionKey.getAlgorithm());
                    keyId = String.valueOf(encryptionKey.getKey());

                    // Get algorithm name for P25
                    if(encryptionKey instanceof APCO25EncryptionKey)
                    {
                        Encryption encryption = ((APCO25EncryptionKey)encryptionKey).getEncryptionAlgorithm();
                        algorithmName = encryption != null ? encryption.toString() : "UNKNOWN";
                    }
                    else
                    {
                        algorithmName = getAlgorithmName(encryptionKey.getAlgorithm());
                    }
                }
            }
        }

        // ENCRYPTION_ALGORITHM
        cells.add(algorithmName);

        // ALGORITHM_ID
        cells.add(algorithmId);

        // KEY_ID
        cells.add(keyId);

        // FROM identifier
        String fromId = "";
        String fromAlias = "";
        if(identifiers != null)
        {
            Identifier fromIdentifier = identifiers.getFromIdentifier();
            if(fromIdentifier != null)
            {
                fromId = fromIdentifier.toString();
                fromAlias = getAlias(identifiers, fromIdentifier);
            }
        }

        // FROM_ID
        cells.add(fromId);

        // FROM_ID_RAW
        cells.add(getRawRadioId(identifiers));

        // FROM_ALIAS
        cells.add(fromAlias);

        // Fully qualified FROM radio (roaming)
        FullyQualifiedRadioIdentifier fqFrom = getFullyQualifiedFromRadio(identifiers);

        // FROM_WACN
        cells.add(fqFrom != null ? fqFrom.getWacn() : "");

        // FROM_SYSTEM
        cells.add(fqFrom != null ? fqFrom.getSystem() : "");

        // FROM_RADIO
        cells.add(fqFrom != null ? fqFrom.getRadio() : "");

        // FROM_IS_ROAMING
        cells.add(fqFrom != null ? (fqFrom.isAliased() ? "1" : "0") : "");

        // TO identifier
        String toId = "";
        String toAlias = "";
        if(identifiers != null)
        {
            Identifier toIdentifier = identifiers.getToIdentifier();
            if(toIdentifier != null)
            {
                toId = toIdentifier.toString();
                toAlias = getAlias(identifiers, toIdentifier);
            }
        }

        // TO_ID
        cells.add(toId);

        // TO_ID_RAW
        cells.add(getRawTalkgroupId(identifiers));

        // TO_ALIAS
        cells.add(toAlias);

        // Fully qualified TO talkgroup (roaming)
        FullyQualifiedTalkgroupIdentifier fqTo = getFullyQualifiedToTalkgroup(identifiers);

        // TO_WACN
        cells.add(fqTo != null ? fqTo.getWacn() : "");

        // TO_SYSTEM
        cells.add(fqTo != null ? fqTo.getSystem() : "");

        // TO_TALKGROUP
        cells.add(fqTo != null ? fqTo.getTalkgroup() : "");

        // TALKGROUP_RAW
        cells.add(getTalkgroupRaw(identifiers));

        // PATCH_GROUP
        cells.add(getPatchGroupString(identifiers));

        // PATCHED_TALKGROUPS
        cells.add(getPatchedTalkgroups(identifiers));

        // TALKER_ALIAS
        cells.add(getTalkerAlias(identifiers));

        // ESN
        cells.add(getEsn(identifiers));

        // UNIT_STATUS
        cells.add(getUnitStatus(identifiers));

        // USER_STATUS
        cells.add(getUserStatus(identifiers));

        // TELEPHONE_NUMBER
        cells.add(getTelephoneNumber(identifiers));

        // LATITUDE
        Double latitude = getLatitude(identifiers);
        cells.add(latitude != null ? latitude : "");

        // LONGITUDE
        Double longitude = getLongitude(identifiers);
        cells.add(longitude != null ? longitude : "");

        // CHANNEL_NUMBER
        cells.add(descriptor != null ? descriptor.toString() : "");

        // FREQUENCY_MHZ from channel descriptor
        if(descriptor != null)
        {
            cells.add(mFrequencyFormat.format(descriptor.getDownlinkFrequency() / 1e6d));
        }
        else
        {
            cells.add("");
        }

        // UPLINK_FREQ_MHZ
        Long uplinkFreq = getUplinkFrequency(descriptor);
        cells.add(uplinkFreq != null ? mFrequencyFormat.format(uplinkFreq / 1e6d) : "");

        // TIMESLOT
        if(event.hasTimeslot())
        {
            cells.add(event.getTimeslot());
        }
        else
        {
            cells.add("");
        }

        // IS_TDMA
        Boolean isTdma = isTdma(descriptor);
        cells.add(isTdma != null ? (isTdma ? "1" : "0") : "");

        // TIMESLOT_COUNT
        Integer timeslotCount = getTimeslotCount(descriptor);
        cells.add(timeslotCount != null ? timeslotCount : "");

        // Network identifiers (P25)
        // WACN
        cells.add(getWacn(identifiers));

        // SYSTEM_ID
        cells.add(getSystemId(identifiers));

        // SITE_ID
        cells.add(getSiteId(identifiers));

        // RFSS
        cells.add(getRfss(identifiers));

        // NAC
        cells.add(getNac(identifiers));

        // LRA
        cells.add(getLra(identifiers));

        // DMR_NETWORK
        cells.add(getDmrNetwork(identifiers, protocol));

        // DMR_SITE
        cells.add(getDmrSite(identifiers, protocol));

        // SYSTEM_CONFIG
        String system = "";
        if(identifiers != null)
        {
            Identifier systemId = identifiers.getIdentifier(IdentifierClass.CONFIGURATION, Form.SYSTEM, Role.ANY);
            if(systemId != null)
            {
                system = systemId.toString();
            }
        }
        cells.add(system);

        // SITE_CONFIG
        String site = "";
        if(identifiers != null)
        {
            Identifier siteId = identifiers.getIdentifier(IdentifierClass.CONFIGURATION, Form.SITE, Role.ANY);
            if(siteId != null)
            {
                site = siteId.toString();
            }
        }
        cells.add(site);

        // NETWORK_ID
        String network = "";
        if(identifiers != null)
        {
            Identifier networkId = identifiers.getIdentifier(IdentifierClass.NETWORK, Form.WACN, Role.ANY);
            if(networkId != null)
            {
                network = networkId.toString();
            }
            else
            {
                networkId = identifiers.getIdentifier(IdentifierClass.NETWORK, Form.NETWORK, Role.ANY);
                if(networkId != null)
                {
                    network = networkId.toString();
                }
            }
        }
        cells.add(network);

        // DETAILS
        String details = event.getDetails();
        cells.add(details != null ? details : "");

        // EVENT_HASH
        cells.add(event.hashCode());

        return mCsvFormat.format(cells.toArray());
    }

    /**
     * Gets the alias for an identifier if available
     * @param collection the identifier collection
     * @param identifier the identifier to lookup
     * @return alias string or empty string
     */
    private String getAlias(IdentifierCollection collection, Identifier identifier)
    {
        if(collection == null || identifier == null)
        {
            return "";
        }

        try
        {
            Identifier aliasListId = collection.getIdentifier(
                IdentifierClass.CONFIGURATION, Form.ALIAS_LIST, Role.ANY);

            if(aliasListId instanceof AliasListConfigurationIdentifier)
            {
                mAliasList = mAliasModel.getAliasList((AliasListConfigurationIdentifier)aliasListId);

                if(mAliasList != null && !mAliasList.getAliases(identifier).isEmpty())
                {
                    return mAliasList.getAliases(identifier).get(0).getName();
                }
            }
        }
        catch(Exception e)
        {
            // Ignore alias lookup failures
        }

        return "";
    }

    // ============================================================
    // Helper methods for extracting network identifiers (P25)
    // ============================================================

    /**
     * Gets WACN (Wide Area Communications Network ID) from identifier collection
     */
    private String getWacn(IdentifierCollection ids)
    {
        if(ids == null) return "";
        Identifier id = ids.getIdentifier(IdentifierClass.NETWORK, Form.WACN, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return String.valueOf(((IntegerIdentifier)id).getValue());
        }
        return "";
    }

    /**
     * Gets P25 System ID from identifier collection
     */
    private String getSystemId(IdentifierCollection ids)
    {
        if(ids == null) return "";
        Identifier id = ids.getIdentifier(IdentifierClass.NETWORK, Form.SYSTEM, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return String.valueOf(((IntegerIdentifier)id).getValue());
        }
        return "";
    }

    /**
     * Gets Site ID from identifier collection
     */
    private String getSiteId(IdentifierCollection ids)
    {
        if(ids == null) return "";
        Identifier id = ids.getIdentifier(IdentifierClass.NETWORK, Form.SITE, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return String.valueOf(((IntegerIdentifier)id).getValue());
        }
        return "";
    }

    /**
     * Gets RF Subsystem ID from identifier collection
     */
    private String getRfss(IdentifierCollection ids)
    {
        if(ids == null) return "";
        Identifier id = ids.getIdentifier(IdentifierClass.NETWORK, Form.RF_SUBSYSTEM, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return String.valueOf(((IntegerIdentifier)id).getValue());
        }
        return "";
    }

    /**
     * Gets Network Access Code from identifier collection
     */
    private String getNac(IdentifierCollection ids)
    {
        if(ids == null) return "";
        Identifier id = ids.getIdentifier(IdentifierClass.NETWORK, Form.NETWORK_ACCESS_CODE, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return String.valueOf(((IntegerIdentifier)id).getValue());
        }
        return "";
    }

    /**
     * Gets Location Registration Area from identifier collection
     */
    private String getLra(IdentifierCollection ids)
    {
        if(ids == null) return "";
        Identifier id = ids.getIdentifier(IdentifierClass.NETWORK, Form.LOCATION_REGISTRATION_AREA, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return String.valueOf(((IntegerIdentifier)id).getValue());
        }
        return "";
    }

    // ============================================================
    // Helper methods for extracting network identifiers (DMR)
    // ============================================================

    /**
     * Gets DMR Network ID from identifier collection
     */
    private String getDmrNetwork(IdentifierCollection ids, Protocol protocol)
    {
        if(ids == null || protocol != Protocol.DMR) return "";
        Identifier id = ids.getIdentifier(IdentifierClass.NETWORK, Form.NETWORK, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return String.valueOf(((IntegerIdentifier)id).getValue());
        }
        return "";
    }

    /**
     * Gets DMR Site ID from identifier collection
     */
    private String getDmrSite(IdentifierCollection ids, Protocol protocol)
    {
        if(ids == null || protocol != Protocol.DMR) return "";
        Identifier id = ids.getIdentifier(IdentifierClass.NETWORK, Form.SITE, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return String.valueOf(((IntegerIdentifier)id).getValue());
        }
        return "";
    }

    // ============================================================
    // Helper methods for extracting user/entity metadata
    // ============================================================

    /**
     * Gets raw radio ID (numeric) from the FROM identifier
     */
    private String getRawRadioId(IdentifierCollection ids)
    {
        if(ids == null) return "";
        Identifier from = ids.getFromIdentifier();
        if(from instanceof RadioIdentifier)
        {
            return String.valueOf(((RadioIdentifier)from).getValue());
        }
        return "";
    }

    /**
     * Gets raw talkgroup ID (numeric) from the TO identifier
     */
    private String getRawTalkgroupId(IdentifierCollection ids)
    {
        if(ids == null) return "";
        Identifier to = ids.getToIdentifier();
        if(to instanceof TalkgroupIdentifier)
        {
            return String.valueOf(((TalkgroupIdentifier)to).getValue());
        }
        return "";
    }

    /**
     * Gets raw talkgroup from Form.TALKGROUP identifier
     */
    private String getTalkgroupRaw(IdentifierCollection ids)
    {
        if(ids == null) return "";
        Identifier id = ids.getIdentifier(IdentifierClass.USER, Form.TALKGROUP, Role.ANY);
        if(id instanceof TalkgroupIdentifier)
        {
            return String.valueOf(((TalkgroupIdentifier)id).getValue());
        }
        return "";
    }

    /**
     * Gets talker alias from identifier collection
     */
    private String getTalkerAlias(IdentifierCollection ids)
    {
        if(ids == null) return "";
        Identifier id = ids.getIdentifier(IdentifierClass.USER, Form.TALKER_ALIAS, Role.ANY);
        if(id instanceof TalkerAliasIdentifier)
        {
            return ((TalkerAliasIdentifier)id).getValue();
        }
        return "";
    }

    /**
     * Gets patch group identifier as string
     */
    private String getPatchGroupString(IdentifierCollection ids)
    {
        if(ids == null) return "";
        Identifier to = ids.getToIdentifier();
        if(to instanceof PatchGroupIdentifier)
        {
            PatchGroup pg = ((PatchGroupIdentifier)to).getValue();
            if(pg != null && pg.getPatchGroup() != null)
            {
                return String.valueOf(pg.getPatchGroup().getValue());
            }
        }
        return "";
    }

    /**
     * Gets comma-separated list of patched talkgroups
     */
    private String getPatchedTalkgroups(IdentifierCollection ids)
    {
        if(ids == null) return "";
        Identifier to = ids.getToIdentifier();
        if(to instanceof PatchGroupIdentifier)
        {
            PatchGroup pg = ((PatchGroupIdentifier)to).getValue();
            if(pg != null)
            {
                Collection<TalkgroupIdentifier> patched = pg.getPatchedTalkgroupIdentifiers();
                if(patched != null && !patched.isEmpty())
                {
                    return patched.stream()
                        .map(tg -> String.valueOf(tg.getValue()))
                        .collect(Collectors.joining(","));
                }
            }
        }
        return "";
    }

    /**
     * Gets ESN (Electronic Serial Number) from identifier collection
     */
    private String getEsn(IdentifierCollection ids)
    {
        if(ids == null) return "";
        Identifier id = ids.getIdentifier(IdentifierClass.USER, Form.ESN, Role.ANY);
        return id != null ? id.toString() : "";
    }

    /**
     * Gets unit status from identifier collection
     */
    private String getUnitStatus(IdentifierCollection ids)
    {
        if(ids == null) return "";
        Identifier id = ids.getIdentifier(IdentifierClass.USER, Form.UNIT_STATUS, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return String.valueOf(((IntegerIdentifier)id).getValue());
        }
        return "";
    }

    /**
     * Gets user status from identifier collection
     */
    private String getUserStatus(IdentifierCollection ids)
    {
        if(ids == null) return "";
        Identifier id = ids.getIdentifier(IdentifierClass.USER, Form.USER_STATUS, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return String.valueOf(((IntegerIdentifier)id).getValue());
        }
        return "";
    }

    /**
     * Gets telephone number from identifier collection
     */
    private String getTelephoneNumber(IdentifierCollection ids)
    {
        if(ids == null) return "";
        Identifier id = ids.getIdentifier(IdentifierClass.USER, Form.TELEPHONE_NUMBER, Role.ANY);
        return id != null ? id.toString() : "";
    }

    // ============================================================
    // Helper methods for fully qualified identifiers (roaming)
    // ============================================================

    /**
     * Gets fully qualified radio info (FROM) for roaming radios
     */
    private FullyQualifiedRadioIdentifier getFullyQualifiedFromRadio(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier from = ids.getFromIdentifier();
        if(from instanceof FullyQualifiedRadioIdentifier)
        {
            return (FullyQualifiedRadioIdentifier)from;
        }
        return null;
    }

    /**
     * Gets fully qualified talkgroup info (TO) for roaming
     */
    private FullyQualifiedTalkgroupIdentifier getFullyQualifiedToTalkgroup(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier to = ids.getToIdentifier();
        if(to instanceof FullyQualifiedTalkgroupIdentifier)
        {
            return (FullyQualifiedTalkgroupIdentifier)to;
        }
        return null;
    }

    // ============================================================
    // Helper methods for location/GPS
    // ============================================================

    /**
     * Gets LocationIdentifier from identifier collection
     */
    private LocationIdentifier getLocation(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier id = ids.getIdentifier(IdentifierClass.USER, Form.LOCATION, Role.ANY);
        if(id instanceof LocationIdentifier)
        {
            return (LocationIdentifier)id;
        }
        return null;
    }

    /**
     * Gets latitude from location identifier
     */
    private Double getLatitude(IdentifierCollection ids)
    {
        LocationIdentifier loc = getLocation(ids);
        if(loc != null && loc.getValue() != null)
        {
            return loc.getValue().getLatitude();
        }
        return null;
    }

    /**
     * Gets longitude from location identifier
     */
    private Double getLongitude(IdentifierCollection ids)
    {
        LocationIdentifier loc = getLocation(ids);
        if(loc != null && loc.getValue() != null)
        {
            return loc.getValue().getLongitude();
        }
        return null;
    }

    // ============================================================
    // Helper methods for channel descriptor
    // ============================================================

    /**
     * Gets uplink frequency from channel descriptor
     */
    private Long getUplinkFrequency(IChannelDescriptor descriptor)
    {
        if(descriptor == null) return null;
        long freq = descriptor.getUplinkFrequency();
        return freq > 0 ? freq : null;
    }

    /**
     * Checks if channel is TDMA
     */
    private Boolean isTdma(IChannelDescriptor descriptor)
    {
        if(descriptor == null) return null;
        return descriptor.isTDMAChannel();
    }

    /**
     * Gets timeslot count from channel descriptor
     */
    private Integer getTimeslotCount(IChannelDescriptor descriptor)
    {
        if(descriptor == null) return null;
        int count = descriptor.getTimeslotCount();
        return count > 0 ? count : null;
    }

    /**
     * Gets algorithm name for non-P25 protocols based on algorithm ID
     * @param algorithmId the numeric algorithm ID
     * @return algorithm name string
     */
    private String getAlgorithmName(int algorithmId)
    {
        // DMR algorithm IDs
        switch(algorithmId)
        {
            case 0x00:
                return "NO_ENCRYPTION";
            case 0x01:
                return "HYTERA_BASIC_PRIVACY";
            case 0x02:
            case 0x26:
                return "HYTERA_RC4_EP";
            case 0x21:
                return "DMRA_RC4_EP";
            case 0x24:
                return "DMRA_AES128";
            case 0x25:
                return "DMRA_AES256";
            default:
                // Try P25 algorithms
                Encryption p25Encryption = Encryption.fromValue(algorithmId);
                if(p25Encryption != Encryption.UNKNOWN)
                {
                    return p25Encryption.toString();
                }
                return "UNKNOWN_0x" + Integer.toHexString(algorithmId).toUpperCase();
        }
    }
}
