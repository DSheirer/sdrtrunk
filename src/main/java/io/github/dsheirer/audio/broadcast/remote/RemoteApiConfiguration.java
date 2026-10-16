/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * *****************************************************************************
 */
package io.github.dsheirer.audio.broadcast.remote;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Configuration for a generic completed-call HTTP multipart destination.
 * Secrets can be sourced from an environment variable so they do not have to be persisted in the playlist.
 */
public class RemoteApiConfiguration extends BroadcastConfiguration
{
    public static final String DEFAULT_API_KEY_ENVIRONMENT_VARIABLE = "SDRTRUNK_REMOTE_API_KEY";
    public static final String DEFAULT_OPENAI_KEY_ENVIRONMENT_VARIABLE = "OPENAI_API_KEY";

    private final StringProperty mApiKey = new SimpleStringProperty();
    private final StringProperty mApiKeyEnvironmentVariable =
        new SimpleStringProperty(DEFAULT_API_KEY_ENVIRONMENT_VARIABLE);
    private final StringProperty mAuthenticationHeader = new SimpleStringProperty("Authorization");
    private final StringProperty mAuthenticationPrefix = new SimpleStringProperty("Bearer ");
    private final IntegerProperty mMaximumRetries = new SimpleIntegerProperty(5);
    private final IntegerProperty mMaximumConcurrentUploads = new SimpleIntegerProperty(2);
    private final IntegerProperty mRequestTimeoutSeconds = new SimpleIntegerProperty(60);
    private final BooleanProperty mRetryIndefinitely = new SimpleBooleanProperty(true);
    private final BooleanProperty mHeartbeatEnabled = new SimpleBooleanProperty(false);
    private final IntegerProperty mHeartbeatIntervalSeconds = new SimpleIntegerProperty(60);
    private final BooleanProperty mOpenAiEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty mTranslateToEnglish = new SimpleBooleanProperty(false);
    private final StringProperty mOpenAiKeyEnvironmentVariable =
        new SimpleStringProperty(DEFAULT_OPENAI_KEY_ENVIRONMENT_VARIABLE);
    private final StringProperty mLocalWhisperExecutable = new SimpleStringProperty();
    private final StringProperty mLocalWhisperModel = new SimpleStringProperty();

    public RemoteApiConfiguration()
    {
        this(BroadcastFormat.MP3);
    }

    public RemoteApiConfiguration(BroadcastFormat format)
    {
        super(format);
        mValid.unbind();
        mValid.bind(Bindings.and(Bindings.isNotEmpty(mHost), Bindings.isNotEmpty(mName)));
    }

    @JacksonXmlProperty(isAttribute = true, localName = "api_key")
    public String getApiKey() { return mApiKey.get(); }
    public void setApiKey(String value) { mApiKey.set(value); }
    public StringProperty apiKeyProperty() { return mApiKey; }

    @JacksonXmlProperty(isAttribute = true, localName = "api_key_environment_variable")
    public String getApiKeyEnvironmentVariable() { return mApiKeyEnvironmentVariable.get(); }
    public void setApiKeyEnvironmentVariable(String value) { mApiKeyEnvironmentVariable.set(value); }

    @JacksonXmlProperty(isAttribute = true, localName = "authentication_header")
    public String getAuthenticationHeader() { return mAuthenticationHeader.get(); }
    public void setAuthenticationHeader(String value) { mAuthenticationHeader.set(value); }

    @JacksonXmlProperty(isAttribute = true, localName = "authentication_prefix")
    public String getAuthenticationPrefix() { return mAuthenticationPrefix.get(); }
    public void setAuthenticationPrefix(String value) { mAuthenticationPrefix.set(value); }

    @JacksonXmlProperty(isAttribute = true, localName = "maximum_retries")
    public int getMaximumRetries() { return mMaximumRetries.get(); }
    public void setMaximumRetries(int value) { mMaximumRetries.set(Math.max(0, value)); }

    @JacksonXmlProperty(isAttribute = true, localName = "maximum_concurrent_uploads")
    public int getMaximumConcurrentUploads() { return mMaximumConcurrentUploads.get(); }
    public void setMaximumConcurrentUploads(int value) { mMaximumConcurrentUploads.set(Math.max(1, value)); }

    @JacksonXmlProperty(isAttribute = true, localName = "request_timeout_seconds")
    public int getRequestTimeoutSeconds() { return mRequestTimeoutSeconds.get(); }
    public void setRequestTimeoutSeconds(int value) { mRequestTimeoutSeconds.set(Math.max(1, value)); }

    @JacksonXmlProperty(isAttribute = true, localName = "retry_indefinitely")
    public boolean isRetryIndefinitely() { return mRetryIndefinitely.get(); }
    public void setRetryIndefinitely(boolean value) { mRetryIndefinitely.set(value); }

    @JacksonXmlProperty(isAttribute = true, localName = "heartbeat_enabled")
    public boolean isHeartbeatEnabled() { return mHeartbeatEnabled.get(); }
    public void setHeartbeatEnabled(boolean value) { mHeartbeatEnabled.set(value); }

    @JacksonXmlProperty(isAttribute = true, localName = "heartbeat_interval_seconds")
    public int getHeartbeatIntervalSeconds() { return mHeartbeatIntervalSeconds.get(); }
    public void setHeartbeatIntervalSeconds(int value) { mHeartbeatIntervalSeconds.set(Math.max(5, value)); }

    @JacksonXmlProperty(isAttribute = true, localName = "openai_enabled")
    public boolean isOpenAiEnabled() { return mOpenAiEnabled.get(); }
    public void setOpenAiEnabled(boolean value) { mOpenAiEnabled.set(value); }

    @JacksonXmlProperty(isAttribute = true, localName = "translate_to_english")
    public boolean isTranslateToEnglish() { return mTranslateToEnglish.get(); }
    public void setTranslateToEnglish(boolean value) { mTranslateToEnglish.set(value); }

    @JacksonXmlProperty(isAttribute = true, localName = "openai_key_environment_variable")
    public String getOpenAiKeyEnvironmentVariable() { return mOpenAiKeyEnvironmentVariable.get(); }
    public void setOpenAiKeyEnvironmentVariable(String value) { mOpenAiKeyEnvironmentVariable.set(value); }

    @JacksonXmlProperty(isAttribute = true, localName = "local_whisper_executable")
    public String getLocalWhisperExecutable() { return mLocalWhisperExecutable.get(); }
    public void setLocalWhisperExecutable(String value) { mLocalWhisperExecutable.set(value); }

    @JacksonXmlProperty(isAttribute = true, localName = "local_whisper_model")
    public String getLocalWhisperModel() { return mLocalWhisperModel.get(); }
    public void setLocalWhisperModel(String value) { mLocalWhisperModel.set(value); }

    public String resolveApiKey()
    {
        String environmentName = getApiKeyEnvironmentVariable();
        String environmentValue = environmentName != null ? System.getenv(environmentName) : null;
        return environmentValue != null && !environmentValue.isBlank() ? environmentValue : getApiKey();
    }

    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.REMOTE_CALL_API;
    }

    @Override
    public BroadcastConfiguration copyOf()
    {
        RemoteApiConfiguration copy = new RemoteApiConfiguration(getBroadcastFormat());
        copy.setName(getName());
        copy.setHost(getHost());
        copy.setEnabled(isEnabled());
        copy.setMaximumRecordingAge(getMaximumRecordingAge());
        copy.setApiKey(getApiKey());
        copy.setApiKeyEnvironmentVariable(getApiKeyEnvironmentVariable());
        copy.setAuthenticationHeader(getAuthenticationHeader());
        copy.setAuthenticationPrefix(getAuthenticationPrefix());
        copy.setMaximumRetries(getMaximumRetries());
        copy.setMaximumConcurrentUploads(getMaximumConcurrentUploads());
        copy.setRequestTimeoutSeconds(getRequestTimeoutSeconds());
        copy.setRetryIndefinitely(isRetryIndefinitely());
        copy.setHeartbeatEnabled(isHeartbeatEnabled());
        copy.setHeartbeatIntervalSeconds(getHeartbeatIntervalSeconds());
        copy.setOpenAiEnabled(isOpenAiEnabled());
        copy.setTranslateToEnglish(isTranslateToEnglish());
        copy.setOpenAiKeyEnvironmentVariable(getOpenAiKeyEnvironmentVariable());
        copy.setLocalWhisperExecutable(getLocalWhisperExecutable());
        copy.setLocalWhisperModel(getLocalWhisperModel());
        return copy;
    }
}
