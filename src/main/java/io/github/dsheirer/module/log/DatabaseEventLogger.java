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
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageListener;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventListener;
import io.github.dsheirer.module.decode.p25.identifier.encryption.APCO25EncryptionKey;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Lra;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Nac;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Rfss;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Site;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.phase1.message.IAdjacentSite;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCGroupVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCUnitToUnitVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.AdjacentStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.NetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.RFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.UnitToUnitVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.UnitRegistrationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.LocationRegistrationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupAffiliationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.UnitRegistrationRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.LocationRegistrationRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.GroupAffiliationRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaBaseStationId;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaGroupRegroupChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaEmergencyAlarmActivation;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.StatusUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.StatusQuery;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.MessageUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.CallAlert;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.RadioUnitMonitorCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.ExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.AcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.DenyResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.QueuedResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.TelephoneInterconnectVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SNDCPDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.EmergencyAlarmRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.StatusUpdateRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.CallAlertRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaUnitGPS;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisTalkerGPSComplete;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructure;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RfssStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RfssStatusBroadcastImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RfssStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AdjacentStatusBroadcastImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AdjacentStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdateImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdateExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelUserAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelUserExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationResponseAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationResponseExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitRegistrationResponseAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitRegistrationResponseExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.EndPushToTalk;
import io.github.dsheirer.module.decode.p25.reference.Encryption;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SQLite database logger for decode events and messages.
 * Stores all events in a local SQLite database for later analysis.
 */
public class DatabaseEventLogger extends Module implements IDecodeEventListener, IMessageListener, Listener<IDecodeEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(DatabaseEventLogger.class);

    private static final EnumSet<DecodeEventType> ENCRYPTED_EVENTS = EnumSet.of(
        DecodeEventType.CALL_ENCRYPTED,
        DecodeEventType.CALL_GROUP_ENCRYPTED,
        DecodeEventType.CALL_PATCH_GROUP_ENCRYPTED,
        DecodeEventType.CALL_INTERCONNECT_ENCRYPTED,
        DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED,
        DecodeEventType.DATA_CALL_ENCRYPTED
    );

    private Connection mConnection;
    private PreparedStatement mInsertEventStatement;
    private PreparedStatement mInsertEncryptedEventStatement;
    private PreparedStatement mInsertMessageStatement;
    private PreparedStatement mInsertNetworkStatusStatement;
    private PreparedStatement mInsertAdjacentSiteStatement;
    private PreparedStatement mInsertBaseStationStatement;
    private PreparedStatement mInsertRegistrationStatement;
    private PreparedStatement mInsertAffiliationStatement;
    private PreparedStatement mInsertChannelGrantStatement;
    private PreparedStatement mInsertStatusMessageStatement;
    private PreparedStatement mInsertGpsLocationStatement;
    private PreparedStatement mInsertEmergencyAlarmStatement;
    private Path mDatabasePath;
    private AliasModel mAliasModel;
    private AliasList mAliasList;
    private boolean mRunning = false;
    private String mChannelName;

    // Cache for current site info (updated from RFSS Status broadcasts)
    private volatile Integer mCurrentSiteId;
    private volatile Integer mCurrentRfss;
    private volatile Integer mCurrentSystemId;
    private volatile Integer mCurrentWacn;
    private volatile Integer mCurrentLra;
    private volatile Integer mCurrentNac;

    /**
     * Constructor
     * @param aliasModel for resolving aliases
     * @param databasePath path to the SQLite database file
     * @param channelName name of the channel being logged
     */
    public DatabaseEventLogger(AliasModel aliasModel, Path databasePath, String channelName)
    {
        mAliasModel = aliasModel;
        mDatabasePath = databasePath;
        mChannelName = channelName;
    }

    /**
     * Starts the database logger and creates necessary tables
     */
    @Override
    public void start()
    {
        if(mRunning)
        {
            return;
        }

        try
        {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Create connection
            String url = "jdbc:sqlite:" + mDatabasePath.toString();
            mConnection = DriverManager.getConnection(url);
            mConnection.setAutoCommit(true);

            // Enable WAL mode for better concurrency (allows concurrent reads/writes)
            try(Statement stmt = mConnection.createStatement())
            {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA busy_timeout=5000"); // Wait up to 5 seconds if database is busy
                stmt.execute("PRAGMA synchronous=NORMAL"); // Faster writes with WAL
            }

            // Create tables
            createTables();

            // Prepare statements
            prepareStatements();

            mRunning = true;
            mLog.info("Database logger started: " + mDatabasePath.toString());
        }
        catch(ClassNotFoundException e)
        {
            mLog.error("SQLite JDBC driver not found", e);
        }
        catch(SQLException e)
        {
            mLog.error("Error initializing database connection", e);
        }
    }

    /**
     * Stops the database logger and closes connections
     */
    @Override
    public void stop()
    {
        if(!mRunning)
        {
            return;
        }

        try
        {
            if(mInsertEventStatement != null)
            {
                mInsertEventStatement.close();
            }
            if(mInsertEncryptedEventStatement != null)
            {
                mInsertEncryptedEventStatement.close();
            }
            if(mInsertMessageStatement != null)
            {
                mInsertMessageStatement.close();
            }
            if(mInsertNetworkStatusStatement != null)
            {
                mInsertNetworkStatusStatement.close();
            }
            if(mInsertAdjacentSiteStatement != null)
            {
                mInsertAdjacentSiteStatement.close();
            }
            if(mInsertBaseStationStatement != null)
            {
                mInsertBaseStationStatement.close();
            }
            if(mConnection != null && !mConnection.isClosed())
            {
                mConnection.close();
            }
            mRunning = false;
            mLog.info("Database logger stopped");
        }
        catch(SQLException e)
        {
            mLog.error("Error closing database connection", e);
        }
    }

    /**
     * Creates the database tables if they don't exist
     */
    private void createTables() throws SQLException
    {
        try(Statement stmt = mConnection.createStatement())
        {
            // Decode events table with comprehensive metadata
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS decode_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    time_end INTEGER,
                    channel_name TEXT,
                    event_type TEXT,
                    protocol TEXT,
                    duration_ms INTEGER,
                    from_id TEXT,
                    from_alias TEXT,
                    from_id_raw INTEGER,
                    from_wacn INTEGER,
                    from_system INTEGER,
                    from_radio INTEGER,
                    from_is_roaming INTEGER,
                    to_id TEXT,
                    to_alias TEXT,
                    to_id_raw INTEGER,
                    to_wacn INTEGER,
                    to_system INTEGER,
                    to_talkgroup INTEGER,
                    talkgroup_raw INTEGER,
                    patch_group TEXT,
                    patched_talkgroups TEXT,
                    talker_alias TEXT,
                    esn TEXT,
                    unit_status INTEGER,
                    user_status INTEGER,
                    telephone_number TEXT,
                    latitude REAL,
                    longitude REAL,
                    channel_number TEXT,
                    frequency_hz INTEGER,
                    uplink_frequency_hz INTEGER,
                    timeslot INTEGER,
                    is_tdma INTEGER,
                    timeslot_count INTEGER,
                    wacn INTEGER,
                    system_id INTEGER,
                    site_id INTEGER,
                    rfss INTEGER,
                    nac INTEGER,
                    lra INTEGER,
                    dmr_network INTEGER,
                    dmr_site INTEGER,
                    system_name TEXT,
                    site_name TEXT,
                    details TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Encrypted events table with comprehensive metadata
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS encrypted_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    time_end INTEGER,
                    channel_name TEXT,
                    event_type TEXT,
                    protocol TEXT,
                    duration_ms INTEGER,
                    encryption_algorithm TEXT,
                    algorithm_id INTEGER,
                    key_id INTEGER,
                    from_id TEXT,
                    from_alias TEXT,
                    from_id_raw INTEGER,
                    from_wacn INTEGER,
                    from_system INTEGER,
                    from_radio INTEGER,
                    from_is_roaming INTEGER,
                    to_id TEXT,
                    to_alias TEXT,
                    to_id_raw INTEGER,
                    to_wacn INTEGER,
                    to_system INTEGER,
                    to_talkgroup INTEGER,
                    talkgroup TEXT,
                    talkgroup_raw INTEGER,
                    patch_group TEXT,
                    patched_talkgroups TEXT,
                    talker_alias TEXT,
                    esn TEXT,
                    unit_status INTEGER,
                    user_status INTEGER,
                    telephone_number TEXT,
                    latitude REAL,
                    longitude REAL,
                    channel_number TEXT,
                    frequency_hz INTEGER,
                    downlink_frequency_hz INTEGER,
                    uplink_frequency_hz INTEGER,
                    timeslot INTEGER,
                    is_tdma INTEGER,
                    timeslot_count INTEGER,
                    wacn INTEGER,
                    system_id INTEGER,
                    site_id INTEGER,
                    rfss INTEGER,
                    nac INTEGER,
                    lra INTEGER,
                    dmr_network INTEGER,
                    dmr_site INTEGER,
                    system_name TEXT,
                    site_name TEXT,
                    network_id TEXT,
                    details TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Messages table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    channel_name TEXT,
                    protocol TEXT,
                    timeslot INTEGER,
                    wacn INTEGER,
                    system_id INTEGER,
                    site_id INTEGER,
                    rfss INTEGER,
                    nac INTEGER,
                    lra INTEGER,
                    message_type TEXT,
                    message_text TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // P25 Network/Site Status table (from RFSS Status Broadcasts)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS p25_network_status (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    channel_name TEXT,
                    wacn INTEGER,
                    system_id INTEGER,
                    rfss INTEGER,
                    site_id INTEGER,
                    lra INTEGER,
                    nac INTEGER,
                    active_network_connection INTEGER,
                    system_service_class TEXT,
                    control_channel TEXT,
                    control_channel_freq_hz INTEGER,
                    message_type TEXT,
                    raw_message TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // P25 Adjacent/Neighbor Sites table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS p25_adjacent_sites (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    channel_name TEXT,
                    neighbor_rfss INTEGER,
                    neighbor_site_id INTEGER,
                    neighbor_lra INTEGER,
                    neighbor_system_id INTEGER,
                    neighbor_channel TEXT,
                    neighbor_channel_freq_hz INTEGER,
                    system_service_class TEXT,
                    message_type TEXT,
                    raw_message TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // P25 Base Station ID table (Motorola specific)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS p25_base_stations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    channel_name TEXT,
                    wacn INTEGER,
                    system_id INTEGER,
                    base_station_id TEXT,
                    transmit_offset_mhz REAL,
                    channel TEXT,
                    message_type TEXT,
                    raw_message TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // P25 Unit Registrations table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS p25_registrations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    channel_name TEXT,
                    registration_type TEXT,
                    radio_id INTEGER,
                    wacn INTEGER,
                    system_id INTEGER,
                    source_id INTEGER,
                    source_address INTEGER,
                    lra INTEGER,
                    site_id INTEGER,
                    rfss INTEGER,
                    response_status TEXT,
                    message_type TEXT,
                    raw_message TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // P25 Group Affiliations table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS p25_affiliations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    channel_name TEXT,
                    affiliation_type TEXT,
                    radio_id INTEGER,
                    talkgroup_id INTEGER,
                    wacn INTEGER,
                    system_id INTEGER,
                    site_id INTEGER,
                    rfss INTEGER,
                    nac INTEGER,
                    lra INTEGER,
                    announcement_group INTEGER,
                    global_affiliation INTEGER,
                    response_status TEXT,
                    message_type TEXT,
                    raw_message TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // P25 Channel Grants table (voice and data channel assignments)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS p25_channel_grants (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    channel_name TEXT,
                    grant_type TEXT,
                    source_radio_id INTEGER,
                    target_id INTEGER,
                    target_type TEXT,
                    talkgroup_id INTEGER,
                    wacn INTEGER,
                    system_id INTEGER,
                    site_id INTEGER,
                    rfss INTEGER,
                    nac INTEGER,
                    lra INTEGER,
                    channel TEXT,
                    frequency_hz INTEGER,
                    timeslot INTEGER,
                    is_encrypted INTEGER,
                    is_emergency INTEGER,
                    service_options TEXT,
                    message_type TEXT,
                    raw_message TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // P25 Status Messages table (status updates, message updates, call alerts)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS p25_status_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    channel_name TEXT,
                    message_type TEXT,
                    source_radio_id INTEGER,
                    target_radio_id INTEGER,
                    talkgroup_id INTEGER,
                    unit_status INTEGER,
                    user_status INTEGER,
                    short_message TEXT,
                    wacn INTEGER,
                    system_id INTEGER,
                    site_id INTEGER,
                    rfss INTEGER,
                    nac INTEGER,
                    lra INTEGER,
                    raw_message TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // P25 GPS Locations table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS p25_gps_locations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    channel_name TEXT,
                    radio_id INTEGER,
                    talkgroup_id INTEGER,
                    latitude REAL,
                    longitude REAL,
                    altitude REAL,
                    speed REAL,
                    heading REAL,
                    location_source TEXT,
                    wacn INTEGER,
                    system_id INTEGER,
                    site_id INTEGER,
                    rfss INTEGER,
                    nac INTEGER,
                    raw_message TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // P25 Emergency Alarms table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS p25_emergency_alarms (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    channel_name TEXT,
                    alarm_type TEXT,
                    source_radio_id INTEGER,
                    talkgroup_id INTEGER,
                    wacn INTEGER,
                    system_id INTEGER,
                    site_id INTEGER,
                    rfss INTEGER,
                    nac INTEGER,
                    raw_message TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Create indexes for faster queries
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_events_timestamp ON decode_events(timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_events_from ON decode_events(from_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_events_to ON decode_events(to_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_events_wacn ON decode_events(wacn)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_events_system_id ON decode_events(system_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_events_site_id ON decode_events(site_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_events_nac ON decode_events(nac)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_events_from_id_raw ON decode_events(from_id_raw)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_events_to_id_raw ON decode_events(to_id_raw)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_encrypted_timestamp ON encrypted_events(timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_encrypted_algorithm ON encrypted_events(encryption_algorithm)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_encrypted_key ON encrypted_events(key_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_encrypted_talkgroup ON encrypted_events(talkgroup)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_encrypted_wacn ON encrypted_events(wacn)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_encrypted_system_id ON encrypted_events(system_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_encrypted_from_id_raw ON encrypted_events(from_id_raw)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_encrypted_to_id_raw ON encrypted_events(to_id_raw)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON messages(timestamp)");

            // Indexes for P25 network tables
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_network_status_timestamp ON p25_network_status(timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_network_status_site ON p25_network_status(site_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_network_status_rfss ON p25_network_status(rfss)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_network_status_system ON p25_network_status(system_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_adjacent_sites_timestamp ON p25_adjacent_sites(timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_adjacent_sites_neighbor ON p25_adjacent_sites(neighbor_site_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_base_stations_timestamp ON p25_base_stations(timestamp)");

            // Indexes for registration, affiliation, and channel grant tables
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_registrations_timestamp ON p25_registrations(timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_registrations_radio ON p25_registrations(radio_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_registrations_type ON p25_registrations(registration_type)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_affiliations_timestamp ON p25_affiliations(timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_affiliations_radio ON p25_affiliations(radio_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_affiliations_talkgroup ON p25_affiliations(talkgroup_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_channel_grants_timestamp ON p25_channel_grants(timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_channel_grants_source ON p25_channel_grants(source_radio_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_channel_grants_talkgroup ON p25_channel_grants(talkgroup_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_channel_grants_type ON p25_channel_grants(grant_type)");

            // Indexes for status messages, GPS locations, and emergency alarms
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_status_msgs_timestamp ON p25_status_messages(timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_status_msgs_source ON p25_status_messages(source_radio_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_status_msgs_type ON p25_status_messages(message_type)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_gps_timestamp ON p25_gps_locations(timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_gps_radio ON p25_gps_locations(radio_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_emergency_timestamp ON p25_emergency_alarms(timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_emergency_source ON p25_emergency_alarms(source_radio_id)");
        }
    }

    /**
     * Prepares SQL statements for insertion
     */
    private void prepareStatements() throws SQLException
    {
        mInsertEventStatement = mConnection.prepareStatement("""
            INSERT INTO decode_events (
                timestamp, time_end, channel_name, event_type, protocol, duration_ms,
                from_id, from_alias, from_id_raw, from_wacn, from_system, from_radio, from_is_roaming,
                to_id, to_alias, to_id_raw, to_wacn, to_system, to_talkgroup,
                talkgroup_raw, patch_group, patched_talkgroups,
                talker_alias, esn, unit_status, user_status, telephone_number,
                latitude, longitude,
                channel_number, frequency_hz, uplink_frequency_hz, timeslot, is_tdma, timeslot_count,
                wacn, system_id, site_id, rfss, nac, lra, dmr_network, dmr_site,
                system_name, site_name, details
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """);

        mInsertEncryptedEventStatement = mConnection.prepareStatement("""
            INSERT INTO encrypted_events (
                timestamp, time_end, channel_name, event_type, protocol, duration_ms,
                encryption_algorithm, algorithm_id, key_id,
                from_id, from_alias, from_id_raw, from_wacn, from_system, from_radio, from_is_roaming,
                to_id, to_alias, to_id_raw, to_wacn, to_system, to_talkgroup,
                talkgroup, talkgroup_raw, patch_group, patched_talkgroups,
                talker_alias, esn, unit_status, user_status, telephone_number,
                latitude, longitude,
                channel_number, frequency_hz, downlink_frequency_hz, uplink_frequency_hz,
                timeslot, is_tdma, timeslot_count,
                wacn, system_id, site_id, rfss, nac, lra, dmr_network, dmr_site,
                system_name, site_name, network_id, details
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """);

        mInsertMessageStatement = mConnection.prepareStatement("""
            INSERT INTO messages (
                timestamp, channel_name, protocol, timeslot, wacn, system_id, site_id,
                rfss, nac, lra, message_type, message_text
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """);

        mInsertNetworkStatusStatement = mConnection.prepareStatement("""
            INSERT INTO p25_network_status (
                timestamp, channel_name, wacn, system_id, rfss, site_id, lra, nac,
                active_network_connection, system_service_class, control_channel,
                control_channel_freq_hz, message_type, raw_message
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """);

        mInsertAdjacentSiteStatement = mConnection.prepareStatement("""
            INSERT INTO p25_adjacent_sites (
                timestamp, channel_name, neighbor_rfss, neighbor_site_id, neighbor_lra,
                neighbor_system_id, neighbor_channel, neighbor_channel_freq_hz,
                system_service_class, message_type, raw_message
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """);

        mInsertBaseStationStatement = mConnection.prepareStatement("""
            INSERT INTO p25_base_stations (
                timestamp, channel_name, wacn, system_id, base_station_id,
                transmit_offset_mhz, channel, message_type, raw_message
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """);

        mInsertRegistrationStatement = mConnection.prepareStatement("""
            INSERT INTO p25_registrations (
                timestamp, channel_name, registration_type, radio_id, wacn, system_id,
                source_id, source_address, lra, site_id, rfss, response_status,
                message_type, raw_message
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """);

        mInsertAffiliationStatement = mConnection.prepareStatement("""
            INSERT INTO p25_affiliations (
                timestamp, channel_name, affiliation_type, radio_id, talkgroup_id,
                wacn, system_id, site_id, rfss, nac, lra, announcement_group, global_affiliation,
                response_status, message_type, raw_message
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """);

        mInsertChannelGrantStatement = mConnection.prepareStatement("""
            INSERT INTO p25_channel_grants (
                timestamp, channel_name, grant_type, source_radio_id, target_id,
                target_type, talkgroup_id, wacn, system_id, site_id, rfss, nac, lra,
                channel, frequency_hz, timeslot, is_encrypted, is_emergency, service_options,
                message_type, raw_message
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """);

        mInsertStatusMessageStatement = mConnection.prepareStatement("""
            INSERT INTO p25_status_messages (
                timestamp, channel_name, message_type, source_radio_id, target_radio_id,
                talkgroup_id, unit_status, user_status, short_message,
                wacn, system_id, site_id, rfss, nac, lra, raw_message
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """);

        mInsertGpsLocationStatement = mConnection.prepareStatement("""
            INSERT INTO p25_gps_locations (
                timestamp, channel_name, radio_id, talkgroup_id, latitude, longitude,
                altitude, speed, heading, location_source,
                wacn, system_id, site_id, rfss, nac, raw_message
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """);

        mInsertEmergencyAlarmStatement = mConnection.prepareStatement("""
            INSERT INTO p25_emergency_alarms (
                timestamp, channel_name, alarm_type, source_radio_id, talkgroup_id,
                wacn, system_id, site_id, rfss, nac, raw_message
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """);
    }

    @Override
    public void receive(IDecodeEvent event)
    {
        if(!mRunning || event == null)
        {
            return;
        }

        try
        {
            if(isEncryptedEvent(event))
            {
                insertEncryptedEvent(event);
            }
            else
            {
                insertDecodeEvent(event);
            }
        }
        catch(SQLException e)
        {
            mLog.error("Error inserting event into database", e);
        }
    }

    /**
     * Checks if the event is an encrypted event type
     */
    private boolean isEncryptedEvent(IDecodeEvent event)
    {
        if(event.getEventType() == null)
        {
            return false;
        }

        if(ENCRYPTED_EVENTS.contains(event.getEventType()))
        {
            return true;
        }

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

    /**
     * Inserts a standard decode event
     */
    private void insertDecodeEvent(IDecodeEvent event) throws SQLException
    {
        IdentifierCollection ids = event.getIdentifierCollection();
        Protocol protocol = event.getProtocol();
        IChannelDescriptor descriptor = event.getChannelDescriptor();

        int idx = 1;

        // 1: timestamp
        mInsertEventStatement.setLong(idx++, event.getTimeStart());

        // 2: time_end
        setNullableLong(mInsertEventStatement, idx++, event.getTimeEnd());

        // 3: channel_name
        mInsertEventStatement.setString(idx++, mChannelName);

        // 4: event_type
        mInsertEventStatement.setString(idx++, event.getEventType() != null ? event.getEventType().toString() : null);

        // 5: protocol
        mInsertEventStatement.setString(idx++, protocol != null ? protocol.toString() : null);

        // 6: duration_ms
        mInsertEventStatement.setLong(idx++, event.getDuration());

        // From identifier
        String fromId = null;
        String fromAlias = null;
        if(ids != null)
        {
            Identifier from = ids.getFromIdentifier();
            if(from != null)
            {
                fromId = from.toString();
                fromAlias = getAlias(ids, from);
            }
        }

        // 7: from_id
        mInsertEventStatement.setString(idx++, fromId);

        // 8: from_alias
        mInsertEventStatement.setString(idx++, fromAlias);

        // 9: from_id_raw
        setNullableInt(mInsertEventStatement, idx++, getRawRadioId(ids));

        // Fully qualified FROM radio (roaming)
        FullyQualifiedRadioIdentifier fqFrom = getFullyQualifiedFromRadio(ids);

        // 10: from_wacn
        setNullableInt(mInsertEventStatement, idx++, fqFrom != null ? fqFrom.getWacn() : null);

        // 11: from_system
        setNullableInt(mInsertEventStatement, idx++, fqFrom != null ? fqFrom.getSystem() : null);

        // 12: from_radio
        setNullableInt(mInsertEventStatement, idx++, fqFrom != null ? fqFrom.getRadio() : null);

        // 13: from_is_roaming
        setNullableBoolean(mInsertEventStatement, idx++, fqFrom != null ? fqFrom.isAliased() : null);

        // To identifier
        String toId = null;
        String toAlias = null;
        if(ids != null)
        {
            Identifier to = ids.getToIdentifier();
            if(to != null)
            {
                toId = to.toString();
                toAlias = getAlias(ids, to);
            }
        }

        // 14: to_id
        mInsertEventStatement.setString(idx++, toId);

        // 15: to_alias
        mInsertEventStatement.setString(idx++, toAlias);

        // 16: to_id_raw
        setNullableInt(mInsertEventStatement, idx++, getRawTalkgroupId(ids));

        // Fully qualified TO talkgroup (roaming)
        FullyQualifiedTalkgroupIdentifier fqTo = getFullyQualifiedToTalkgroup(ids);

        // 17: to_wacn
        setNullableInt(mInsertEventStatement, idx++, fqTo != null ? fqTo.getWacn() : null);

        // 18: to_system
        setNullableInt(mInsertEventStatement, idx++, fqTo != null ? fqTo.getSystem() : null);

        // 19: to_talkgroup
        setNullableInt(mInsertEventStatement, idx++, fqTo != null ? fqTo.getTalkgroup() : null);

        // 20: talkgroup_raw
        setNullableInt(mInsertEventStatement, idx++, getTalkgroupRaw(ids));

        // 21: patch_group
        mInsertEventStatement.setString(idx++, getPatchGroupString(ids));

        // 22: patched_talkgroups
        mInsertEventStatement.setString(idx++, getPatchedTalkgroups(ids));

        // 23: talker_alias
        mInsertEventStatement.setString(idx++, getTalkerAlias(ids));

        // 24: esn
        mInsertEventStatement.setString(idx++, getEsn(ids));

        // 25: unit_status
        setNullableInt(mInsertEventStatement, idx++, getUnitStatus(ids));

        // 26: user_status
        setNullableInt(mInsertEventStatement, idx++, getUserStatus(ids));

        // 27: telephone_number
        mInsertEventStatement.setString(idx++, getTelephoneNumber(ids));

        // 28: latitude
        setNullableDouble(mInsertEventStatement, idx++, getLatitude(ids));

        // 29: longitude
        setNullableDouble(mInsertEventStatement, idx++, getLongitude(ids));

        // 30: channel_number
        mInsertEventStatement.setString(idx++, descriptor != null ? descriptor.toString() : null);

        // 31: frequency_hz
        long frequency = 0;
        if(descriptor != null)
        {
            frequency = descriptor.getDownlinkFrequency();
        }
        mInsertEventStatement.setLong(idx++, frequency);

        // 32: uplink_frequency_hz
        setNullableLong(mInsertEventStatement, idx++, getUplinkFrequency(descriptor));

        // 33: timeslot
        mInsertEventStatement.setInt(idx++, event.hasTimeslot() ? event.getTimeslot() : 0);

        // 34: is_tdma
        setNullableBoolean(mInsertEventStatement, idx++, isTdma(descriptor));

        // 35: timeslot_count
        setNullableInt(mInsertEventStatement, idx++, getTimeslotCount(descriptor));

        // Network identifiers (P25)
        // 36: wacn
        setNullableInt(mInsertEventStatement, idx++, getWacn(ids));

        // 37: system_id
        setNullableInt(mInsertEventStatement, idx++, getSystemId(ids));

        // 38: site_id
        setNullableInt(mInsertEventStatement, idx++, getSiteId(ids));

        // 39: rfss
        setNullableInt(mInsertEventStatement, idx++, getRfss(ids));

        // 40: nac
        setNullableInt(mInsertEventStatement, idx++, getNac(ids));

        // 41: lra
        setNullableInt(mInsertEventStatement, idx++, getLra(ids));

        // 42: dmr_network
        setNullableInt(mInsertEventStatement, idx++, getDmrNetwork(ids, protocol));

        // 43: dmr_site
        setNullableInt(mInsertEventStatement, idx++, getDmrSite(ids, protocol));

        // System and Site names (configuration)
        String system = getIdentifierValue(ids, IdentifierClass.CONFIGURATION, Form.SYSTEM);
        String site = getIdentifierValue(ids, IdentifierClass.CONFIGURATION, Form.SITE);

        // 44: system_name
        mInsertEventStatement.setString(idx++, system);

        // 45: site_name
        mInsertEventStatement.setString(idx++, site);

        // 46: details
        mInsertEventStatement.setString(idx++, event.getDetails());

        mInsertEventStatement.executeUpdate();
    }

    /**
     * Inserts an encrypted event with additional encryption metadata
     */
    private void insertEncryptedEvent(IDecodeEvent event) throws SQLException
    {
        IdentifierCollection ids = event.getIdentifierCollection();
        Protocol protocol = event.getProtocol();
        IChannelDescriptor descriptor = event.getChannelDescriptor();

        int idx = 1;

        // 1: timestamp
        mInsertEncryptedEventStatement.setLong(idx++, event.getTimeStart());

        // 2: time_end
        setNullableLong(mInsertEncryptedEventStatement, idx++, event.getTimeEnd());

        // 3: channel_name
        mInsertEncryptedEventStatement.setString(idx++, mChannelName);

        // 4: event_type
        mInsertEncryptedEventStatement.setString(idx++, event.getEventType() != null ? event.getEventType().toString() : null);

        // 5: protocol
        mInsertEncryptedEventStatement.setString(idx++, protocol != null ? protocol.toString() : null);

        // 6: duration_ms
        mInsertEncryptedEventStatement.setLong(idx++, event.getDuration());

        // Encryption details
        String algorithmName = null;
        int algorithmId = 0;
        int keyId = 0;

        if(ids != null)
        {
            Identifier encryptionId = ids.getEncryptionIdentifier();
            if(encryptionId instanceof EncryptionKeyIdentifier)
            {
                EncryptionKey key = ((EncryptionKeyIdentifier)encryptionId).getValue();
                if(key != null)
                {
                    algorithmId = key.getAlgorithm();
                    keyId = key.getKey();

                    if(key instanceof APCO25EncryptionKey)
                    {
                        Encryption enc = ((APCO25EncryptionKey)key).getEncryptionAlgorithm();
                        algorithmName = enc != null ? enc.toString() : "UNKNOWN";
                    }
                    else
                    {
                        algorithmName = getAlgorithmName(algorithmId);
                    }
                }
            }
        }

        // 7: encryption_algorithm
        mInsertEncryptedEventStatement.setString(idx++, algorithmName);

        // 8: algorithm_id
        mInsertEncryptedEventStatement.setInt(idx++, algorithmId);

        // 9: key_id
        mInsertEncryptedEventStatement.setInt(idx++, keyId);

        // From identifier
        String fromId = null;
        String fromAlias = null;
        if(ids != null)
        {
            Identifier from = ids.getFromIdentifier();
            if(from != null)
            {
                fromId = from.toString();
                fromAlias = getAlias(ids, from);
            }
        }

        // 10: from_id
        mInsertEncryptedEventStatement.setString(idx++, fromId);

        // 11: from_alias
        mInsertEncryptedEventStatement.setString(idx++, fromAlias);

        // 12: from_id_raw
        setNullableInt(mInsertEncryptedEventStatement, idx++, getRawRadioId(ids));

        // Fully qualified FROM radio (roaming)
        FullyQualifiedRadioIdentifier fqFrom = getFullyQualifiedFromRadio(ids);

        // 13: from_wacn
        setNullableInt(mInsertEncryptedEventStatement, idx++, fqFrom != null ? fqFrom.getWacn() : null);

        // 14: from_system
        setNullableInt(mInsertEncryptedEventStatement, idx++, fqFrom != null ? fqFrom.getSystem() : null);

        // 15: from_radio
        setNullableInt(mInsertEncryptedEventStatement, idx++, fqFrom != null ? fqFrom.getRadio() : null);

        // 16: from_is_roaming
        setNullableBoolean(mInsertEncryptedEventStatement, idx++, fqFrom != null ? fqFrom.isAliased() : null);

        // To identifier
        String toId = null;
        String toAlias = null;
        String talkgroup = null;
        if(ids != null)
        {
            Identifier to = ids.getToIdentifier();
            if(to != null)
            {
                toId = to.toString();
                toAlias = getAlias(ids, to);
                if(to.getForm() == Form.TALKGROUP || to.getForm() == Form.PATCH_GROUP)
                {
                    talkgroup = to.toString();
                }
            }
        }

        // 17: to_id
        mInsertEncryptedEventStatement.setString(idx++, toId);

        // 18: to_alias
        mInsertEncryptedEventStatement.setString(idx++, toAlias);

        // 19: to_id_raw
        setNullableInt(mInsertEncryptedEventStatement, idx++, getRawTalkgroupId(ids));

        // Fully qualified TO talkgroup (roaming)
        FullyQualifiedTalkgroupIdentifier fqTo = getFullyQualifiedToTalkgroup(ids);

        // 20: to_wacn
        setNullableInt(mInsertEncryptedEventStatement, idx++, fqTo != null ? fqTo.getWacn() : null);

        // 21: to_system
        setNullableInt(mInsertEncryptedEventStatement, idx++, fqTo != null ? fqTo.getSystem() : null);

        // 22: to_talkgroup
        setNullableInt(mInsertEncryptedEventStatement, idx++, fqTo != null ? fqTo.getTalkgroup() : null);

        // 23: talkgroup
        mInsertEncryptedEventStatement.setString(idx++, talkgroup);

        // 24: talkgroup_raw
        setNullableInt(mInsertEncryptedEventStatement, idx++, getTalkgroupRaw(ids));

        // 25: patch_group
        mInsertEncryptedEventStatement.setString(idx++, getPatchGroupString(ids));

        // 26: patched_talkgroups
        mInsertEncryptedEventStatement.setString(idx++, getPatchedTalkgroups(ids));

        // 27: talker_alias
        mInsertEncryptedEventStatement.setString(idx++, getTalkerAlias(ids));

        // 28: esn
        mInsertEncryptedEventStatement.setString(idx++, getEsn(ids));

        // 29: unit_status
        setNullableInt(mInsertEncryptedEventStatement, idx++, getUnitStatus(ids));

        // 30: user_status
        setNullableInt(mInsertEncryptedEventStatement, idx++, getUserStatus(ids));

        // 31: telephone_number
        mInsertEncryptedEventStatement.setString(idx++, getTelephoneNumber(ids));

        // 32: latitude
        setNullableDouble(mInsertEncryptedEventStatement, idx++, getLatitude(ids));

        // 33: longitude
        setNullableDouble(mInsertEncryptedEventStatement, idx++, getLongitude(ids));

        // 34: channel_number
        mInsertEncryptedEventStatement.setString(idx++, descriptor != null ? descriptor.toString() : null);

        // 35: frequency_hz
        long frequency = 0;
        if(descriptor != null)
        {
            frequency = descriptor.getDownlinkFrequency();
        }
        mInsertEncryptedEventStatement.setLong(idx++, frequency);

        // 36: downlink_frequency_hz from configuration
        long downlinkFreq = 0;
        if(ids != null)
        {
            Identifier freqId = ids.getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL_FREQUENCY, Role.ANY);
            if(freqId instanceof FrequencyConfigurationIdentifier)
            {
                downlinkFreq = ((FrequencyConfigurationIdentifier)freqId).getValue();
            }
        }
        mInsertEncryptedEventStatement.setLong(idx++, downlinkFreq);

        // 37: uplink_frequency_hz
        setNullableLong(mInsertEncryptedEventStatement, idx++, getUplinkFrequency(descriptor));

        // 38: timeslot
        mInsertEncryptedEventStatement.setInt(idx++, event.hasTimeslot() ? event.getTimeslot() : 0);

        // 39: is_tdma
        setNullableBoolean(mInsertEncryptedEventStatement, idx++, isTdma(descriptor));

        // 40: timeslot_count
        setNullableInt(mInsertEncryptedEventStatement, idx++, getTimeslotCount(descriptor));

        // Network identifiers (P25)
        // 41: wacn
        setNullableInt(mInsertEncryptedEventStatement, idx++, getWacn(ids));

        // 42: system_id
        setNullableInt(mInsertEncryptedEventStatement, idx++, getSystemId(ids));

        // 43: site_id
        setNullableInt(mInsertEncryptedEventStatement, idx++, getSiteId(ids));

        // 44: rfss
        setNullableInt(mInsertEncryptedEventStatement, idx++, getRfss(ids));

        // 45: nac
        setNullableInt(mInsertEncryptedEventStatement, idx++, getNac(ids));

        // 46: lra
        setNullableInt(mInsertEncryptedEventStatement, idx++, getLra(ids));

        // 47: dmr_network
        setNullableInt(mInsertEncryptedEventStatement, idx++, getDmrNetwork(ids, protocol));

        // 48: dmr_site
        setNullableInt(mInsertEncryptedEventStatement, idx++, getDmrSite(ids, protocol));

        // System, Site, Network names (configuration)
        String system = getIdentifierValue(ids, IdentifierClass.CONFIGURATION, Form.SYSTEM);
        String site = getIdentifierValue(ids, IdentifierClass.CONFIGURATION, Form.SITE);
        String network = getIdentifierValue(ids, IdentifierClass.NETWORK, Form.WACN);
        if(network == null)
        {
            network = getIdentifierValue(ids, IdentifierClass.NETWORK, Form.NETWORK);
        }

        // 49: system_name
        mInsertEncryptedEventStatement.setString(idx++, system);

        // 50: site_name
        mInsertEncryptedEventStatement.setString(idx++, site);

        // 51: network_id
        mInsertEncryptedEventStatement.setString(idx++, network);

        // 52: details
        mInsertEncryptedEventStatement.setString(idx++, event.getDetails());

        mInsertEncryptedEventStatement.executeUpdate();
    }

    /**
     * Logs a decoded message to the database
     */
    public void logMessage(IMessage message)
    {
        if(!mRunning || message == null)
        {
            return;
        }

        try
        {
            int idx = 1;
            mInsertMessageStatement.setLong(idx++, message.getTimestamp());
            mInsertMessageStatement.setString(idx++, mChannelName);
            mInsertMessageStatement.setString(idx++, message.getProtocol() != null ? message.getProtocol().toString() : null);
            mInsertMessageStatement.setInt(idx++, message.getTimeslot());
            // Site/base station info from cached values
            setNullableInt(mInsertMessageStatement, idx++, mCurrentWacn);
            setNullableInt(mInsertMessageStatement, idx++, mCurrentSystemId);
            setNullableInt(mInsertMessageStatement, idx++, mCurrentSiteId);
            setNullableInt(mInsertMessageStatement, idx++, mCurrentRfss);
            setNullableInt(mInsertMessageStatement, idx++, mCurrentNac);
            setNullableInt(mInsertMessageStatement, idx++, mCurrentLra);
            mInsertMessageStatement.setString(idx++, message.getClass().getSimpleName());
            mInsertMessageStatement.setString(idx++, message.toString());
            mInsertMessageStatement.executeUpdate();

            // Process P25 TSBK messages for network status
            if(message instanceof TSBKMessage)
            {
                processTsbkMessage((TSBKMessage)message);
            }
            // Process P25 Phase 2 MAC messages
            else if(message instanceof MacMessage)
            {
                processMacMessage((MacMessage)message);
            }
        }
        catch(SQLException e)
        {
            mLog.error("Error inserting message into database", e);
        }
    }

    /**
     * Processes P25 TSBK messages to extract network/site information, registrations,
     * affiliations, and channel grants
     */
    private void processTsbkMessage(TSBKMessage tsbk)
    {
        try
        {
            // Network/Site Status messages
            if(tsbk instanceof RFSSStatusBroadcast)
            {
                processRfssStatusBroadcast((RFSSStatusBroadcast)tsbk);
            }
            else if(tsbk instanceof NetworkStatusBroadcast)
            {
                processNetworkStatusBroadcast((NetworkStatusBroadcast)tsbk);
            }
            else if(tsbk instanceof AdjacentStatusBroadcast)
            {
                processAdjacentStatusBroadcast((AdjacentStatusBroadcast)tsbk);
            }
            else if(tsbk instanceof MotorolaBaseStationId)
            {
                processBaseStationId((MotorolaBaseStationId)tsbk);
            }
            // Registration messages
            else if(tsbk instanceof UnitRegistrationResponse)
            {
                processUnitRegistrationResponse((UnitRegistrationResponse)tsbk);
            }
            else if(tsbk instanceof UnitRegistrationRequest)
            {
                processUnitRegistrationRequest((UnitRegistrationRequest)tsbk);
            }
            else if(tsbk instanceof LocationRegistrationResponse)
            {
                processLocationRegistrationResponse((LocationRegistrationResponse)tsbk);
            }
            else if(tsbk instanceof LocationRegistrationRequest)
            {
                processLocationRegistrationRequest((LocationRegistrationRequest)tsbk);
            }
            // Affiliation messages
            else if(tsbk instanceof GroupAffiliationResponse)
            {
                processGroupAffiliationResponse((GroupAffiliationResponse)tsbk);
            }
            else if(tsbk instanceof GroupAffiliationRequest)
            {
                processGroupAffiliationRequest((GroupAffiliationRequest)tsbk);
            }
            // Channel grant messages
            else if(tsbk instanceof GroupVoiceChannelGrant)
            {
                processGroupVoiceChannelGrant((GroupVoiceChannelGrant)tsbk);
            }
            else if(tsbk instanceof GroupVoiceChannelGrantUpdate)
            {
                processGroupVoiceChannelGrantUpdate((GroupVoiceChannelGrantUpdate)tsbk);
            }
            else if(tsbk instanceof UnitToUnitVoiceChannelGrant)
            {
                processUnitToUnitVoiceChannelGrant((UnitToUnitVoiceChannelGrant)tsbk);
            }
            else if(tsbk instanceof TelephoneInterconnectVoiceChannelGrant)
            {
                processTelephoneInterconnectGrant((TelephoneInterconnectVoiceChannelGrant)tsbk);
            }
            else if(tsbk instanceof SNDCPDataChannelGrant)
            {
                processSndcpDataChannelGrant((SNDCPDataChannelGrant)tsbk);
            }
            else if(tsbk instanceof MotorolaGroupRegroupChannelGrant)
            {
                processMotorolaGroupRegroupChannelGrant((MotorolaGroupRegroupChannelGrant)tsbk);
            }
            // Status and message update messages
            else if(tsbk instanceof StatusUpdate)
            {
                processStatusUpdate((StatusUpdate)tsbk);
            }
            else if(tsbk instanceof StatusQuery)
            {
                processStatusQuery((StatusQuery)tsbk);
            }
            else if(tsbk instanceof StatusUpdateRequest)
            {
                processStatusUpdateRequest((StatusUpdateRequest)tsbk);
            }
            else if(tsbk instanceof MessageUpdate)
            {
                processMessageUpdate((MessageUpdate)tsbk);
            }
            else if(tsbk instanceof CallAlert)
            {
                processCallAlert((CallAlert)tsbk);
            }
            else if(tsbk instanceof CallAlertRequest)
            {
                processCallAlertRequest((CallAlertRequest)tsbk);
            }
            // Emergency alarm messages
            else if(tsbk instanceof EmergencyAlarmRequest)
            {
                processEmergencyAlarmRequest((EmergencyAlarmRequest)tsbk);
            }
            else if(tsbk instanceof MotorolaEmergencyAlarmActivation)
            {
                processMotorolaEmergencyAlarm((MotorolaEmergencyAlarmActivation)tsbk);
            }
            // Extended function and response messages
            else if(tsbk instanceof ExtendedFunctionCommand)
            {
                processExtendedFunctionCommand((ExtendedFunctionCommand)tsbk);
            }
            else if(tsbk instanceof RadioUnitMonitorCommand)
            {
                processRadioUnitMonitorCommand((RadioUnitMonitorCommand)tsbk);
            }
            else if(tsbk instanceof AcknowledgeResponse)
            {
                processAcknowledgeResponse((AcknowledgeResponse)tsbk);
            }
            else if(tsbk instanceof DenyResponse)
            {
                processDenyResponse((DenyResponse)tsbk);
            }
            else if(tsbk instanceof QueuedResponse)
            {
                processQueuedResponse((QueuedResponse)tsbk);
            }
        }
        catch(SQLException e)
        {
            mLog.error("Error processing TSBK message", e);
        }
    }

    /**
     * Processes RFSS Status Broadcast - current site information
     */
    private void processRfssStatusBroadcast(RFSSStatusBroadcast rfss) throws SQLException
    {
        // Update cached current site info - extract values from Identifiers
        Identifier siteId = rfss.getSite();
        if(siteId != null)
        {
            mCurrentSiteId = extractIntValue(siteId);
        }
        Identifier rfssId = rfss.getRfss();
        if(rfssId != null)
        {
            mCurrentRfss = extractIntValue(rfssId);
        }
        Identifier systemId = rfss.getSystem();
        if(systemId != null)
        {
            mCurrentSystemId = extractIntValue(systemId);
        }
        Identifier lraId = rfss.getLocationRegistrationArea();
        if(lraId != null)
        {
            mCurrentLra = extractIntValue(lraId);
        }
        Identifier nacId = rfss.getNAC();
        if(nacId != null)
        {
            mCurrentNac = extractIntValue(nacId);
        }

        int idx = 1;
        // 1: timestamp
        mInsertNetworkStatusStatement.setLong(idx++, rfss.getTimestamp());
        // 2: channel_name
        mInsertNetworkStatusStatement.setString(idx++, mChannelName);
        // 3: wacn (not in RFSS Status, set null)
        mInsertNetworkStatusStatement.setNull(idx++, Types.INTEGER);
        // 4: system_id
        setNullableInt(mInsertNetworkStatusStatement, idx++, mCurrentSystemId);
        // 5: rfss
        setNullableInt(mInsertNetworkStatusStatement, idx++, mCurrentRfss);
        // 6: site_id
        setNullableInt(mInsertNetworkStatusStatement, idx++, mCurrentSiteId);
        // 7: lra
        setNullableInt(mInsertNetworkStatusStatement, idx++, mCurrentLra);
        // 8: nac
        setNullableInt(mInsertNetworkStatusStatement, idx++, mCurrentNac);
        // 9: active_network_connection
        mInsertNetworkStatusStatement.setInt(idx++, rfss.isActiveNetworkConnectionToRfssControllerSite() ? 1 : 0);
        // 10: system_service_class
        mInsertNetworkStatusStatement.setString(idx++, rfss.getSystemServiceClass() != null ? rfss.getSystemServiceClass().toString() : null);
        // 11: control_channel
        mInsertNetworkStatusStatement.setString(idx++, rfss.getChannel() != null ? rfss.getChannel().toString() : null);
        // 12: control_channel_freq_hz
        setNullableLong(mInsertNetworkStatusStatement, idx++, rfss.getChannel() != null ? rfss.getChannel().getDownlinkFrequency() : null);
        // 13: message_type
        mInsertNetworkStatusStatement.setString(idx++, "RFSS_STATUS_BROADCAST");
        // 14: raw_message
        mInsertNetworkStatusStatement.setString(idx++, rfss.toString());

        mInsertNetworkStatusStatement.executeUpdate();
    }

    /**
     * Processes Network Status Broadcast - WACN and system info
     */
    private void processNetworkStatusBroadcast(NetworkStatusBroadcast nsb) throws SQLException
    {
        // Update cached WACN - extract values from Identifiers
        Identifier wacnId = nsb.getWacn();
        if(wacnId != null)
        {
            mCurrentWacn = extractIntValue(wacnId);
        }
        Identifier systemId = nsb.getSystem();
        if(systemId != null)
        {
            mCurrentSystemId = extractIntValue(systemId);
        }
        Identifier nacId = nsb.getNAC();
        if(nacId != null)
        {
            mCurrentNac = extractIntValue(nacId);
        }

        int idx = 1;
        // 1: timestamp
        mInsertNetworkStatusStatement.setLong(idx++, nsb.getTimestamp());
        // 2: channel_name
        mInsertNetworkStatusStatement.setString(idx++, mChannelName);
        // 3: wacn
        setNullableInt(mInsertNetworkStatusStatement, idx++, mCurrentWacn);
        // 4: system_id
        setNullableInt(mInsertNetworkStatusStatement, idx++, mCurrentSystemId);
        // 5: rfss (not in Network Status)
        mInsertNetworkStatusStatement.setNull(idx++, Types.INTEGER);
        // 6: site_id (not in Network Status)
        mInsertNetworkStatusStatement.setNull(idx++, Types.INTEGER);
        // 7: lra
        Identifier lraId = nsb.getLocationRegistrationArea();
        if(lraId != null)
        {
            mCurrentLra = extractIntValue(lraId);
        }
        setNullableInt(mInsertNetworkStatusStatement, idx++, mCurrentLra);
        // 8: nac
        setNullableInt(mInsertNetworkStatusStatement, idx++, mCurrentNac);
        // 9: active_network_connection
        mInsertNetworkStatusStatement.setNull(idx++, Types.INTEGER);
        // 10: system_service_class
        mInsertNetworkStatusStatement.setString(idx++, nsb.getSystemServiceClass() != null ? nsb.getSystemServiceClass().toString() : null);
        // 11: control_channel
        mInsertNetworkStatusStatement.setString(idx++, nsb.getChannel() != null ? nsb.getChannel().toString() : null);
        // 12: control_channel_freq_hz
        setNullableLong(mInsertNetworkStatusStatement, idx++, nsb.getChannel() != null ? nsb.getChannel().getDownlinkFrequency() : null);
        // 13: message_type
        mInsertNetworkStatusStatement.setString(idx++, "NETWORK_STATUS_BROADCAST");
        // 14: raw_message
        mInsertNetworkStatusStatement.setString(idx++, nsb.toString());

        mInsertNetworkStatusStatement.executeUpdate();
    }

    /**
     * Processes Adjacent Status Broadcast - neighbor site information
     */
    private void processAdjacentStatusBroadcast(AdjacentStatusBroadcast asb) throws SQLException
    {
        int idx = 1;
        // 1: timestamp
        mInsertAdjacentSiteStatement.setLong(idx++, asb.getTimestamp());
        // 2: channel_name
        mInsertAdjacentSiteStatement.setString(idx++, mChannelName);
        // 3: neighbor_rfss
        setNullableInt(mInsertAdjacentSiteStatement, idx++, extractIntValue(asb.getRfss()));
        // 4: neighbor_site_id
        setNullableInt(mInsertAdjacentSiteStatement, idx++, extractIntValue(asb.getSite()));
        // 5: neighbor_lra
        setNullableInt(mInsertAdjacentSiteStatement, idx++, extractIntValue(asb.getLocationRegistrationArea()));
        // 6: neighbor_system_id
        setNullableInt(mInsertAdjacentSiteStatement, idx++, extractIntValue(asb.getSystem()));
        // 7: neighbor_channel
        mInsertAdjacentSiteStatement.setString(idx++, asb.getChannel() != null ? asb.getChannel().toString() : null);
        // 8: neighbor_channel_freq_hz
        setNullableLong(mInsertAdjacentSiteStatement, idx++, asb.getChannel() != null ? asb.getChannel().getDownlinkFrequency() : null);
        // 9: system_service_class
        mInsertAdjacentSiteStatement.setString(idx++, asb.getSystemServiceClass() != null ? asb.getSystemServiceClass().toString() : null);
        // 10: message_type
        mInsertAdjacentSiteStatement.setString(idx++, "ADJACENT_STATUS_BROADCAST");
        // 11: raw_message
        mInsertAdjacentSiteStatement.setString(idx++, asb.toString());

        mInsertAdjacentSiteStatement.executeUpdate();
    }

    /**
     * Processes Motorola Base Station ID message
     * Note: MotorolaBaseStationId only provides CWID (call sign) and channel info.
     * WACN/System are taken from cached values obtained from Network Status broadcasts.
     */
    private void processBaseStationId(MotorolaBaseStationId bsid) throws SQLException
    {
        int idx = 1;
        // 1: timestamp
        mInsertBaseStationStatement.setLong(idx++, bsid.getTimestamp());
        // 2: channel_name
        mInsertBaseStationStatement.setString(idx++, mChannelName);
        // 3: wacn (from cached Network Status)
        setNullableInt(mInsertBaseStationStatement, idx++, mCurrentWacn);
        // 4: system_id (from cached Network Status)
        setNullableInt(mInsertBaseStationStatement, idx++, mCurrentSystemId);
        // 5: base_station_id (CWID - call sign identifier)
        mInsertBaseStationStatement.setString(idx++, bsid.getCWID());
        // 6: transmit_offset_mhz (not available in this message type)
        mInsertBaseStationStatement.setNull(idx++, Types.REAL);
        // 7: channel
        mInsertBaseStationStatement.setString(idx++, bsid.getChannel() != null ? bsid.getChannel().toString() : null);
        // 8: message_type
        mInsertBaseStationStatement.setString(idx++, "MOTOROLA_BASE_STATION_ID");
        // 9: raw_message
        mInsertBaseStationStatement.setString(idx++, bsid.toString());

        mInsertBaseStationStatement.executeUpdate();
    }

    // ============================================================
    // Registration message processing
    // ============================================================

    /**
     * Processes Unit Registration Response
     */
    private void processUnitRegistrationResponse(UnitRegistrationResponse urr) throws SQLException
    {
        int idx = 1;
        mInsertRegistrationStatement.setLong(idx++, urr.getTimestamp());
        mInsertRegistrationStatement.setString(idx++, mChannelName);
        mInsertRegistrationStatement.setString(idx++, "UNIT_REGISTRATION_RESPONSE");

        // Extract radio ID from identifiers
        Integer radioId = null;
        for(Identifier id : urr.getIdentifiers())
        {
            if(id instanceof RadioIdentifier)
            {
                radioId = extractIntValue(id);
                break;
            }
        }
        setNullableInt(mInsertRegistrationStatement, idx++, radioId);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentSystemId);

        // Source ID and address - not directly available in this message type
        mInsertRegistrationStatement.setNull(idx++, Types.INTEGER); // source_id
        mInsertRegistrationStatement.setNull(idx++, Types.INTEGER); // source_address
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentLra);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentRfss);
        mInsertRegistrationStatement.setString(idx++, urr.getResponse() != null ? urr.getResponse().toString() : null);
        mInsertRegistrationStatement.setString(idx++, "UNIT_REGISTRATION_RESPONSE");
        mInsertRegistrationStatement.setString(idx++, urr.toString());

        mInsertRegistrationStatement.executeUpdate();
    }

    /**
     * Processes Unit Registration Request
     */
    private void processUnitRegistrationRequest(UnitRegistrationRequest urq) throws SQLException
    {
        int idx = 1;
        mInsertRegistrationStatement.setLong(idx++, urq.getTimestamp());
        mInsertRegistrationStatement.setString(idx++, mChannelName);
        mInsertRegistrationStatement.setString(idx++, "UNIT_REGISTRATION_REQUEST");

        // Extract radio ID from identifiers
        Integer radioId = null;
        for(Identifier id : urq.getIdentifiers())
        {
            if(id instanceof RadioIdentifier)
            {
                radioId = extractIntValue(id);
                break;
            }
        }
        setNullableInt(mInsertRegistrationStatement, idx++, radioId);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentSystemId);
        mInsertRegistrationStatement.setNull(idx++, Types.INTEGER); // source_id
        mInsertRegistrationStatement.setNull(idx++, Types.INTEGER); // source_address
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentLra);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentRfss);
        mInsertRegistrationStatement.setNull(idx++, Types.VARCHAR); // No response in request
        mInsertRegistrationStatement.setString(idx++, "UNIT_REGISTRATION_REQUEST");
        mInsertRegistrationStatement.setString(idx++, urq.toString());

        mInsertRegistrationStatement.executeUpdate();
    }

    /**
     * Processes Location Registration Response
     */
    private void processLocationRegistrationResponse(LocationRegistrationResponse lrr) throws SQLException
    {
        int idx = 1;
        mInsertRegistrationStatement.setLong(idx++, lrr.getTimestamp());
        mInsertRegistrationStatement.setString(idx++, mChannelName);
        mInsertRegistrationStatement.setString(idx++, "LOCATION_REGISTRATION_RESPONSE");

        // Extract radio ID from identifiers
        Integer radioId = null;
        for(Identifier id : lrr.getIdentifiers())
        {
            if(id instanceof RadioIdentifier)
            {
                radioId = extractIntValue(id);
                break;
            }
        }
        setNullableInt(mInsertRegistrationStatement, idx++, radioId);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentSystemId);
        mInsertRegistrationStatement.setNull(idx++, Types.INTEGER); // source_id
        mInsertRegistrationStatement.setNull(idx++, Types.INTEGER); // source_address
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentLra); // Use cached LRA
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentRfss); // Use cached RFSS
        mInsertRegistrationStatement.setString(idx++, lrr.getResponse() != null ? lrr.getResponse().toString() : null);
        mInsertRegistrationStatement.setString(idx++, "LOCATION_REGISTRATION_RESPONSE");
        mInsertRegistrationStatement.setString(idx++, lrr.toString());

        mInsertRegistrationStatement.executeUpdate();
    }

    /**
     * Processes Location Registration Request
     */
    private void processLocationRegistrationRequest(LocationRegistrationRequest lrq) throws SQLException
    {
        int idx = 1;
        mInsertRegistrationStatement.setLong(idx++, lrq.getTimestamp());
        mInsertRegistrationStatement.setString(idx++, mChannelName);
        mInsertRegistrationStatement.setString(idx++, "LOCATION_REGISTRATION_REQUEST");

        // Extract radio ID from identifiers
        Integer radioId = null;
        for(Identifier id : lrq.getIdentifiers())
        {
            if(id instanceof RadioIdentifier)
            {
                radioId = extractIntValue(id);
                break;
            }
        }
        setNullableInt(mInsertRegistrationStatement, idx++, radioId);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentSystemId);
        mInsertRegistrationStatement.setNull(idx++, Types.INTEGER); // source_id
        mInsertRegistrationStatement.setNull(idx++, Types.INTEGER); // source_address
        setNullableInt(mInsertRegistrationStatement, idx++, extractIntValue(lrq.getLocationRegistrationArea()));
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentRfss);
        mInsertRegistrationStatement.setNull(idx++, Types.VARCHAR); // No response in request
        mInsertRegistrationStatement.setString(idx++, "LOCATION_REGISTRATION_REQUEST");
        mInsertRegistrationStatement.setString(idx++, lrq.toString());

        mInsertRegistrationStatement.executeUpdate();
    }

    // ============================================================
    // Affiliation message processing
    // ============================================================

    /**
     * Processes Group Affiliation Response
     */
    private void processGroupAffiliationResponse(GroupAffiliationResponse gar) throws SQLException
    {
        int idx = 1;
        mInsertAffiliationStatement.setLong(idx++, gar.getTimestamp());
        mInsertAffiliationStatement.setString(idx++, mChannelName);
        mInsertAffiliationStatement.setString(idx++, "GROUP_AFFILIATION_RESPONSE");

        // Extract radio ID and talkgroup from identifiers
        Integer radioId = null;
        Integer talkgroupId = null;
        Integer announcementGroup = null;
        for(Identifier id : gar.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && radioId == null)
            {
                radioId = extractIntValue(id);
            }
            else if(id instanceof TalkgroupIdentifier)
            {
                if(id.getRole() == Role.TO)
                {
                    talkgroupId = extractIntValue(id);
                }
                else if(id.getRole() == Role.FROM)
                {
                    announcementGroup = extractIntValue(id);
                }
            }
        }
        setNullableInt(mInsertAffiliationStatement, idx++, radioId);
        setNullableInt(mInsertAffiliationStatement, idx++, talkgroupId);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentSystemId);
        // Site info
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentNac);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentLra);
        setNullableInt(mInsertAffiliationStatement, idx++, announcementGroup);
        mInsertAffiliationStatement.setInt(idx++, gar.isGlobalAffiliation() ? 1 : 0);
        mInsertAffiliationStatement.setString(idx++, gar.getAffiliationResponse() != null ? gar.getAffiliationResponse().toString() : null);
        mInsertAffiliationStatement.setString(idx++, "GROUP_AFFILIATION_RESPONSE");
        mInsertAffiliationStatement.setString(idx++, gar.toString());

        mInsertAffiliationStatement.executeUpdate();
    }

    /**
     * Processes Group Affiliation Request
     */
    private void processGroupAffiliationRequest(GroupAffiliationRequest gaq) throws SQLException
    {
        int idx = 1;
        mInsertAffiliationStatement.setLong(idx++, gaq.getTimestamp());
        mInsertAffiliationStatement.setString(idx++, mChannelName);
        mInsertAffiliationStatement.setString(idx++, "GROUP_AFFILIATION_REQUEST");

        // Extract radio ID and talkgroup from identifiers
        Integer radioId = null;
        Integer talkgroupId = null;
        for(Identifier id : gaq.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && radioId == null)
            {
                radioId = extractIntValue(id);
            }
            else if(id instanceof TalkgroupIdentifier && talkgroupId == null)
            {
                talkgroupId = extractIntValue(id);
            }
        }
        setNullableInt(mInsertAffiliationStatement, idx++, radioId);
        setNullableInt(mInsertAffiliationStatement, idx++, talkgroupId);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentSystemId);
        // Site info
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentNac);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentLra);
        mInsertAffiliationStatement.setNull(idx++, Types.INTEGER); // announcement_group
        mInsertAffiliationStatement.setNull(idx++, Types.INTEGER); // global_affiliation
        mInsertAffiliationStatement.setNull(idx++, Types.VARCHAR); // No response in request
        mInsertAffiliationStatement.setString(idx++, "GROUP_AFFILIATION_REQUEST");
        mInsertAffiliationStatement.setString(idx++, gaq.toString());

        mInsertAffiliationStatement.executeUpdate();
    }

    // ============================================================
    // Channel Grant message processing
    // ============================================================

    /**
     * Processes Group Voice Channel Grant
     */
    private void processGroupVoiceChannelGrant(GroupVoiceChannelGrant gvcg) throws SQLException
    {
        int idx = 1;
        mInsertChannelGrantStatement.setLong(idx++, gvcg.getTimestamp());
        mInsertChannelGrantStatement.setString(idx++, mChannelName);
        mInsertChannelGrantStatement.setString(idx++, "GROUP_VOICE");

        // Extract identifiers
        Integer sourceRadioId = null;
        Integer talkgroupId = null;
        for(Identifier id : gvcg.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && sourceRadioId == null)
            {
                sourceRadioId = extractIntValue(id);
            }
            else if(id instanceof TalkgroupIdentifier && talkgroupId == null)
            {
                talkgroupId = extractIntValue(id);
            }
        }
        setNullableInt(mInsertChannelGrantStatement, idx++, sourceRadioId);
        setNullableInt(mInsertChannelGrantStatement, idx++, talkgroupId); // target_id
        mInsertChannelGrantStatement.setString(idx++, "TALKGROUP"); // target_type
        setNullableInt(mInsertChannelGrantStatement, idx++, talkgroupId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSystemId);
        // Site info
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentNac);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentLra);
        mInsertChannelGrantStatement.setString(idx++, gvcg.getChannel() != null ? gvcg.getChannel().toString() : null);
        setNullableLong(mInsertChannelGrantStatement, idx++, gvcg.getChannel() != null ? gvcg.getChannel().getDownlinkFrequency() : null);
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER); // timeslot
        mInsertChannelGrantStatement.setInt(idx++, gvcg.isEncrypted() ? 1 : 0);
        // Emergency flag is part of service options, not a direct method
        mInsertChannelGrantStatement.setInt(idx++, (gvcg.getServiceOptions() != null && gvcg.getServiceOptions().isEmergency()) ? 1 : 0);
        mInsertChannelGrantStatement.setString(idx++, gvcg.getServiceOptions() != null ? gvcg.getServiceOptions().toString() : null);
        mInsertChannelGrantStatement.setString(idx++, "GROUP_VOICE_CHANNEL_GRANT");
        mInsertChannelGrantStatement.setString(idx++, gvcg.toString());

        mInsertChannelGrantStatement.executeUpdate();
    }

    /**
     * Processes Unit-to-Unit Voice Channel Grant
     */
    private void processUnitToUnitVoiceChannelGrant(UnitToUnitVoiceChannelGrant uvcg) throws SQLException
    {
        int idx = 1;
        mInsertChannelGrantStatement.setLong(idx++, uvcg.getTimestamp());
        mInsertChannelGrantStatement.setString(idx++, mChannelName);
        mInsertChannelGrantStatement.setString(idx++, "UNIT_TO_UNIT_VOICE");

        // Extract identifiers
        Integer sourceRadioId = null;
        Integer targetRadioId = null;
        for(Identifier id : uvcg.getIdentifiers())
        {
            if(id instanceof RadioIdentifier)
            {
                if(id.getRole() == Role.FROM)
                {
                    sourceRadioId = extractIntValue(id);
                }
                else if(id.getRole() == Role.TO)
                {
                    targetRadioId = extractIntValue(id);
                }
            }
        }
        setNullableInt(mInsertChannelGrantStatement, idx++, sourceRadioId);
        setNullableInt(mInsertChannelGrantStatement, idx++, targetRadioId);
        mInsertChannelGrantStatement.setString(idx++, "RADIO"); // target_type
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER); // talkgroup_id (not applicable)
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSystemId);
        // Site info
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentNac);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentLra);
        mInsertChannelGrantStatement.setString(idx++, uvcg.getChannel() != null ? uvcg.getChannel().toString() : null);
        setNullableLong(mInsertChannelGrantStatement, idx++, uvcg.getChannel() != null ? uvcg.getChannel().getDownlinkFrequency() : null);
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER); // timeslot
        mInsertChannelGrantStatement.setInt(idx++, uvcg.isEncrypted() ? 1 : 0);
        mInsertChannelGrantStatement.setInt(idx++, 0); // No direct emergency flag for unit-to-unit
        mInsertChannelGrantStatement.setNull(idx++, Types.VARCHAR); // No service options for unit-to-unit
        mInsertChannelGrantStatement.setString(idx++, "UNIT_TO_UNIT_VOICE_CHANNEL_GRANT");
        mInsertChannelGrantStatement.setString(idx++, uvcg.toString());

        mInsertChannelGrantStatement.executeUpdate();
    }

    /**
     * Processes Motorola Group Regroup Channel Grant (patch group)
     */
    private void processMotorolaGroupRegroupChannelGrant(MotorolaGroupRegroupChannelGrant mrcg) throws SQLException
    {
        int idx = 1;
        mInsertChannelGrantStatement.setLong(idx++, mrcg.getTimestamp());
        mInsertChannelGrantStatement.setString(idx++, mChannelName);
        mInsertChannelGrantStatement.setString(idx++, "MOTOROLA_GROUP_REGROUP");

        // Extract identifiers
        Integer sourceRadioId = null;
        Integer patchgroupId = null;
        for(Identifier id : mrcg.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && sourceRadioId == null)
            {
                sourceRadioId = extractIntValue(id);
            }
            else if(id instanceof TalkgroupIdentifier && patchgroupId == null)
            {
                patchgroupId = extractIntValue(id);
            }
        }
        setNullableInt(mInsertChannelGrantStatement, idx++, sourceRadioId);
        setNullableInt(mInsertChannelGrantStatement, idx++, patchgroupId);
        mInsertChannelGrantStatement.setString(idx++, "PATCH_GROUP");
        setNullableInt(mInsertChannelGrantStatement, idx++, patchgroupId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSystemId);
        // Site info
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentNac);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentLra);
        mInsertChannelGrantStatement.setString(idx++, mrcg.getChannel() != null ? mrcg.getChannel().toString() : null);
        setNullableLong(mInsertChannelGrantStatement, idx++, mrcg.getChannel() != null ? mrcg.getChannel().getDownlinkFrequency() : null);
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER); // timeslot
        mInsertChannelGrantStatement.setInt(idx++, mrcg.isEncrypted() ? 1 : 0);
        // Emergency flag from service options
        mInsertChannelGrantStatement.setInt(idx++, (mrcg.getServiceOptions() != null && mrcg.getServiceOptions().isEmergency()) ? 1 : 0);
        mInsertChannelGrantStatement.setString(idx++, mrcg.getServiceOptions() != null ? mrcg.getServiceOptions().toString() : null);
        mInsertChannelGrantStatement.setString(idx++, "MOTOROLA_GROUP_REGROUP_CHANNEL_GRANT");
        mInsertChannelGrantStatement.setString(idx++, mrcg.toString());

        mInsertChannelGrantStatement.executeUpdate();
    }

    // ============================================================
    // Additional channel grant processing
    // ============================================================

    /**
     * Processes Group Voice Channel Grant Update
     */
    private void processGroupVoiceChannelGrantUpdate(GroupVoiceChannelGrantUpdate gvcgu) throws SQLException
    {
        int idx = 1;
        mInsertChannelGrantStatement.setLong(idx++, gvcgu.getTimestamp());
        mInsertChannelGrantStatement.setString(idx++, mChannelName);
        mInsertChannelGrantStatement.setString(idx++, "GROUP_VOICE_UPDATE");

        Integer talkgroupId = null;
        for(Identifier id : gvcgu.getIdentifiers())
        {
            if(id instanceof TalkgroupIdentifier && talkgroupId == null)
            {
                talkgroupId = extractIntValue(id);
            }
        }
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER); // source_radio_id
        setNullableInt(mInsertChannelGrantStatement, idx++, talkgroupId);
        mInsertChannelGrantStatement.setString(idx++, "TALKGROUP");
        setNullableInt(mInsertChannelGrantStatement, idx++, talkgroupId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentNac);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentLra);
        mInsertChannelGrantStatement.setNull(idx++, Types.VARCHAR); // channel
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER); // frequency_hz
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER); // timeslot
        mInsertChannelGrantStatement.setInt(idx++, 0); // is_encrypted
        mInsertChannelGrantStatement.setInt(idx++, 0); // is_emergency
        mInsertChannelGrantStatement.setNull(idx++, Types.VARCHAR); // service_options
        mInsertChannelGrantStatement.setString(idx++, "GROUP_VOICE_CHANNEL_GRANT_UPDATE");
        mInsertChannelGrantStatement.setString(idx++, gvcgu.toString());
        mInsertChannelGrantStatement.executeUpdate();
    }

    /**
     * Processes Telephone Interconnect Voice Channel Grant
     */
    private void processTelephoneInterconnectGrant(TelephoneInterconnectVoiceChannelGrant ticg) throws SQLException
    {
        int idx = 1;
        mInsertChannelGrantStatement.setLong(idx++, ticg.getTimestamp());
        mInsertChannelGrantStatement.setString(idx++, mChannelName);
        mInsertChannelGrantStatement.setString(idx++, "TELEPHONE_INTERCONNECT");

        Integer radioId = null;
        for(Identifier id : ticg.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && radioId == null)
            {
                radioId = extractIntValue(id);
            }
        }
        setNullableInt(mInsertChannelGrantStatement, idx++, radioId);
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER); // target_id
        mInsertChannelGrantStatement.setString(idx++, "PSTN");
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER); // talkgroup_id
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentNac);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentLra);
        mInsertChannelGrantStatement.setString(idx++, ticg.getChannel() != null ? ticg.getChannel().toString() : null);
        setNullableLong(mInsertChannelGrantStatement, idx++, ticg.getChannel() != null ? ticg.getChannel().getDownlinkFrequency() : null);
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER); // timeslot
        mInsertChannelGrantStatement.setInt(idx++, ticg.isEncrypted() ? 1 : 0);
        mInsertChannelGrantStatement.setInt(idx++, 0); // is_emergency
        mInsertChannelGrantStatement.setString(idx++, ticg.getServiceOptions() != null ? ticg.getServiceOptions().toString() : null);
        mInsertChannelGrantStatement.setString(idx++, "TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT");
        mInsertChannelGrantStatement.setString(idx++, ticg.toString());
        mInsertChannelGrantStatement.executeUpdate();
    }

    /**
     * Processes SNDCP Data Channel Grant
     */
    private void processSndcpDataChannelGrant(SNDCPDataChannelGrant sdcg) throws SQLException
    {
        int idx = 1;
        mInsertChannelGrantStatement.setLong(idx++, sdcg.getTimestamp());
        mInsertChannelGrantStatement.setString(idx++, mChannelName);
        mInsertChannelGrantStatement.setString(idx++, "SNDCP_DATA");

        Integer radioId = null;
        for(Identifier id : sdcg.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && radioId == null)
            {
                radioId = extractIntValue(id);
            }
        }
        setNullableInt(mInsertChannelGrantStatement, idx++, radioId);
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER); // target_id
        mInsertChannelGrantStatement.setString(idx++, "DATA");
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER); // talkgroup_id
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentNac);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentLra);
        mInsertChannelGrantStatement.setString(idx++, sdcg.getChannel() != null ? sdcg.getChannel().toString() : null);
        setNullableLong(mInsertChannelGrantStatement, idx++, sdcg.getChannel() != null ? sdcg.getChannel().getDownlinkFrequency() : null);
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER); // timeslot
        mInsertChannelGrantStatement.setInt(idx++, 0); // is_encrypted
        mInsertChannelGrantStatement.setInt(idx++, 0); // is_emergency
        mInsertChannelGrantStatement.setNull(idx++, Types.VARCHAR); // service_options
        mInsertChannelGrantStatement.setString(idx++, "SNDCP_DATA_CHANNEL_GRANT");
        mInsertChannelGrantStatement.setString(idx++, sdcg.toString());
        mInsertChannelGrantStatement.executeUpdate();
    }

    // ============================================================
    // Status and Message Update processing
    // ============================================================

    /**
     * Processes Status Update (OSP)
     */
    private void processStatusUpdate(StatusUpdate su) throws SQLException
    {
        int idx = 1;
        mInsertStatusMessageStatement.setLong(idx++, su.getTimestamp());
        mInsertStatusMessageStatement.setString(idx++, mChannelName);
        mInsertStatusMessageStatement.setString(idx++, "STATUS_UPDATE");

        Integer sourceRadioId = null;
        Integer targetRadioId = null;
        for(Identifier id : su.getIdentifiers())
        {
            if(id instanceof RadioIdentifier)
            {
                if(id.getRole() == Role.FROM && sourceRadioId == null)
                {
                    sourceRadioId = extractIntValue(id);
                }
                else if(id.getRole() == Role.TO && targetRadioId == null)
                {
                    targetRadioId = extractIntValue(id);
                }
            }
        }
        setNullableInt(mInsertStatusMessageStatement, idx++, sourceRadioId);
        setNullableInt(mInsertStatusMessageStatement, idx++, targetRadioId);
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // talkgroup_id
        setNullableInt(mInsertStatusMessageStatement, idx++, extractIntValue(su.getUnitStatus()));
        setNullableInt(mInsertStatusMessageStatement, idx++, extractIntValue(su.getUserStatus()));
        mInsertStatusMessageStatement.setNull(idx++, Types.VARCHAR); // short_message
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentNac);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentLra);
        mInsertStatusMessageStatement.setString(idx++, su.toString());
        mInsertStatusMessageStatement.executeUpdate();
    }

    /**
     * Processes Status Query (OSP)
     */
    private void processStatusQuery(StatusQuery sq) throws SQLException
    {
        int idx = 1;
        mInsertStatusMessageStatement.setLong(idx++, sq.getTimestamp());
        mInsertStatusMessageStatement.setString(idx++, mChannelName);
        mInsertStatusMessageStatement.setString(idx++, "STATUS_QUERY");

        Integer targetRadioId = null;
        for(Identifier id : sq.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && targetRadioId == null)
            {
                targetRadioId = extractIntValue(id);
            }
        }
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // source_radio_id
        setNullableInt(mInsertStatusMessageStatement, idx++, targetRadioId);
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // talkgroup_id
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // unit_status
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // user_status
        mInsertStatusMessageStatement.setNull(idx++, Types.VARCHAR); // short_message
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentNac);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentLra);
        mInsertStatusMessageStatement.setString(idx++, sq.toString());
        mInsertStatusMessageStatement.executeUpdate();
    }

    /**
     * Processes Status Update Request (ISP)
     */
    private void processStatusUpdateRequest(StatusUpdateRequest sur) throws SQLException
    {
        int idx = 1;
        mInsertStatusMessageStatement.setLong(idx++, sur.getTimestamp());
        mInsertStatusMessageStatement.setString(idx++, mChannelName);
        mInsertStatusMessageStatement.setString(idx++, "STATUS_UPDATE_REQUEST");

        Integer sourceRadioId = null;
        for(Identifier id : sur.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && sourceRadioId == null)
            {
                sourceRadioId = extractIntValue(id);
            }
        }
        setNullableInt(mInsertStatusMessageStatement, idx++, sourceRadioId);
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // target_radio_id
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // talkgroup_id
        setNullableInt(mInsertStatusMessageStatement, idx++, extractIntValue(sur.getUnitStatus()));
        setNullableInt(mInsertStatusMessageStatement, idx++, extractIntValue(sur.getUserStatus()));
        mInsertStatusMessageStatement.setNull(idx++, Types.VARCHAR); // short_message
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentNac);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentLra);
        mInsertStatusMessageStatement.setString(idx++, sur.toString());
        mInsertStatusMessageStatement.executeUpdate();
    }

    /**
     * Processes Message Update (OSP)
     */
    private void processMessageUpdate(MessageUpdate mu) throws SQLException
    {
        int idx = 1;
        mInsertStatusMessageStatement.setLong(idx++, mu.getTimestamp());
        mInsertStatusMessageStatement.setString(idx++, mChannelName);
        mInsertStatusMessageStatement.setString(idx++, "MESSAGE_UPDATE");

        Integer targetRadioId = null;
        for(Identifier id : mu.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && targetRadioId == null)
            {
                targetRadioId = extractIntValue(id);
            }
        }
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // source_radio_id
        setNullableInt(mInsertStatusMessageStatement, idx++, targetRadioId);
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // talkgroup_id
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // unit_status
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // user_status
        mInsertStatusMessageStatement.setString(idx++, mu.getShortDataMessage() != null ? mu.getShortDataMessage().toString() : null);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentNac);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentLra);
        mInsertStatusMessageStatement.setString(idx++, mu.toString());
        mInsertStatusMessageStatement.executeUpdate();
    }

    /**
     * Processes Call Alert (OSP)
     */
    private void processCallAlert(CallAlert ca) throws SQLException
    {
        int idx = 1;
        mInsertStatusMessageStatement.setLong(idx++, ca.getTimestamp());
        mInsertStatusMessageStatement.setString(idx++, mChannelName);
        mInsertStatusMessageStatement.setString(idx++, "CALL_ALERT");

        Integer targetRadioId = null;
        for(Identifier id : ca.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && targetRadioId == null)
            {
                targetRadioId = extractIntValue(id);
            }
        }
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // source_radio_id
        setNullableInt(mInsertStatusMessageStatement, idx++, targetRadioId);
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // talkgroup_id
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // unit_status
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // user_status
        mInsertStatusMessageStatement.setNull(idx++, Types.VARCHAR); // short_message
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentNac);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentLra);
        mInsertStatusMessageStatement.setString(idx++, ca.toString());
        mInsertStatusMessageStatement.executeUpdate();
    }

    /**
     * Processes Call Alert Request (ISP)
     */
    private void processCallAlertRequest(CallAlertRequest car) throws SQLException
    {
        int idx = 1;
        mInsertStatusMessageStatement.setLong(idx++, car.getTimestamp());
        mInsertStatusMessageStatement.setString(idx++, mChannelName);
        mInsertStatusMessageStatement.setString(idx++, "CALL_ALERT_REQUEST");

        Integer sourceRadioId = null;
        Integer targetRadioId = null;
        for(Identifier id : car.getIdentifiers())
        {
            if(id instanceof RadioIdentifier)
            {
                if(id.getRole() == Role.FROM && sourceRadioId == null)
                {
                    sourceRadioId = extractIntValue(id);
                }
                else if(id.getRole() == Role.TO && targetRadioId == null)
                {
                    targetRadioId = extractIntValue(id);
                }
            }
        }
        setNullableInt(mInsertStatusMessageStatement, idx++, sourceRadioId);
        setNullableInt(mInsertStatusMessageStatement, idx++, targetRadioId);
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // talkgroup_id
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // unit_status
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // user_status
        mInsertStatusMessageStatement.setNull(idx++, Types.VARCHAR); // short_message
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentNac);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentLra);
        mInsertStatusMessageStatement.setString(idx++, car.toString());
        mInsertStatusMessageStatement.executeUpdate();
    }

    // ============================================================
    // Emergency Alarm processing
    // ============================================================

    /**
     * Processes Emergency Alarm Request (ISP)
     */
    private void processEmergencyAlarmRequest(EmergencyAlarmRequest ear) throws SQLException
    {
        int idx = 1;
        mInsertEmergencyAlarmStatement.setLong(idx++, ear.getTimestamp());
        mInsertEmergencyAlarmStatement.setString(idx++, mChannelName);
        mInsertEmergencyAlarmStatement.setString(idx++, "EMERGENCY_ALARM_REQUEST");

        Integer sourceRadioId = null;
        Integer talkgroupId = null;
        for(Identifier id : ear.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && sourceRadioId == null)
            {
                sourceRadioId = extractIntValue(id);
            }
            else if(id instanceof TalkgroupIdentifier && talkgroupId == null)
            {
                talkgroupId = extractIntValue(id);
            }
        }
        setNullableInt(mInsertEmergencyAlarmStatement, idx++, sourceRadioId);
        setNullableInt(mInsertEmergencyAlarmStatement, idx++, talkgroupId);
        setNullableInt(mInsertEmergencyAlarmStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertEmergencyAlarmStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertEmergencyAlarmStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertEmergencyAlarmStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertEmergencyAlarmStatement, idx++, mCurrentNac);
        mInsertEmergencyAlarmStatement.setString(idx++, ear.toString());
        mInsertEmergencyAlarmStatement.executeUpdate();
    }

    /**
     * Processes Motorola Emergency Alarm Activation
     */
    private void processMotorolaEmergencyAlarm(MotorolaEmergencyAlarmActivation meaa) throws SQLException
    {
        int idx = 1;
        mInsertEmergencyAlarmStatement.setLong(idx++, meaa.getTimestamp());
        mInsertEmergencyAlarmStatement.setString(idx++, mChannelName);
        mInsertEmergencyAlarmStatement.setString(idx++, "MOTOROLA_EMERGENCY_ACTIVATION");

        Integer sourceRadioId = null;
        Integer talkgroupId = null;
        for(Identifier id : meaa.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && sourceRadioId == null)
            {
                sourceRadioId = extractIntValue(id);
            }
            else if(id instanceof TalkgroupIdentifier && talkgroupId == null)
            {
                talkgroupId = extractIntValue(id);
            }
        }
        setNullableInt(mInsertEmergencyAlarmStatement, idx++, sourceRadioId);
        setNullableInt(mInsertEmergencyAlarmStatement, idx++, talkgroupId);
        setNullableInt(mInsertEmergencyAlarmStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertEmergencyAlarmStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertEmergencyAlarmStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertEmergencyAlarmStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertEmergencyAlarmStatement, idx++, mCurrentNac);
        mInsertEmergencyAlarmStatement.setString(idx++, meaa.toString());
        mInsertEmergencyAlarmStatement.executeUpdate();
    }

    // ============================================================
    // Extended Function and Response message processing
    // ============================================================

    /**
     * Processes Extended Function Command
     */
    private void processExtendedFunctionCommand(ExtendedFunctionCommand efc) throws SQLException
    {
        int idx = 1;
        mInsertStatusMessageStatement.setLong(idx++, efc.getTimestamp());
        mInsertStatusMessageStatement.setString(idx++, mChannelName);
        mInsertStatusMessageStatement.setString(idx++, "EXTENDED_FUNCTION_CMD");

        Integer targetRadioId = null;
        for(Identifier id : efc.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && targetRadioId == null)
            {
                targetRadioId = extractIntValue(id);
            }
        }
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // source_radio_id
        setNullableInt(mInsertStatusMessageStatement, idx++, targetRadioId);
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // talkgroup_id
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // unit_status
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // user_status
        mInsertStatusMessageStatement.setString(idx++, efc.getExtendedFunction() != null ? efc.getExtendedFunction().toString() : null);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentNac);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentLra);
        mInsertStatusMessageStatement.setString(idx++, efc.toString());
        mInsertStatusMessageStatement.executeUpdate();
    }

    /**
     * Processes Radio Unit Monitor Command
     */
    private void processRadioUnitMonitorCommand(RadioUnitMonitorCommand rumc) throws SQLException
    {
        int idx = 1;
        mInsertStatusMessageStatement.setLong(idx++, rumc.getTimestamp());
        mInsertStatusMessageStatement.setString(idx++, mChannelName);
        mInsertStatusMessageStatement.setString(idx++, "RADIO_UNIT_MONITOR_CMD");

        Integer targetRadioId = null;
        for(Identifier id : rumc.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && targetRadioId == null)
            {
                targetRadioId = extractIntValue(id);
            }
        }
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // source_radio_id
        setNullableInt(mInsertStatusMessageStatement, idx++, targetRadioId);
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // talkgroup_id
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // unit_status
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // user_status
        mInsertStatusMessageStatement.setNull(idx++, Types.VARCHAR); // short_message
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentNac);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentLra);
        mInsertStatusMessageStatement.setString(idx++, rumc.toString());
        mInsertStatusMessageStatement.executeUpdate();
    }

    /**
     * Processes Acknowledge Response
     */
    private void processAcknowledgeResponse(AcknowledgeResponse ar) throws SQLException
    {
        int idx = 1;
        mInsertStatusMessageStatement.setLong(idx++, ar.getTimestamp());
        mInsertStatusMessageStatement.setString(idx++, mChannelName);
        mInsertStatusMessageStatement.setString(idx++, "ACKNOWLEDGE_RESPONSE");

        Integer targetRadioId = null;
        for(Identifier id : ar.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && targetRadioId == null)
            {
                targetRadioId = extractIntValue(id);
            }
        }
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // source_radio_id
        setNullableInt(mInsertStatusMessageStatement, idx++, targetRadioId);
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // talkgroup_id
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // unit_status
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // user_status
        mInsertStatusMessageStatement.setNull(idx++, Types.VARCHAR); // short_message
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentNac);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentLra);
        mInsertStatusMessageStatement.setString(idx++, ar.toString());
        mInsertStatusMessageStatement.executeUpdate();
    }

    /**
     * Processes Deny Response
     */
    private void processDenyResponse(DenyResponse dr) throws SQLException
    {
        int idx = 1;
        mInsertStatusMessageStatement.setLong(idx++, dr.getTimestamp());
        mInsertStatusMessageStatement.setString(idx++, mChannelName);
        mInsertStatusMessageStatement.setString(idx++, "DENY_RESPONSE");

        Integer targetRadioId = null;
        for(Identifier id : dr.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && targetRadioId == null)
            {
                targetRadioId = extractIntValue(id);
            }
        }
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // source_radio_id
        setNullableInt(mInsertStatusMessageStatement, idx++, targetRadioId);
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // talkgroup_id
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // unit_status
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // user_status
        mInsertStatusMessageStatement.setString(idx++, dr.getDenyReason() != null ? dr.getDenyReason().toString() : null);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentNac);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentLra);
        mInsertStatusMessageStatement.setString(idx++, dr.toString());
        mInsertStatusMessageStatement.executeUpdate();
    }

    /**
     * Processes Queued Response
     */
    private void processQueuedResponse(QueuedResponse qr) throws SQLException
    {
        int idx = 1;
        mInsertStatusMessageStatement.setLong(idx++, qr.getTimestamp());
        mInsertStatusMessageStatement.setString(idx++, mChannelName);
        mInsertStatusMessageStatement.setString(idx++, "QUEUED_RESPONSE");

        Integer targetRadioId = null;
        for(Identifier id : qr.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && targetRadioId == null)
            {
                targetRadioId = extractIntValue(id);
            }
        }
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // source_radio_id
        setNullableInt(mInsertStatusMessageStatement, idx++, targetRadioId);
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // talkgroup_id
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // unit_status
        mInsertStatusMessageStatement.setNull(idx++, Types.INTEGER); // user_status
        mInsertStatusMessageStatement.setString(idx++, qr.getQueuedResponseReason() != null ? qr.getQueuedResponseReason().toString() : null);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentNac);
        setNullableInt(mInsertStatusMessageStatement, idx++, mCurrentLra);
        mInsertStatusMessageStatement.setString(idx++, qr.toString());
        mInsertStatusMessageStatement.executeUpdate();
    }

    // ==================== Phase 2 MAC Message Processing ====================

    /**
     * Processes P25 Phase 2 MAC messages to extract network/site information, registrations,
     * affiliations, and channel grants. These are similar to Phase 1 TSBK but for TDMA traffic.
     */
    private void processMacMessage(MacMessage mac)
    {
        try
        {
            MacStructure structure = mac.getMacStructure();
            if(structure == null)
            {
                return;
            }

            // Site Status broadcasts
            if(structure instanceof RfssStatusBroadcastImplicit)
            {
                processMacRfssStatusBroadcast((RfssStatusBroadcastImplicit)structure, mac.getTimestamp());
            }
            else if(structure instanceof RfssStatusBroadcastExplicit)
            {
                processMacRfssStatusBroadcastExplicit((RfssStatusBroadcastExplicit)structure, mac.getTimestamp());
            }
            // Network Status broadcasts
            else if(structure instanceof NetworkStatusBroadcastImplicit)
            {
                processMacNetworkStatusBroadcast((NetworkStatusBroadcastImplicit)structure, mac.getTimestamp());
            }
            else if(structure instanceof NetworkStatusBroadcastExplicit)
            {
                processMacNetworkStatusBroadcastExplicit((NetworkStatusBroadcastExplicit)structure, mac.getTimestamp());
            }
            // Adjacent Site broadcasts
            else if(structure instanceof AdjacentStatusBroadcastImplicit)
            {
                processMacAdjacentStatusBroadcast((AdjacentStatusBroadcastImplicit)structure, mac.getTimestamp());
            }
            else if(structure instanceof AdjacentStatusBroadcastExplicit)
            {
                processMacAdjacentStatusBroadcastExplicit((AdjacentStatusBroadcastExplicit)structure, mac.getTimestamp());
            }
            // Group Voice Channel Grants
            else if(structure instanceof GroupVoiceChannelGrantImplicit)
            {
                processMacGroupVoiceChannelGrant((GroupVoiceChannelGrantImplicit)structure, mac.getTimestamp());
            }
            else if(structure instanceof GroupVoiceChannelGrantExplicit)
            {
                processMacGroupVoiceChannelGrantExplicit((GroupVoiceChannelGrantExplicit)structure, mac.getTimestamp());
            }
            else if(structure instanceof GroupVoiceChannelGrantUpdateImplicit)
            {
                processMacGroupVoiceChannelGrantUpdate((GroupVoiceChannelGrantUpdateImplicit)structure, mac.getTimestamp());
            }
            else if(structure instanceof GroupVoiceChannelGrantUpdateExplicit)
            {
                processMacGroupVoiceChannelGrantUpdateExplicit((GroupVoiceChannelGrantUpdateExplicit)structure, mac.getTimestamp());
            }
            // Group Voice Channel User (identifies who is talking)
            else if(structure instanceof GroupVoiceChannelUserAbbreviated)
            {
                processMacGroupVoiceChannelUser((GroupVoiceChannelUserAbbreviated)structure, mac.getTimestamp());
            }
            else if(structure instanceof GroupVoiceChannelUserExtended)
            {
                processMacGroupVoiceChannelUserExtended((GroupVoiceChannelUserExtended)structure, mac.getTimestamp());
            }
            // Affiliation messages
            else if(structure instanceof GroupAffiliationResponseAbbreviated)
            {
                processMacGroupAffiliationResponse((GroupAffiliationResponseAbbreviated)structure, mac.getTimestamp());
            }
            else if(structure instanceof GroupAffiliationResponseExtended)
            {
                processMacGroupAffiliationResponseExtended((GroupAffiliationResponseExtended)structure, mac.getTimestamp());
            }
            // Registration messages
            else if(structure instanceof UnitRegistrationResponseAbbreviated)
            {
                processMacUnitRegistrationResponse((UnitRegistrationResponseAbbreviated)structure, mac.getTimestamp());
            }
            else if(structure instanceof UnitRegistrationResponseExtended)
            {
                processMacUnitRegistrationResponseExtended((UnitRegistrationResponseExtended)structure, mac.getTimestamp());
            }
            // PTT events (voice activity)
            else if(structure instanceof PushToTalk)
            {
                processMacPushToTalk((PushToTalk)structure, mac.getTimestamp());
            }
            else if(structure instanceof EndPushToTalk)
            {
                processMacEndPushToTalk((EndPushToTalk)structure, mac.getTimestamp());
            }
        }
        catch(SQLException e)
        {
            mLog.error("Error processing MAC message", e);
        }
    }

    /**
     * Processes Phase 2 RFSS Status Broadcast (Implicit) - updates cached site info
     * Note: Raw message is already saved in the messages table, so we only update cached values here
     */
    private void processMacRfssStatusBroadcast(RfssStatusBroadcastImplicit rfss, long timestamp) throws SQLException
    {
        // RfssStatusBroadcast provides accessor methods
        mCurrentSystemId = extractIntValue(rfss.getSystem());
        mCurrentRfss = extractIntValue(rfss.getRFSS());
        mCurrentSiteId = extractIntValue(rfss.getSite());
        mCurrentLra = extractIntValue(rfss.getLRA());
        // Extract NAC from identifiers
        for(Identifier id : rfss.getIdentifiers())
        {
            if(id instanceof APCO25Nac)
            {
                mCurrentNac = extractIntValue(id);
            }
        }
    }

    /**
     * Processes Phase 2 RFSS Status Broadcast (Explicit)
     */
    private void processMacRfssStatusBroadcastExplicit(RfssStatusBroadcastExplicit rfss, long timestamp) throws SQLException
    {
        mCurrentSystemId = extractIntValue(rfss.getSystem());
        mCurrentRfss = extractIntValue(rfss.getRFSS());
        mCurrentSiteId = extractIntValue(rfss.getSite());
        mCurrentLra = extractIntValue(rfss.getLRA());
        for(Identifier id : rfss.getIdentifiers())
        {
            if(id instanceof APCO25Nac)
            {
                mCurrentNac = extractIntValue(id);
            }
        }
    }

    /**
     * Processes Phase 2 Network Status Broadcast (Implicit)
     */
    private void processMacNetworkStatusBroadcast(NetworkStatusBroadcastImplicit nsb, long timestamp) throws SQLException
    {
        // Extract network info from identifiers
        for(Identifier id : nsb.getIdentifiers())
        {
            if(id instanceof APCO25Nac)
            {
                mCurrentNac = extractIntValue(id);
            }
        }
    }

    /**
     * Processes Phase 2 Network Status Broadcast (Explicit)
     */
    private void processMacNetworkStatusBroadcastExplicit(NetworkStatusBroadcastExplicit nsb, long timestamp) throws SQLException
    {
        for(Identifier id : nsb.getIdentifiers())
        {
            if(id instanceof APCO25Nac)
            {
                mCurrentNac = extractIntValue(id);
            }
        }
    }

    /**
     * Processes Phase 2 Adjacent Status Broadcast (Implicit)
     * Adjacent site info is logged but doesn't update current site cache
     */
    private void processMacAdjacentStatusBroadcast(AdjacentStatusBroadcastImplicit asb, long timestamp) throws SQLException
    {
        // Adjacent sites are informational - just extract what we can
        // The raw message is already logged in the messages table
    }

    /**
     * Processes Phase 2 Adjacent Status Broadcast (Explicit)
     */
    private void processMacAdjacentStatusBroadcastExplicit(AdjacentStatusBroadcastExplicit asb, long timestamp) throws SQLException
    {
        // Adjacent sites are informational - raw message already logged
    }

    /**
     * Processes Phase 2 Group Voice Channel Grant (Implicit)
     */
    private void processMacGroupVoiceChannelGrant(GroupVoiceChannelGrantImplicit gvcg, long timestamp) throws SQLException
    {
        Integer talkgroupId = null;
        Integer sourceRadioId = null;

        for(Identifier id : gvcg.getIdentifiers())
        {
            if(id instanceof TalkgroupIdentifier && talkgroupId == null)
            {
                talkgroupId = extractIntValue(id);
            }
            else if(id instanceof RadioIdentifier && sourceRadioId == null)
            {
                sourceRadioId = extractIntValue(id);
            }
        }

        boolean encrypted = gvcg.getServiceOptions() != null && gvcg.getServiceOptions().isEncrypted();

        int idx = 1;
        mInsertChannelGrantStatement.setLong(idx++, timestamp);
        mInsertChannelGrantStatement.setString(idx++, mChannelName);
        mInsertChannelGrantStatement.setString(idx++, "GROUP_VOICE_CHANNEL_GRANT_P2");
        setNullableInt(mInsertChannelGrantStatement, idx++, sourceRadioId);
        setNullableInt(mInsertChannelGrantStatement, idx++, talkgroupId);
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER); // channel
        mInsertChannelGrantStatement.setBoolean(idx++, encrypted);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentNac);
        mInsertChannelGrantStatement.setString(idx++, gvcg.toString());
        mInsertChannelGrantStatement.executeUpdate();
    }

    /**
     * Processes Phase 2 Group Voice Channel Grant (Explicit)
     */
    private void processMacGroupVoiceChannelGrantExplicit(GroupVoiceChannelGrantExplicit gvcg, long timestamp) throws SQLException
    {
        Integer talkgroupId = null;
        Integer sourceRadioId = null;

        for(Identifier id : gvcg.getIdentifiers())
        {
            if(id instanceof TalkgroupIdentifier && talkgroupId == null)
            {
                talkgroupId = extractIntValue(id);
            }
            else if(id instanceof RadioIdentifier && sourceRadioId == null)
            {
                sourceRadioId = extractIntValue(id);
            }
        }

        boolean encrypted = gvcg.getServiceOptions() != null && gvcg.getServiceOptions().isEncrypted();

        int idx = 1;
        mInsertChannelGrantStatement.setLong(idx++, timestamp);
        mInsertChannelGrantStatement.setString(idx++, mChannelName);
        mInsertChannelGrantStatement.setString(idx++, "GROUP_VOICE_CHANNEL_GRANT_EXPLICIT_P2");
        setNullableInt(mInsertChannelGrantStatement, idx++, sourceRadioId);
        setNullableInt(mInsertChannelGrantStatement, idx++, talkgroupId);
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER);
        mInsertChannelGrantStatement.setBoolean(idx++, encrypted);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentNac);
        mInsertChannelGrantStatement.setString(idx++, gvcg.toString());
        mInsertChannelGrantStatement.executeUpdate();
    }

    /**
     * Processes Phase 2 Group Voice Channel Grant Update (Implicit)
     */
    private void processMacGroupVoiceChannelGrantUpdate(GroupVoiceChannelGrantUpdateImplicit gvcgu, long timestamp) throws SQLException
    {
        Integer talkgroupId = null;

        for(Identifier id : gvcgu.getIdentifiers())
        {
            if(id instanceof TalkgroupIdentifier && talkgroupId == null)
            {
                talkgroupId = extractIntValue(id);
            }
        }

        int idx = 1;
        mInsertChannelGrantStatement.setLong(idx++, timestamp);
        mInsertChannelGrantStatement.setString(idx++, mChannelName);
        mInsertChannelGrantStatement.setString(idx++, "GROUP_VOICE_CHANNEL_GRANT_UPDATE_P2");
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER); // source_radio_id
        setNullableInt(mInsertChannelGrantStatement, idx++, talkgroupId);
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER);
        mInsertChannelGrantStatement.setBoolean(idx++, false);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentNac);
        mInsertChannelGrantStatement.setString(idx++, gvcgu.toString());
        mInsertChannelGrantStatement.executeUpdate();
    }

    /**
     * Processes Phase 2 Group Voice Channel Grant Update (Explicit)
     */
    private void processMacGroupVoiceChannelGrantUpdateExplicit(GroupVoiceChannelGrantUpdateExplicit gvcgu, long timestamp) throws SQLException
    {
        Integer talkgroupId = null;

        for(Identifier id : gvcgu.getIdentifiers())
        {
            if(id instanceof TalkgroupIdentifier && talkgroupId == null)
            {
                talkgroupId = extractIntValue(id);
            }
        }

        int idx = 1;
        mInsertChannelGrantStatement.setLong(idx++, timestamp);
        mInsertChannelGrantStatement.setString(idx++, mChannelName);
        mInsertChannelGrantStatement.setString(idx++, "GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT_P2");
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER);
        setNullableInt(mInsertChannelGrantStatement, idx++, talkgroupId);
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER);
        mInsertChannelGrantStatement.setBoolean(idx++, false);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentNac);
        mInsertChannelGrantStatement.setString(idx++, gvcgu.toString());
        mInsertChannelGrantStatement.executeUpdate();
    }

    /**
     * Processes Phase 2 Group Voice Channel User (Abbreviated) - identifies who is talking on a channel
     */
    private void processMacGroupVoiceChannelUser(GroupVoiceChannelUserAbbreviated gvcu, long timestamp) throws SQLException
    {
        Integer talkgroupId = null;
        Integer sourceRadioId = null;

        for(Identifier id : gvcu.getIdentifiers())
        {
            if(id instanceof TalkgroupIdentifier && talkgroupId == null)
            {
                talkgroupId = extractIntValue(id);
            }
            else if(id instanceof RadioIdentifier && sourceRadioId == null)
            {
                sourceRadioId = extractIntValue(id);
            }
        }

        // Log as a channel grant to capture who is talking
        int idx = 1;
        mInsertChannelGrantStatement.setLong(idx++, timestamp);
        mInsertChannelGrantStatement.setString(idx++, mChannelName);
        mInsertChannelGrantStatement.setString(idx++, "GROUP_VOICE_CHANNEL_USER_P2");
        setNullableInt(mInsertChannelGrantStatement, idx++, sourceRadioId);
        setNullableInt(mInsertChannelGrantStatement, idx++, talkgroupId);
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER);
        mInsertChannelGrantStatement.setBoolean(idx++, false);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentNac);
        mInsertChannelGrantStatement.setString(idx++, gvcu.toString());
        mInsertChannelGrantStatement.executeUpdate();
    }

    /**
     * Processes Phase 2 Group Voice Channel User (Extended)
     */
    private void processMacGroupVoiceChannelUserExtended(GroupVoiceChannelUserExtended gvcu, long timestamp) throws SQLException
    {
        Integer talkgroupId = null;
        Integer sourceRadioId = null;

        for(Identifier id : gvcu.getIdentifiers())
        {
            if(id instanceof TalkgroupIdentifier && talkgroupId == null)
            {
                talkgroupId = extractIntValue(id);
            }
            else if(id instanceof RadioIdentifier && sourceRadioId == null)
            {
                sourceRadioId = extractIntValue(id);
            }
        }

        int idx = 1;
        mInsertChannelGrantStatement.setLong(idx++, timestamp);
        mInsertChannelGrantStatement.setString(idx++, mChannelName);
        mInsertChannelGrantStatement.setString(idx++, "GROUP_VOICE_CHANNEL_USER_EXTENDED_P2");
        setNullableInt(mInsertChannelGrantStatement, idx++, sourceRadioId);
        setNullableInt(mInsertChannelGrantStatement, idx++, talkgroupId);
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER);
        mInsertChannelGrantStatement.setBoolean(idx++, false);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentNac);
        mInsertChannelGrantStatement.setString(idx++, gvcu.toString());
        mInsertChannelGrantStatement.executeUpdate();
    }

    /**
     * Processes Phase 2 Group Affiliation Response (Abbreviated)
     */
    private void processMacGroupAffiliationResponse(GroupAffiliationResponseAbbreviated gar, long timestamp) throws SQLException
    {
        Integer radioId = null;
        Integer talkgroupId = null;

        for(Identifier id : gar.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && radioId == null)
            {
                radioId = extractIntValue(id);
            }
            else if(id instanceof TalkgroupIdentifier && talkgroupId == null)
            {
                talkgroupId = extractIntValue(id);
            }
        }

        int idx = 1;
        mInsertAffiliationStatement.setLong(idx++, timestamp);
        mInsertAffiliationStatement.setString(idx++, mChannelName);
        mInsertAffiliationStatement.setString(idx++, "GROUP_AFFILIATION_RESPONSE_P2");
        setNullableInt(mInsertAffiliationStatement, idx++, radioId);
        setNullableInt(mInsertAffiliationStatement, idx++, talkgroupId);
        mInsertAffiliationStatement.setNull(idx++, Types.VARCHAR); // response
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentNac);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentLra);
        mInsertAffiliationStatement.setString(idx++, gar.toString());
        mInsertAffiliationStatement.executeUpdate();
    }

    /**
     * Processes Phase 2 Group Affiliation Response (Extended)
     */
    private void processMacGroupAffiliationResponseExtended(GroupAffiliationResponseExtended gar, long timestamp) throws SQLException
    {
        Integer radioId = null;
        Integer talkgroupId = null;

        for(Identifier id : gar.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && radioId == null)
            {
                radioId = extractIntValue(id);
            }
            else if(id instanceof TalkgroupIdentifier && talkgroupId == null)
            {
                talkgroupId = extractIntValue(id);
            }
        }

        int idx = 1;
        mInsertAffiliationStatement.setLong(idx++, timestamp);
        mInsertAffiliationStatement.setString(idx++, mChannelName);
        mInsertAffiliationStatement.setString(idx++, "GROUP_AFFILIATION_RESPONSE_EXTENDED_P2");
        setNullableInt(mInsertAffiliationStatement, idx++, radioId);
        setNullableInt(mInsertAffiliationStatement, idx++, talkgroupId);
        mInsertAffiliationStatement.setNull(idx++, Types.VARCHAR);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentNac);
        setNullableInt(mInsertAffiliationStatement, idx++, mCurrentLra);
        mInsertAffiliationStatement.setString(idx++, gar.toString());
        mInsertAffiliationStatement.executeUpdate();
    }

    /**
     * Processes Phase 2 Unit Registration Response (Abbreviated)
     */
    private void processMacUnitRegistrationResponse(UnitRegistrationResponseAbbreviated urr, long timestamp) throws SQLException
    {
        Integer radioId = null;

        for(Identifier id : urr.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && radioId == null)
            {
                radioId = extractIntValue(id);
            }
        }

        int idx = 1;
        mInsertRegistrationStatement.setLong(idx++, timestamp);
        mInsertRegistrationStatement.setString(idx++, mChannelName);
        mInsertRegistrationStatement.setString(idx++, "UNIT_REGISTRATION_RESPONSE_P2");
        setNullableInt(mInsertRegistrationStatement, idx++, radioId);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentSystemId); // Use cached system ID
        mInsertRegistrationStatement.setNull(idx++, Types.VARCHAR); // response
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentNac);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentLra);
        mInsertRegistrationStatement.setString(idx++, urr.toString());
        mInsertRegistrationStatement.executeUpdate();
    }

    /**
     * Processes Phase 2 Unit Registration Response (Extended)
     */
    private void processMacUnitRegistrationResponseExtended(UnitRegistrationResponseExtended urr, long timestamp) throws SQLException
    {
        Integer radioId = null;

        for(Identifier id : urr.getIdentifiers())
        {
            if(id instanceof RadioIdentifier && radioId == null)
            {
                radioId = extractIntValue(id);
            }
        }

        int idx = 1;
        mInsertRegistrationStatement.setLong(idx++, timestamp);
        mInsertRegistrationStatement.setString(idx++, mChannelName);
        mInsertRegistrationStatement.setString(idx++, "UNIT_REGISTRATION_RESPONSE_EXTENDED_P2");
        setNullableInt(mInsertRegistrationStatement, idx++, radioId);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentSystemId);
        mInsertRegistrationStatement.setNull(idx++, Types.VARCHAR);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentNac);
        setNullableInt(mInsertRegistrationStatement, idx++, mCurrentLra);
        mInsertRegistrationStatement.setString(idx++, urr.toString());
        mInsertRegistrationStatement.executeUpdate();
    }

    /**
     * Processes Phase 2 Push-To-Talk (PTT) - indicates start of transmission
     */
    private void processMacPushToTalk(PushToTalk ptt, long timestamp) throws SQLException
    {
        Integer talkgroupId = null;
        Integer sourceRadioId = null;

        for(Identifier id : ptt.getIdentifiers())
        {
            if(id instanceof TalkgroupIdentifier && talkgroupId == null)
            {
                talkgroupId = extractIntValue(id);
            }
            else if(id instanceof RadioIdentifier && sourceRadioId == null)
            {
                sourceRadioId = extractIntValue(id);
            }
        }

        // Log PTT events as channel grants to capture transmission starts
        int idx = 1;
        mInsertChannelGrantStatement.setLong(idx++, timestamp);
        mInsertChannelGrantStatement.setString(idx++, mChannelName);
        mInsertChannelGrantStatement.setString(idx++, "PUSH_TO_TALK_P2");
        setNullableInt(mInsertChannelGrantStatement, idx++, sourceRadioId);
        setNullableInt(mInsertChannelGrantStatement, idx++, talkgroupId);
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER);
        mInsertChannelGrantStatement.setBoolean(idx++, false);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentNac);
        mInsertChannelGrantStatement.setString(idx++, ptt.toString());
        mInsertChannelGrantStatement.executeUpdate();
    }

    /**
     * Processes Phase 2 End Push-To-Talk - indicates end of transmission
     */
    private void processMacEndPushToTalk(EndPushToTalk eptt, long timestamp) throws SQLException
    {
        Integer talkgroupId = null;
        Integer sourceRadioId = null;

        for(Identifier id : eptt.getIdentifiers())
        {
            if(id instanceof TalkgroupIdentifier && talkgroupId == null)
            {
                talkgroupId = extractIntValue(id);
            }
            else if(id instanceof RadioIdentifier && sourceRadioId == null)
            {
                sourceRadioId = extractIntValue(id);
            }
        }

        int idx = 1;
        mInsertChannelGrantStatement.setLong(idx++, timestamp);
        mInsertChannelGrantStatement.setString(idx++, mChannelName);
        mInsertChannelGrantStatement.setString(idx++, "END_PUSH_TO_TALK_P2");
        setNullableInt(mInsertChannelGrantStatement, idx++, sourceRadioId);
        setNullableInt(mInsertChannelGrantStatement, idx++, talkgroupId);
        mInsertChannelGrantStatement.setNull(idx++, Types.INTEGER);
        mInsertChannelGrantStatement.setBoolean(idx++, false);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentWacn);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSystemId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentSiteId);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentRfss);
        setNullableInt(mInsertChannelGrantStatement, idx++, mCurrentNac);
        mInsertChannelGrantStatement.setString(idx++, eptt.toString());
        mInsertChannelGrantStatement.executeUpdate();
    }

    /**
     * Gets the current cached site ID from RFSS Status broadcasts
     * @return the current site ID or null if not yet received
     */
    public Integer getCurrentSiteId()
    {
        return mCurrentSiteId;
    }

    /**
     * Gets the current cached RFSS ID from RFSS Status broadcasts
     * @return the current RFSS ID or null if not yet received
     */
    public Integer getCurrentRfss()
    {
        return mCurrentRfss;
    }

    /**
     * Gets the alias for an identifier
     */
    private String getAlias(IdentifierCollection collection, Identifier identifier)
    {
        if(collection == null || identifier == null)
        {
            return null;
        }

        try
        {
            Identifier aliasListId = collection.getIdentifier(IdentifierClass.CONFIGURATION, Form.ALIAS_LIST, Role.ANY);
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

        return null;
    }

    /**
     * Gets identifier value as string
     */
    private String getIdentifierValue(IdentifierCollection ids, IdentifierClass idClass, Form form)
    {
        if(ids == null)
        {
            return null;
        }

        Identifier id = ids.getIdentifier(idClass, form, Role.ANY);
        return id != null ? id.toString() : null;
    }

    /**
     * Gets algorithm name for non-P25 protocols
     */
    private String getAlgorithmName(int algorithmId)
    {
        switch(algorithmId)
        {
            case 0x00: return "NO_ENCRYPTION";
            case 0x01: return "HYTERA_BASIC_PRIVACY";
            case 0x02:
            case 0x26: return "HYTERA_RC4_EP";
            case 0x21: return "DMRA_RC4_EP";
            case 0x24: return "DMRA_AES128";
            case 0x25: return "DMRA_AES256";
            default:
                Encryption p25 = Encryption.fromValue(algorithmId);
                if(p25 != Encryption.UNKNOWN)
                {
                    return p25.toString();
                }
                return "UNKNOWN_0x" + Integer.toHexString(algorithmId).toUpperCase();
        }
    }

    // ============================================================
    // Helper methods for extracting network identifiers (P25)
    // ============================================================

    /**
     * Extracts an integer value from a generic Identifier.
     * Works with APCO25 identifiers that wrap integer values.
     * @param id the Identifier to extract value from
     * @return the integer value or null if not extractable
     */
    private Integer extractIntValue(Identifier id)
    {
        if(id == null)
        {
            return null;
        }
        if(id instanceof IntegerIdentifier)
        {
            return ((IntegerIdentifier)id).getValue();
        }
        // Try to get value via reflection for APCO25 identifiers
        Object value = id.getValue();
        if(value instanceof Integer)
        {
            return (Integer)value;
        }
        if(value instanceof Number)
        {
            return ((Number)value).intValue();
        }
        return null;
    }

    /**
     * Gets WACN (Wide Area Communications Network ID) from identifier collection
     */
    private Integer getWacn(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier id = ids.getIdentifier(IdentifierClass.NETWORK, Form.WACN, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return ((IntegerIdentifier)id).getValue();
        }
        return null;
    }

    /**
     * Gets P25 System ID from identifier collection
     */
    private Integer getSystemId(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier id = ids.getIdentifier(IdentifierClass.NETWORK, Form.SYSTEM, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return ((IntegerIdentifier)id).getValue();
        }
        return null;
    }

    /**
     * Gets Site ID from identifier collection
     */
    private Integer getSiteId(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier id = ids.getIdentifier(IdentifierClass.NETWORK, Form.SITE, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return ((IntegerIdentifier)id).getValue();
        }
        return null;
    }

    /**
     * Gets RF Subsystem ID from identifier collection
     */
    private Integer getRfss(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier id = ids.getIdentifier(IdentifierClass.NETWORK, Form.RF_SUBSYSTEM, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return ((IntegerIdentifier)id).getValue();
        }
        return null;
    }

    /**
     * Gets Network Access Code from identifier collection
     */
    private Integer getNac(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier id = ids.getIdentifier(IdentifierClass.NETWORK, Form.NETWORK_ACCESS_CODE, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return ((IntegerIdentifier)id).getValue();
        }
        return null;
    }

    /**
     * Gets Location Registration Area from identifier collection
     */
    private Integer getLra(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier id = ids.getIdentifier(IdentifierClass.NETWORK, Form.LOCATION_REGISTRATION_AREA, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return ((IntegerIdentifier)id).getValue();
        }
        return null;
    }

    // ============================================================
    // Helper methods for extracting network identifiers (DMR)
    // ============================================================

    /**
     * Gets DMR Network ID from identifier collection
     */
    private Integer getDmrNetwork(IdentifierCollection ids, Protocol protocol)
    {
        if(ids == null || protocol != Protocol.DMR) return null;
        Identifier id = ids.getIdentifier(IdentifierClass.NETWORK, Form.NETWORK, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return ((IntegerIdentifier)id).getValue();
        }
        return null;
    }

    /**
     * Gets DMR Site ID from identifier collection
     */
    private Integer getDmrSite(IdentifierCollection ids, Protocol protocol)
    {
        if(ids == null || protocol != Protocol.DMR) return null;
        Identifier id = ids.getIdentifier(IdentifierClass.NETWORK, Form.SITE, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return ((IntegerIdentifier)id).getValue();
        }
        return null;
    }

    // ============================================================
    // Helper methods for extracting user/entity metadata
    // ============================================================

    /**
     * Gets raw radio ID (numeric) from the FROM identifier
     */
    private Integer getRawRadioId(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier from = ids.getFromIdentifier();
        if(from instanceof RadioIdentifier)
        {
            return ((RadioIdentifier)from).getValue();
        }
        return null;
    }

    /**
     * Gets raw talkgroup ID (numeric) from the TO identifier
     */
    private Integer getRawTalkgroupId(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier to = ids.getToIdentifier();
        if(to instanceof TalkgroupIdentifier)
        {
            return ((TalkgroupIdentifier)to).getValue();
        }
        return null;
    }

    /**
     * Gets raw talkgroup from Form.TALKGROUP identifier
     */
    private Integer getTalkgroupRaw(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier id = ids.getIdentifier(IdentifierClass.USER, Form.TALKGROUP, Role.ANY);
        if(id instanceof TalkgroupIdentifier)
        {
            return ((TalkgroupIdentifier)id).getValue();
        }
        return null;
    }

    /**
     * Gets talker alias from identifier collection
     */
    private String getTalkerAlias(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier id = ids.getIdentifier(IdentifierClass.USER, Form.TALKER_ALIAS, Role.ANY);
        if(id instanceof TalkerAliasIdentifier)
        {
            return ((TalkerAliasIdentifier)id).getValue();
        }
        return null;
    }

    /**
     * Gets patch group identifier as string
     */
    private String getPatchGroupString(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier to = ids.getToIdentifier();
        if(to instanceof PatchGroupIdentifier)
        {
            PatchGroup pg = ((PatchGroupIdentifier)to).getValue();
            if(pg != null && pg.getPatchGroup() != null)
            {
                return String.valueOf(pg.getPatchGroup().getValue());
            }
        }
        return null;
    }

    /**
     * Gets comma-separated list of patched talkgroups
     */
    private String getPatchedTalkgroups(IdentifierCollection ids)
    {
        if(ids == null) return null;
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
        return null;
    }

    /**
     * Gets ESN (Electronic Serial Number) from identifier collection
     */
    private String getEsn(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier id = ids.getIdentifier(IdentifierClass.USER, Form.ESN, Role.ANY);
        return id != null ? id.toString() : null;
    }

    /**
     * Gets unit status from identifier collection
     */
    private Integer getUnitStatus(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier id = ids.getIdentifier(IdentifierClass.USER, Form.UNIT_STATUS, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return ((IntegerIdentifier)id).getValue();
        }
        return null;
    }

    /**
     * Gets user status from identifier collection
     */
    private Integer getUserStatus(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier id = ids.getIdentifier(IdentifierClass.USER, Form.USER_STATUS, Role.ANY);
        if(id instanceof IntegerIdentifier)
        {
            return ((IntegerIdentifier)id).getValue();
        }
        return null;
    }

    /**
     * Gets telephone number from identifier collection
     */
    private String getTelephoneNumber(IdentifierCollection ids)
    {
        if(ids == null) return null;
        Identifier id = ids.getIdentifier(IdentifierClass.USER, Form.TELEPHONE_NUMBER, Role.ANY);
        return id != null ? id.toString() : null;
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

    // ============================================================
    // Null-safe SQL parameter setters
    // ============================================================

    /**
     * Sets an Integer parameter, using NULL if value is null
     */
    private void setNullableInt(PreparedStatement stmt, int index, Integer value) throws SQLException
    {
        if(value != null)
        {
            stmt.setInt(index, value);
        }
        else
        {
            stmt.setNull(index, Types.INTEGER);
        }
    }

    /**
     * Sets a Long parameter, using NULL if value is null
     */
    private void setNullableLong(PreparedStatement stmt, int index, Long value) throws SQLException
    {
        if(value != null)
        {
            stmt.setLong(index, value);
        }
        else
        {
            stmt.setNull(index, Types.INTEGER);
        }
    }

    /**
     * Sets a Double parameter, using NULL if value is null
     */
    private void setNullableDouble(PreparedStatement stmt, int index, Double value) throws SQLException
    {
        if(value != null)
        {
            stmt.setDouble(index, value);
        }
        else
        {
            stmt.setNull(index, Types.REAL);
        }
    }

    /**
     * Sets a Boolean parameter as Integer (0/1), using NULL if value is null
     */
    private void setNullableBoolean(PreparedStatement stmt, int index, Boolean value) throws SQLException
    {
        if(value != null)
        {
            stmt.setInt(index, value ? 1 : 0);
        }
        else
        {
            stmt.setNull(index, Types.INTEGER);
        }
    }

    @Override
    public Listener<IDecodeEvent> getDecodeEventListener()
    {
        return this;
    }

    /**
     * Message listener for receiving raw P25 messages
     */
    private Listener<IMessage> mMessageListener = new Listener<IMessage>()
    {
        @Override
        public void receive(IMessage message)
        {
            logMessage(message);
        }
    };

    @Override
    public Listener<IMessage> getMessageListener()
    {
        return mMessageListener;
    }

    @Override
    public void reset()
    {
        // Nothing to reset
    }
}
