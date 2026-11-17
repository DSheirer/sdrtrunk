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

package io.github.dsheirer.alias;

import com.google.common.annotations.VisibleForTesting;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.radio.NXDNTypeDRadio;
import io.github.dsheirer.alias.id.radio.P25FullyQualifiedRadio;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.identifier.alias.TalkerAliasIdentifier;
import io.github.dsheirer.identifier.radio.FullyQualifiedRadioIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.protocol.Protocol;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Automatically adds decoded talker aliases to an alias list as radio aliases, using the talker alias as the alias name,
 * and renames a stored radio alias when the radio's talker alias changes.
 *
 * Talker aliases span up to 4 messages, so a decoded talker alias can be a fragment (the leading characters) of the
 * full alias, even when the decoder reports it as complete.  To avoid replacing a full alias with a fragment:
 * <ul>
 *     <li>A decoded alias that is the start of the stored alias name is a fragment and never replaces it.</li>
 *     <li>A decoded alias that starts with the stored alias name is a longer version of a stored fragment and
 *     replaces it.</li>
 *     <li>Any other decoded alias that differs from the stored alias name replaces it only after it is decoded
 *     {@link #CONFIRMATIONS_REQUIRED} times in a row for the radio.</li>
 * </ul>
 *
 * Only aliases whose only identifier is the radio are created or renamed.  An alias that combines the radio with other
 * identifiers (e.g. several radios or talkgroups) is left alone.
 *
 * This class is thread safe.  Talker aliases are received on decoder threads and the alias model is updated on the
 * JavaFX application thread.
 */
public class TalkerAliasAutoAdder implements BiConsumer<RadioIdentifier,TalkerAliasIdentifier>
{
    private static final Logger mLog = LoggerFactory.getLogger(TalkerAliasAutoAdder.class);

    /**
     * Group assigned to aliases created from talker aliases.
     */
    public static final String ALIAS_GROUP = "Talker Alias";

    /**
     * Number of consecutive times a different talker alias must be decoded for a radio before it replaces the stored
     * alias name.
     */
    public static final int CONFIRMATIONS_REQUIRED = 2;

    /**
     * Interval for re-checking the stored alias for a talker alias that was already checked, so that a renamed or
     * deleted alias is restored without checking the alias list for every decoded talker alias.
     */
    private static final long RECHECK_INTERVAL_MS = 30_000;

    /**
     * Outcome of comparing a decoded talker alias with the stored alias name.
     */
    public enum Decision
    {
        /**
         * The stored alias name matches the talker alias.
         */
        UNCHANGED,
        /**
         * The talker alias is a fragment of the stored alias name.
         */
        FRAGMENT,
        /**
         * The talker alias differs from the stored alias name and is waiting for more decodes.
         */
        AWAIT_CONFIRMATION,
        /**
         * The talker alias replaces the stored alias name.
         */
        UPDATE
    }

    private final AliasModel mAliasModel;
    private final String mAliasListName;
    //Guarded by this
    private final Map<String,Candidate> mCandidates = new HashMap<>();
    private final Map<String,Checked> mChecked = new HashMap<>();

    /**
     * Constructs an instance
     * @param aliasModel to add aliases to
     * @param aliasListName of the alias list to add aliases to
     */
    public TalkerAliasAutoAdder(AliasModel aliasModel, String aliasListName)
    {
        mAliasModel = aliasModel;
        mAliasListName = aliasListName;
    }

    /**
     * Name of the alias list that talker aliases are added to.
     */
    public String getAliasListName()
    {
        return mAliasListName;
    }

    /**
     * Processes a talker alias that is assigned to a radio.
     * @param radio that the talker alias belongs to
     * @param talkerAlias for the radio
     */
    @Override
    public void accept(RadioIdentifier radio, TalkerAliasIdentifier talkerAlias)
    {
        if(radio == null || talkerAlias == null || mAliasListName == null || mAliasListName.isEmpty())
        {
            return;
        }

        String value = normalize(talkerAlias.getValue());
        Radio aliasId = createAliasId(radio);

        if(value == null || aliasId == null || !aliasId.isValid())
        {
            return;
        }

        String key = getKey(aliasId);
        long now = System.currentTimeMillis();
        int confirmations;

        synchronized(this)
        {
            Checked checked = mChecked.get(key);

            if(checked != null && checked.value().equals(value) && (now - checked.timestamp()) < RECHECK_INTERVAL_MS)
            {
                return;
            }

            confirmations = confirm(key, value);
        }

        runOnFxThread(() -> apply(radio, aliasId, key, value, confirmations));
    }

    /**
     * Counts the consecutive decodes of the talker alias for the radio.  A fragment of the talker alias that is
     * waiting for confirmation doesn't interrupt the count, because fragments are decoded while the full alias is
     * assembled.
     * @return number of consecutive decodes of the value, including this one.
     */
    private int confirm(String key, String value)
    {
        Candidate candidate = mCandidates.get(key);

        if(candidate != null && candidate.value().equals(value))
        {
            candidate = new Candidate(value, candidate.count() + 1);
            mCandidates.put(key, candidate);
            return candidate.count();
        }

        if(candidate != null && isFragment(value, candidate.value()))
        {
            return 1;
        }

        mCandidates.put(key, new Candidate(value, 1));
        return 1;
    }

    /**
     * Creates or updates the alias for the radio.  Runs on the JavaFX application thread.
     */
    private synchronized void apply(RadioIdentifier radio, Radio aliasId, String key, String value, int confirmations)
    {
        AliasList aliasList = mAliasModel.getAliasList(mAliasListName);
        Alias existing = findExactMatch(aliasList.getAliases(radio), aliasId);

        if(existing == null)
        {
            Alias alias = new Alias(value);
            alias.setAliasListName(mAliasListName);
            alias.setGroup(ALIAS_GROUP);
            alias.addAliasID(aliasId);
            mAliasModel.addAlias(alias);
            markChecked(key, value);
            mLog.info("Talker alias [" + value + "] added for radio [" + aliasId + "] to alias list [" +
                mAliasListName + "]");
            return;
        }

        if(getIdentityCount(existing) > 1)
        {
            //The alias also identifies other radios or talkgroups - leave it alone.
            markChecked(key, value);
            return;
        }

        String stored = normalize(existing.getName());

        switch(decide(stored, value, confirmations))
        {
            case UPDATE:
                existing.setName(value);
                markChecked(key, value);
                mLog.info("Talker alias for radio [" + aliasId + "] in alias list [" + mAliasListName +
                    "] changed from [" + stored + "] to [" + value + "]");
                break;
            case UNCHANGED:
            case FRAGMENT:
                markChecked(key, value);
                break;
            case AWAIT_CONFIRMATION:
            default:
                break;
        }
    }

    private void markChecked(String key, String value)
    {
        mChecked.put(key, new Checked(value, System.currentTimeMillis()));
        mCandidates.remove(key);
    }

    /**
     * Compares the decoded talker alias with the stored alias name.
     * @param stored alias name, normalized, or null
     * @param decoded talker alias, normalized
     * @param confirmations number of consecutive decodes of the talker alias
     * @return decision
     */
    @VisibleForTesting
    static Decision decide(String stored, String decoded, int confirmations)
    {
        if(stored == null || stored.isEmpty())
        {
            return Decision.UPDATE;
        }

        if(stored.equals(decoded))
        {
            return Decision.UNCHANGED;
        }

        if(isFragment(decoded, stored))
        {
            return Decision.FRAGMENT;
        }

        //A longer version of a stored fragment.
        if(isFragment(stored, decoded) && !hasUndecodableCharacters(decoded))
        {
            return Decision.UPDATE;
        }

        //A different alias that may be a corrupted or mis-assembled alias.  It's replaced only once confirmed, and an
        //alias that has undecodable characters (e.g. a partial multi-byte character) never replaces a stored alias.
        if(confirmations >= CONFIRMATIONS_REQUIRED && !hasUndecodableCharacters(decoded))
        {
            return Decision.UPDATE;
        }

        return Decision.AWAIT_CONFIRMATION;
    }

    /**
     * Indicates if the value is a fragment (the leading characters) of the full value.
     */
    @VisibleForTesting
    static boolean isFragment(String value, String full)
    {
        return value != null && full != null && value.length() < full.length() && full.startsWith(value);
    }

    private static boolean hasUndecodableCharacters(String value)
    {
        return value.indexOf('�') >= 0;
    }

    /**
     * Removes padding, control characters and trailing undecodable characters (e.g. from a partial multi-byte
     * character at the end of an alias fragment) from the talker alias.
     * @param value to normalize
     * @return normalized value or null if the value is empty.
     */
    @VisibleForTesting
    static String normalize(String value)
    {
        if(value == null)
        {
            return null;
        }

        StringBuilder sb = new StringBuilder(value.length());

        for(int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);

            if(!Character.isISOControl(c))
            {
                sb.append(c);
            }
        }

        int end = sb.length();

        while(end > 0 && (sb.charAt(end - 1) == '�' || Character.isWhitespace(sb.charAt(end - 1))))
        {
            end--;
        }

        String normalized = sb.substring(0, end).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Creates a radio alias identifier that exactly matches the radio.
     * @param radio identifier
     * @return alias identifier or null if the radio protocol isn't known.
     */
    @VisibleForTesting
    static Radio createAliasId(RadioIdentifier radio)
    {
        if(radio instanceof FullyQualifiedRadioIdentifier fqri && fqri.getProtocol() == Protocol.APCO25)
        {
            return new P25FullyQualifiedRadio(fqri.getWacn(), fqri.getSystem(), fqri.getRadio());
        }

        if(radio instanceof NXDNRadioIdentifier nxdn && nxdn.isTypeD())
        {
            return new NXDNTypeDRadio(nxdn.getTypeDHomeRepeater(), nxdn.getTypeDRadio());
        }

        Protocol protocol = radio.getProtocol();

        if(protocol == null || protocol == Protocol.UNKNOWN || radio.getValue() == null)
        {
            return null;
        }

        return new Radio(protocol, radio.getValue());
    }

    /**
     * Finds the alias that has an identifier that exactly matches the radio (i.e. not a radio range).
     */
    @VisibleForTesting
    static Alias findExactMatch(List<Alias> aliases, Radio aliasId)
    {
        if(aliases != null)
        {
            for(Alias alias: aliases)
            {
                for(AliasID id: alias.getAliasIdentifiers())
                {
                    if(isExactMatch(id, aliasId))
                    {
                        return alias;
                    }
                }
            }
        }

        return null;
    }

    private static boolean isExactMatch(AliasID id, Radio aliasId)
    {
        if(aliasId instanceof P25FullyQualifiedRadio target)
        {
            return id instanceof P25FullyQualifiedRadio fqr && fqr.getHashKey().equals(target.getHashKey());
        }

        return id instanceof Radio radio && !(id instanceof P25FullyQualifiedRadio) &&
            radio.getProtocol() == aliasId.getProtocol() && radio.getValue() == aliasId.getValue();
    }

    /**
     * Number of identifiers (e.g. radios, talkgroups) in the alias, excluding audio settings like priority, record and
     * streaming.
     */
    private static int getIdentityCount(Alias alias)
    {
        int count = 0;

        for(AliasID id: alias.getAliasIdentifiers())
        {
            if(!id.isAudioIdentifier())
            {
                count++;
            }
        }

        return count;
    }

    private static String getKey(Radio aliasId)
    {
        if(aliasId instanceof P25FullyQualifiedRadio fqr)
        {
            return aliasId.getProtocol() + ":" + fqr.getHashKey();
        }

        return aliasId.getProtocol() + ":" + aliasId.getValue();
    }

    /**
     * Runs on the FX thread, or on this thread when the FX toolkit isn't running (headless).
     */
    private static void runOnFxThread(Runnable runnable)
    {
        if(Platform.isFxApplicationThread())
        {
            runnable.run();
            return;
        }

        try
        {
            Platform.runLater(runnable);
        }
        catch(IllegalStateException ise)
        {
            runnable.run();
        }
    }

    private record Candidate(String value, int count) {}
    private record Checked(String value, long timestamp) {}
}
