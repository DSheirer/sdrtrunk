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
package io.github.dsheirer.settings.database;

import io.github.dsheirer.database.DatabaseConfiguration;
import io.github.dsheirer.database.DatabaseType;
import io.github.dsheirer.database.ageoff.AgeOffAction;
import io.github.dsheirer.database.ageoff.RetentionPeriod;
import io.github.dsheirer.database.backup.BackupInterval;
import io.github.dsheirer.settings.AbstractSettings;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.sample.Listener;

/**
 * Call database selection and call audio age-off rule settings.
 *
 * Note: the network database password is stored in plain text in the Java preferences store.
 */
public class DatabaseSettings extends AbstractSettings
{

    public static final String DEFAULT_POSTGRES_DATABASE = "postgres";
    public static final int DEFAULT_AGE_DAYS = 30;
    public static final int DEFAULT_EVENT_AGE_DAYS = 30;
    public static final int DEFAULT_STREAM_ACTIVITY_AGE_DAYS = 3;
    public static final int DEFAULT_BACKUP_RETAIN = 7;
    public static final int MAX_BACKUP_RETAIN = 365;
    public static final int DEFAULT_SIZE_LIMIT_MB = 10 * 1024;

    /**
     * Constructs an instance
     * @param updateListener to receive notifications whenever these settings change
     */
    public DatabaseSettings(Listener<SettingsType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public SettingsType getSettingsType()
    {
        return SettingsType.DATABASE;
    }

    /**
     * Currently selected call database.  Defaults to the built-in H2 database.
     */
    public DatabaseConfiguration getDatabaseConfiguration()
    {
        DatabaseType type;

        try
        {
            type = DatabaseType.valueOf(Setting.DATABASE_TYPE.get(DatabaseType.H2.name()));
        }
        catch(IllegalArgumentException iae)
        {
            type = DatabaseType.H2;
        }

        return new DatabaseConfiguration(type, Setting.DATABASE_HOST.get("localhost"),
            Setting.DATABASE_PORT.getInt(type.getDefaultPort()),
            type.requiresDatabaseName() ? Setting.DATABASE_NAME.get(DEFAULT_POSTGRES_DATABASE) : "",
            Setting.DATABASE_SCHEMA.get(DatabaseConfiguration.DEFAULT_SCHEMA),
            Setting.DATABASE_USER.get(""), Setting.DATABASE_PASSWORD.get(""));
    }

    /**
     * Stores the selected call database.  The database manager applies the change; this only persists it.
     */
    public void setDatabaseConfiguration(DatabaseConfiguration configuration)
    {
        Setting.DATABASE_TYPE.put(configuration.type().name());
        Setting.DATABASE_SCHEMA.put(configuration.schema());

        if(configuration.type().isNetwork())
        {
            Setting.DATABASE_HOST.put(configuration.host());
            Setting.DATABASE_PORT.putInt(configuration.port());
            Setting.DATABASE_USER.put(configuration.user());
            Setting.DATABASE_PASSWORD.put(configuration.password());
        }

        if(configuration.type().requiresDatabaseName())
        {
            Setting.DATABASE_NAME.put(configuration.database());
        }

        notifySettingsUpdated();
    }

    /**
     * Last used network settings for the database type, used to pre-fill the editor when the user switches types.
     */
    public DatabaseConfiguration getStoredConfiguration(DatabaseType type)
    {
        DatabaseConfiguration current = getDatabaseConfiguration();
        int port = current.type() == type ? current.port() : type.getDefaultPort();
        return new DatabaseConfiguration(type, Setting.DATABASE_HOST.get("localhost"), port,
            type.requiresDatabaseName() ? Setting.DATABASE_NAME.get(DEFAULT_POSTGRES_DATABASE) : "",
            current.schema(),
            Setting.DATABASE_USER.get(""), Setting.DATABASE_PASSWORD.get(""));
    }

    /**
     * How often the database is backed up automatically
     */
    public BackupInterval getBackupInterval()
    {
        try
        {
            return BackupInterval.valueOf(Setting.DATABASE_BACKUP_INTERVAL.get(BackupInterval.DEFAULT.name()));
        }
        catch(IllegalArgumentException iae)
        {
            return BackupInterval.DEFAULT;
        }
    }

    public void setBackupInterval(BackupInterval interval)
    {
        Setting.DATABASE_BACKUP_INTERVAL.put(interval.name());
        notifySettingsUpdated();
    }

    /**
     * Number of backup files to keep.  The oldest backups are deleted after a backup when there are more.
     */
    public int getBackupRetain()
    {
        return Math.clamp(Setting.DATABASE_BACKUP_RETAIN.getInt(DEFAULT_BACKUP_RETAIN), 1, MAX_BACKUP_RETAIN);
    }

    public void setBackupRetain(int retain)
    {
        Setting.DATABASE_BACKUP_RETAIN.putInt(Math.clamp(retain, 1, MAX_BACKUP_RETAIN));
        notifySettingsUpdated();
    }

    /**
     * Indicates if the event age rule (delete decode events older than a number of days) is enabled.  Enabled by
     * default, because a busy control channel produces many events.
     */
    public boolean isEventRuleEnabled()
    {
        return Setting.DATABASE_EVENT_RULE_ENABLED.getBoolean(true);
    }

    public void setEventRuleEnabled(boolean enabled)
    {
        Setting.DATABASE_EVENT_RULE_ENABLED.putBoolean(enabled);
        notifySettingsUpdated();
    }

    /**
     * Maximum decode event age in days for the event age rule
     */
    public int getEventRuleDays()
    {
        return Math.max(1, Setting.DATABASE_EVENT_RULE_DAYS.getInt(DEFAULT_EVENT_AGE_DAYS));
    }

    public void setEventRuleDays(int days)
    {
        Setting.DATABASE_EVENT_RULE_DAYS.putInt(Math.max(1, days));
        notifySettingsUpdated();
    }

    /**
     * Indicates if the stream activity age rule (delete the streaming tab rows of calls older than a number of days)
     * is enabled.  Enabled by default: the rows are for troubleshooting streaming, so they are kept for a short time.
     */
    public boolean isStreamActivityRuleEnabled()
    {
        return Setting.DATABASE_STREAM_ACTIVITY_RULE_ENABLED.getBoolean(true);
    }

    public void setStreamActivityRuleEnabled(boolean enabled)
    {
        Setting.DATABASE_STREAM_ACTIVITY_RULE_ENABLED.putBoolean(enabled);
        notifySettingsUpdated();
    }

    /**
     * Maximum stream activity age in days for the stream activity age rule
     */
    public int getStreamActivityRuleDays()
    {
        return Math.max(1, Setting.DATABASE_STREAM_ACTIVITY_RULE_DAYS.getInt(DEFAULT_STREAM_ACTIVITY_AGE_DAYS));
    }

    public void setStreamActivityRuleDays(int days)
    {
        Setting.DATABASE_STREAM_ACTIVITY_RULE_DAYS.putInt(Math.max(1, days));
        notifySettingsUpdated();
    }

    public boolean isAgeRuleEnabled()
    {
        return Setting.DATABASE_AGE_RULE_ENABLED.getBoolean(false);
    }

    public void setAgeRuleEnabled(boolean enabled)
    {
        Setting.DATABASE_AGE_RULE_ENABLED.putBoolean(enabled);
        notifySettingsUpdated();
    }

    /**
     * Maximum call age in days for the age rule
     */
    public int getAgeRuleDays()
    {
        return Math.max(1, Setting.DATABASE_AGE_RULE_DAYS.getInt(DEFAULT_AGE_DAYS));
    }

    public void setAgeRuleDays(int days)
    {
        Setting.DATABASE_AGE_RULE_DAYS.putInt(Math.max(1, days));
        notifySettingsUpdated();
    }

    public AgeOffAction getAgeRuleAction()
    {
        return getAction(Setting.DATABASE_AGE_RULE_ACTION);
    }

    public void setAgeRuleAction(AgeOffAction action)
    {
        Setting.DATABASE_AGE_RULE_ACTION.put(action.name());
        notifySettingsUpdated();
    }

    public boolean isSizeRuleEnabled()
    {
        return Setting.DATABASE_SIZE_RULE_ENABLED.getBoolean(false);
    }

    public void setSizeRuleEnabled(boolean enabled)
    {
        Setting.DATABASE_SIZE_RULE_ENABLED.putBoolean(enabled);
        notifySettingsUpdated();
    }

    /**
     * Maximum total size of call recordings in megabytes for the size rule
     */
    public int getSizeRuleLimitMB()
    {
        return Math.max(1, Setting.DATABASE_SIZE_RULE_LIMIT_MB.getInt(DEFAULT_SIZE_LIMIT_MB));
    }

    public void setSizeRuleLimitMB(int limitMB)
    {
        Setting.DATABASE_SIZE_RULE_LIMIT_MB.putInt(Math.max(1, limitMB));
        notifySettingsUpdated();
    }

    public AgeOffAction getSizeRuleAction()
    {
        return getAction(Setting.DATABASE_SIZE_RULE_ACTION);
    }

    public void setSizeRuleAction(AgeOffAction action)
    {
        Setting.DATABASE_SIZE_RULE_ACTION.put(action.name());
        notifySettingsUpdated();
    }

    /**
     * How long duplicate calls, and Do Not Monitor calls that aren't marked for recording by an alias, are kept
     * before the call and its recording are deleted.
     */
    public RetentionPeriod getDuplicateDoNotMonitorRetention()
    {
        try
        {
            return RetentionPeriod.valueOf(Setting.DATABASE_DUPLICATE_DNM_RETENTION.get(RetentionPeriod.DEFAULT.name()));
        }
        catch(IllegalArgumentException iae)
        {
            return RetentionPeriod.DEFAULT;
        }
    }

    public void setDuplicateDoNotMonitorRetention(RetentionPeriod retention)
    {
        Setting.DATABASE_DUPLICATE_DNM_RETENTION.put(retention.name());
        notifySettingsUpdated();
    }

    private AgeOffAction getAction(Setting setting)
    {
        try
        {
            return AgeOffAction.valueOf(setting.get(AgeOffAction.DELETE_CALL_AND_RECORDING.name()));
        }
        catch(IllegalArgumentException iae)
        {
            return AgeOffAction.DELETE_CALL_AND_RECORDING;
        }
    }
}
