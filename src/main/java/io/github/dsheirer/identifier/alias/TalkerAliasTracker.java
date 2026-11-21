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
package io.github.dsheirer.identifier.alias;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Keeps the talker alias in a decoder's identifier collection matched to the radio that is talking, and assigns each
 * received talker alias to the right radio in the talker alias manager.  Use one instance for each decoder state (or
 * timeslot) on the decoder state's thread.
 *
 * Talker aliases can arrive:
 * <ul>
 *     <li>During the call, after the talking radio is identified (most protocols).</li>
 *     <li>During the call, before the talking radio is identified.  The alias is shown for the call and stored for
 *     the radio once the radio is identified.</li>
 *     <li>Between calls.  Depending on the protocol, the alias belongs to the talker of the call that just ended (e.g.
 *     P25 Phase 1 L3Harris aliases in the TDULC messages) or to the talker of the next call (e.g. P25 Phase 2
 *     L3Harris aliases that arrive just before the Push-To-Talk).  See {@link BetweenCalls}.</li>
 *     <li>With their own radio identifier (e.g. Motorola), which is stored for that radio.</li>
 * </ul>
 *
 * The decoder state calls {@link #callActivity(long)} after it updates the identifier collection for call activity,
 * {@link #callEnded()} when a call ends, before it removes the FROM identifiers, and {@link #received} for each
 * talker alias.
 */
public class TalkerAliasTracker
{
    /**
     * Talker that an alias received between calls belongs to
     */
    public enum BetweenCalls
    {
        /**
         * The talker of the call that just ended
         */
        PREVIOUS_TALKER,
        /**
         * The talker of the next call, when it starts within {@link #NEXT_CALL_WINDOW_MS}
         */
        NEXT_TALKER
    }

    /**
     * Maximum time from a talker alias received between calls to the start of the next call, for the alias to belong
     * to the next call's talker.
     */
    public static final long NEXT_CALL_WINDOW_MS = 2000;

    /**
     * Call that a received talker alias belongs to
     */
    public enum Assignment
    {
        /**
         * The call in progress.  The alias is shown in the identifier collection.
         */
        CURRENT_CALL,
        /**
         * The call that just ended.  The alias isn't shown, since no call is in progress.
         */
        ENDED_CALL,
        /**
         * The next call.  The alias is held and shown when the next call starts.
         */
        NEXT_CALL,
        /**
         * Neither, e.g. an alias for a radio that isn't talking.  The alias is stored for its radio, if known.
         */
        UNASSIGNED
    }

    private final MutableIdentifierCollection mIdentifierCollection;
    private final Supplier<TalkerAliasManager> mTalkerAliasManager;
    private final BetweenCalls mBetweenCalls;
    private boolean mCallActive;
    private TalkerAliasIdentifier mNextCallAlias;
    private long mNextCallAliasTimestamp;
    private Identifier mTalker;
    private RadioIdentifier mEndedCallRadio;

    /**
     * Constructs an instance
     * @param identifierCollection of the decoder state
     * @param talkerAliasManager for storing the talker alias of each radio.  The supplier can return null when the
     * decoder doesn't have a talker alias manager.
     * @param betweenCalls talker that an alias received between calls belongs to, for the protocol
     */
    public TalkerAliasTracker(MutableIdentifierCollection identifierCollection,
                              Supplier<TalkerAliasManager> talkerAliasManager, BetweenCalls betweenCalls)
    {
        mIdentifierCollection = identifierCollection;
        mTalkerAliasManager = talkerAliasManager;
        mBetweenCalls = betweenCalls;
    }

    /**
     * Signals call activity, after the decoder updates the identifier collection from a call message.  Keeps the
     * talker alias matched to the talking radio: removes the alias when a different radio talks, stores an alias that
     * arrived before the talking radio was identified, and adds the stored alias for the talking radio.
     * @param timestamp of the call message
     */
    public void callActivity(long timestamp)
    {
        if(!mCallActive && mNextCallAlias != null && Math.abs(timestamp - mNextCallAliasTimestamp) <= NEXT_CALL_WINDOW_MS)
        {
            //The alias received just before this call belongs to this call's talker.  It's stored for the talker once
            //the talker is identified.
            mIdentifierCollection.update(mNextCallAlias);
        }

        mNextCallAlias = null;
        mCallActive = true;
        mEndedCallRadio = null;
        update();
    }

    /**
     * Keeps the talker alias matched to the talking radio after an identifier collection update, without signaling
     * call activity.
     */
    public void update()
    {
        Identifier current = getFromRadio();

        if(current != null)
        {
            Identifier alias = getTalkerAlias();
            TalkerAliasManager manager = mTalkerAliasManager.get();

            if(mTalker != null && !isSameRadio(mTalker, current))
            {
                //A different radio is talking
                mIdentifierCollection.remove(IdentifierClass.USER, Form.TALKER_ALIAS, Role.FROM);
            }
            else if(mTalker == null && alias instanceof TalkerAliasIdentifier talkerAlias &&
                    current instanceof RadioIdentifier radio && manager != null)
            {
                //The alias arrived during this call before the radio was identified
                manager.update(radio, talkerAlias);
            }

            if(manager != null)
            {
                manager.enrichMutable(mIdentifierCollection);
            }
        }

        mTalker = current;
    }

    /**
     * Signals the end of the call.  Call before removing the FROM identifiers from the identifier collection.
     * Remembers the talker for an alias that arrives after the call, and removes the talker alias.  Can be called
     * more than once for the same call.
     */
    public void callEnded()
    {
        Identifier current = getFromRadio();

        if(current instanceof RadioIdentifier radio)
        {
            mEndedCallRadio = radio;
        }
        else if(mTalker instanceof RadioIdentifier radio)
        {
            mEndedCallRadio = radio;
        }

        mCallActive = false;
        mTalker = null;
        mIdentifierCollection.remove(IdentifierClass.USER, Form.TALKER_ALIAS, Role.FROM);
    }

    /**
     * Resets the tracker, e.g. when the decoder state resets.
     */
    public void reset()
    {
        mCallActive = false;
        mTalker = null;
        mEndedCallRadio = null;
        mNextCallAlias = null;
    }

    /**
     * Processes a received talker alias: stores it for its radio and shows it when the radio is talking.
     *
     * @param talkerAlias that was received
     * @param aliasRadio that the alias identifies itself (e.g. Motorola), or null when the alias belongs to the
     * talker of the current call, or of the previous or next call, depending on the protocol.
     * @param timestamp of the alias message
     * @return the call that the alias belongs to.
     */
    public Assignment received(TalkerAliasIdentifier talkerAlias, RadioIdentifier aliasRadio, long timestamp)
    {
        if(talkerAlias == null)
        {
            return Assignment.UNASSIGNED;
        }

        Identifier current = getFromRadio();
        RadioIdentifier radio = aliasRadio;

        if(radio == null)
        {
            if(current instanceof RadioIdentifier currentRadio)
            {
                radio = currentRadio;
            }
            else if(!mCallActive)
            {
                if(mBetweenCalls == BetweenCalls.NEXT_TALKER)
                {
                    //Hold the alias for the next call's talker
                    mNextCallAlias = talkerAlias;
                    mNextCallAliasTimestamp = timestamp;
                    return Assignment.NEXT_CALL;
                }

                radio = mEndedCallRadio;
            }
        }

        TalkerAliasManager manager = mTalkerAliasManager.get();

        if(radio != null && manager != null)
        {
            manager.update(radio, talkerAlias);
        }

        if(current != null && radio != null && isSameRadio(current, radio))
        {
            mIdentifierCollection.update(talkerAlias);
            return Assignment.CURRENT_CALL;
        }

        if(radio == null && mCallActive)
        {
            //The talking radio isn't identified yet.  The alias is stored for the radio once it's identified.
            mIdentifierCollection.update(talkerAlias);
            return Assignment.CURRENT_CALL;
        }

        if(radio != null && !mCallActive && mEndedCallRadio != null && isSameRadio(radio, mEndedCallRadio))
        {
            return Assignment.ENDED_CALL;
        }

        return Assignment.UNASSIGNED;
    }

    /**
     * Talker of the call that just ended, or null.
     */
    public RadioIdentifier getEndedCallRadio()
    {
        return mEndedCallRadio;
    }

    /**
     * Talking radio for the current call, or null.
     */
    public RadioIdentifier getCurrentRadio()
    {
        return getFromRadio() instanceof RadioIdentifier radio ? radio : null;
    }

    /**
     * Indicates if a call is in progress
     */
    public boolean isCallActive()
    {
        return mCallActive;
    }

    /**
     * Indicates if the identifiers are the same radio, comparing radio values so that a radio and its fully qualified
     * variant match.
     */
    public static boolean isSameRadio(Identifier a, Identifier b)
    {
        if(a instanceof RadioIdentifier ra && b instanceof RadioIdentifier rb)
        {
            return Objects.equals(ra.getValue(), rb.getValue());
        }

        return Objects.equals(a, b);
    }

    private Identifier getFromRadio()
    {
        return TalkerAliasManager.getFromRadio(mIdentifierCollection);
    }

    private Identifier getTalkerAlias()
    {
        return mIdentifierCollection.getIdentifier(IdentifierClass.USER, Form.TALKER_ALIAS, Role.FROM);
    }
}
