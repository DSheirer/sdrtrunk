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
package io.github.dsheirer.preference.database;

import io.github.dsheirer.database.DatabaseConfiguration;
import io.github.dsheirer.database.DatabaseType;
import io.github.dsheirer.database.ageoff.AgeOffAction;
import io.github.dsheirer.database.ageoff.RetentionPeriod;
import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;

import java.util.prefs.Preferences;

/**
 * Call database selection and call audio age-off rule preferences.
 *
 * Note: the network database password is stored in plain text in the Java preferences store.
 */
public class DatabasePreference extends Preference
{
    private static final String KEY_TYPE = "database.type";
    private static final String KEY_HOST = "database.host";
    private static final String KEY_PORT = "database.port";
    private static final String KEY_DATABASE = "database.name";
    private static final String KEY_SCHEMA = "database.schema";
    private static final String KEY_USER = "database.user";
    private static final String KEY_PASSWORD = "database.password";
    private static final String KEY_AGE_RULE_ENABLED = "ageoff.age.enabled";
    private static final String KEY_AGE_RULE_DAYS = "ageoff.age.days";
    private static final String KEY_AGE_RULE_ACTION = "ageoff.age.action";
    private static final String KEY_SIZE_RULE_ENABLED = "ageoff.size.enabled";
    private static final String KEY_SIZE_RULE_LIMIT_MB = "ageoff.size.limit.mb";
    private static final String KEY_SIZE_RULE_ACTION = "ageoff.size.action";
    private static final String KEY_DUPLICATE_DNM_RETENTION = "ageoff.duplicate.dnm.retention";

    public static final String DEFAULT_POSTGRES_DATABASE = "postgres";
    public static final int DEFAULT_AGE_DAYS = 30;
    public static final int DEFAULT_SIZE_LIMIT_MB = 10 * 1024;

    private final Preferences mPreferences = Preferences.userNodeForPackage(DatabasePreference.class);

    /**
     * Constructs an instance
     * @param updateListener to receive notifications whenever these preferences change
     */
    public DatabasePreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.DATABASE;
    }

    /**
     * Currently selected call database.  Defaults to the built-in H2 database.
     */
    public DatabaseConfiguration getDatabaseConfiguration()
    {
        DatabaseType type;

        try
        {
            type = DatabaseType.valueOf(mPreferences.get(KEY_TYPE, DatabaseType.H2.name()));
        }
        catch(IllegalArgumentException iae)
        {
            type = DatabaseType.H2;
        }

        return new DatabaseConfiguration(type, mPreferences.get(KEY_HOST, "localhost"),
            mPreferences.getInt(KEY_PORT, type.getDefaultPort()),
            type.requiresDatabaseName() ? mPreferences.get(KEY_DATABASE, DEFAULT_POSTGRES_DATABASE) : "",
            mPreferences.get(KEY_SCHEMA, DatabaseConfiguration.DEFAULT_SCHEMA),
            mPreferences.get(KEY_USER, ""), mPreferences.get(KEY_PASSWORD, ""));
    }

    /**
     * Stores the selected call database.  The database manager applies the change; this only persists it.
     */
    public void setDatabaseConfiguration(DatabaseConfiguration configuration)
    {
        mPreferences.put(KEY_TYPE, configuration.type().name());
        mPreferences.put(KEY_SCHEMA, configuration.schema());

        if(configuration.type().isNetwork())
        {
            mPreferences.put(KEY_HOST, configuration.host());
            mPreferences.putInt(KEY_PORT, configuration.port());
            mPreferences.put(KEY_USER, configuration.user());
            mPreferences.put(KEY_PASSWORD, configuration.password());
        }

        if(configuration.type().requiresDatabaseName())
        {
            mPreferences.put(KEY_DATABASE, configuration.database());
        }

        notifyPreferenceUpdated();
    }

    /**
     * Last used network settings for the database type, used to pre-fill the editor when the user switches types.
     */
    public DatabaseConfiguration getStoredConfiguration(DatabaseType type)
    {
        DatabaseConfiguration current = getDatabaseConfiguration();
        int port = current.type() == type ? current.port() : type.getDefaultPort();
        return new DatabaseConfiguration(type, mPreferences.get(KEY_HOST, "localhost"), port,
            type.requiresDatabaseName() ? mPreferences.get(KEY_DATABASE, DEFAULT_POSTGRES_DATABASE) : "",
            current.schema(),
            mPreferences.get(KEY_USER, ""), mPreferences.get(KEY_PASSWORD, ""));
    }

    public boolean isAgeRuleEnabled()
    {
        return mPreferences.getBoolean(KEY_AGE_RULE_ENABLED, false);
    }

    public void setAgeRuleEnabled(boolean enabled)
    {
        mPreferences.putBoolean(KEY_AGE_RULE_ENABLED, enabled);
        notifyPreferenceUpdated();
    }

    /**
     * Maximum call age in days for the age rule
     */
    public int getAgeRuleDays()
    {
        return Math.max(1, mPreferences.getInt(KEY_AGE_RULE_DAYS, DEFAULT_AGE_DAYS));
    }

    public void setAgeRuleDays(int days)
    {
        mPreferences.putInt(KEY_AGE_RULE_DAYS, Math.max(1, days));
        notifyPreferenceUpdated();
    }

    public AgeOffAction getAgeRuleAction()
    {
        return getAction(KEY_AGE_RULE_ACTION);
    }

    public void setAgeRuleAction(AgeOffAction action)
    {
        mPreferences.put(KEY_AGE_RULE_ACTION, action.name());
        notifyPreferenceUpdated();
    }

    public boolean isSizeRuleEnabled()
    {
        return mPreferences.getBoolean(KEY_SIZE_RULE_ENABLED, false);
    }

    public void setSizeRuleEnabled(boolean enabled)
    {
        mPreferences.putBoolean(KEY_SIZE_RULE_ENABLED, enabled);
        notifyPreferenceUpdated();
    }

    /**
     * Maximum total size of call recordings in megabytes for the size rule
     */
    public int getSizeRuleLimitMB()
    {
        return Math.max(1, mPreferences.getInt(KEY_SIZE_RULE_LIMIT_MB, DEFAULT_SIZE_LIMIT_MB));
    }

    public void setSizeRuleLimitMB(int limitMB)
    {
        mPreferences.putInt(KEY_SIZE_RULE_LIMIT_MB, Math.max(1, limitMB));
        notifyPreferenceUpdated();
    }

    public AgeOffAction getSizeRuleAction()
    {
        return getAction(KEY_SIZE_RULE_ACTION);
    }

    public void setSizeRuleAction(AgeOffAction action)
    {
        mPreferences.put(KEY_SIZE_RULE_ACTION, action.name());
        notifyPreferenceUpdated();
    }

    /**
     * How long duplicate calls, and Do Not Monitor calls that aren't marked for recording by an alias, are kept
     * before the call and its recording are deleted.
     */
    public RetentionPeriod getDuplicateDoNotMonitorRetention()
    {
        try
        {
            return RetentionPeriod.valueOf(mPreferences.get(KEY_DUPLICATE_DNM_RETENTION,
                RetentionPeriod.DEFAULT.name()));
        }
        catch(IllegalArgumentException iae)
        {
            return RetentionPeriod.DEFAULT;
        }
    }

    public void setDuplicateDoNotMonitorRetention(RetentionPeriod retention)
    {
        mPreferences.put(KEY_DUPLICATE_DNM_RETENTION, retention.name());
        notifyPreferenceUpdated();
    }

    private AgeOffAction getAction(String key)
    {
        try
        {
            return AgeOffAction.valueOf(mPreferences.get(key, AgeOffAction.DELETE_CALL_AND_RECORDING.name()));
        }
        catch(IllegalArgumentException iae)
        {
            return AgeOffAction.DELETE_CALL_AND_RECORDING;
        }
    }
}
