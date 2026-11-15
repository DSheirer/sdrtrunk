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
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests assigning talker aliases to the right radio and keeping the shown talker alias matched to the talking radio.
 */
public class TalkerAliasTrackerTest
{
    private static final RadioIdentifier RADIO_A = (RadioIdentifier)APCO25RadioIdentifier.createFrom(100);
    private static final RadioIdentifier RADIO_B = (RadioIdentifier)APCO25RadioIdentifier.createFrom(200);
    private static final TalkerAliasIdentifier ALIAS_A = P25TalkerAliasIdentifier.create("ENGINE 7");
    private static final TalkerAliasIdentifier ALIAS_B = P25TalkerAliasIdentifier.create("MEDIC 12");
    private MutableIdentifierCollection mIdentifiers;
    private TalkerAliasManager mManager;
    private TalkerAliasTracker mTracker;
    private long mTime = 1_000_000;

    @BeforeEach
    public void setup()
    {
        mIdentifiers = new MutableIdentifierCollection();
        mManager = new TalkerAliasManager();
        mTracker = new TalkerAliasTracker(mIdentifiers, () -> mManager, TalkerAliasTracker.BetweenCalls.PREVIOUS_TALKER);
    }

    /**
     * Call message from the radio: the decoder updates the identifiers and signals call activity
     */
    private void talk(RadioIdentifier radio)
    {
        mIdentifiers.update(radio);
        mTracker.callActivity(mTime);
        mTime += 100;
    }

    /**
     * Call ends: the decoder signals the end and removes the FROM identifiers
     */
    private void endCall()
    {
        mTracker.callEnded();
        mIdentifiers.remove(Role.FROM);
    }

    private Identifier getShownAlias()
    {
        return mIdentifiers.getIdentifier(IdentifierClass.USER, Form.TALKER_ALIAS, Role.FROM);
    }

    private Identifier getStoredAlias(RadioIdentifier radio)
    {
        MutableIdentifierCollection mic = new MutableIdentifierCollection();
        mic.update(radio);
        mManager.enrichMutable(mic);
        return mic.getIdentifier(IdentifierClass.USER, Form.TALKER_ALIAS, Role.FROM);
    }

    @Test
    public void aliasDuringCallIsShownAndStored()
    {
        talk(RADIO_A);
        assertEquals(TalkerAliasTracker.Assignment.CURRENT_CALL, mTracker.received(ALIAS_A, null, mTime));
        assertEquals(ALIAS_A, getShownAlias());
        assertEquals(ALIAS_A, getStoredAlias(RADIO_A));
        endCall();
        assertNull(getShownAlias());

        //The talker's next call shows the stored alias right away
        talk(RADIO_A);
        assertEquals(ALIAS_A, getShownAlias());
    }

    @Test
    public void aliasBeforeTheRadioIsIdentifiedIsStoredForTheRadio()
    {
        mTracker.callActivity(mTime); //Call starts without identifying the talker (e.g. a P25 Phase 1 HDU)
        assertEquals(TalkerAliasTracker.Assignment.CURRENT_CALL, mTracker.received(ALIAS_A, null, mTime));
        assertEquals(ALIAS_A, getShownAlias());

        talk(RADIO_A);
        assertEquals(ALIAS_A, getShownAlias(), "Alias is kept when the talker is identified");
        assertEquals(ALIAS_A, getStoredAlias(RADIO_A), "and stored for the talker");
    }

    @Test
    public void aliasBeforeTheRadioInANewCallIsntAssignedToThePreviousTalker()
    {
        talk(RADIO_A);
        endCall();
        mTracker.callActivity(mTime); //Next call starts without identifying the talker
        mTracker.received(ALIAS_B, null, mTime);
        assertNull(getStoredAlias(RADIO_A), "Not stored for the previous call's talker");

        talk(RADIO_B);
        assertEquals(ALIAS_B, getStoredAlias(RADIO_B));
    }

    @Test
    public void aliasAfterTheCallEndsBelongsToThatCallsTalker()
    {
        talk(RADIO_A);
        endCall();
        assertEquals(TalkerAliasTracker.Assignment.ENDED_CALL, mTracker.received(ALIAS_A, null, mTime));
        assertEquals(RADIO_A, mTracker.getEndedCallRadio());
        assertNull(getShownAlias(), "Not shown with no call in progress");
        assertEquals(ALIAS_A, getStoredAlias(RADIO_A));

        talk(RADIO_B);
        assertNull(getShownAlias(), "Not shown for a different talker");
        endCall();

        talk(RADIO_A);
        assertEquals(ALIAS_A, getShownAlias());
    }

    @Test
    public void callEndedMoreThanOnceKeepsTheTalker()
    {
        talk(RADIO_A);
        endCall();
        endCall(); //e.g. repeated TDULC or End PTT messages
        assertEquals(TalkerAliasTracker.Assignment.ENDED_CALL, mTracker.received(ALIAS_A, null, mTime));
        assertEquals(ALIAS_A, getStoredAlias(RADIO_A));
    }

    @Test
    public void aliasIsRemovedWhenADifferentRadioTalks()
    {
        talk(RADIO_A);
        mTracker.received(ALIAS_A, null, mTime);
        talk(RADIO_B); //Talker changes without a call end
        assertNull(getShownAlias());

        mManager.update(RADIO_B, ALIAS_B);
        talk(RADIO_B);
        assertEquals(ALIAS_B, getShownAlias());
    }

    @Test
    public void aliasIsKeptWhenTheRadioBecomesFullyQualified()
    {
        talk(RADIO_A);
        mTracker.received(ALIAS_A, null, mTime);
        mIdentifiers.update(APCO25FullyQualifiedRadioIdentifier.createFrom(100, 1, 2, 100));
        mTracker.callActivity(mTime);
        assertEquals(ALIAS_A, getShownAlias());
    }

    @Test
    public void aliasWithItsOwnRadio()
    {
        //Motorola talker aliases identify their radio
        talk(RADIO_A);
        assertEquals(TalkerAliasTracker.Assignment.UNASSIGNED, mTracker.received(ALIAS_B, RADIO_B, mTime),
            "Alias for a radio that isn't talking");
        assertNull(getShownAlias());
        assertEquals(ALIAS_B, getStoredAlias(RADIO_B));

        assertEquals(TalkerAliasTracker.Assignment.CURRENT_CALL, mTracker.received(ALIAS_A, RADIO_A, mTime));
        assertEquals(ALIAS_A, getShownAlias());
        endCall();

        assertEquals(TalkerAliasTracker.Assignment.ENDED_CALL, mTracker.received(ALIAS_A, RADIO_A, mTime));
        assertEquals(TalkerAliasTracker.Assignment.UNASSIGNED, mTracker.received(ALIAS_B, RADIO_B, mTime));
    }

    @Test
    public void aliasWithNoCallOrTalkerIsIgnored()
    {
        assertEquals(TalkerAliasTracker.Assignment.UNASSIGNED, mTracker.received(ALIAS_A, null, mTime));
        assertNull(getShownAlias());
    }

    @Test
    public void worksWithoutATalkerAliasManager()
    {
        TalkerAliasTracker tracker = new TalkerAliasTracker(mIdentifiers, () -> null,
            TalkerAliasTracker.BetweenCalls.PREVIOUS_TALKER);
        mIdentifiers.update(RADIO_A);
        tracker.callActivity(mTime);
        assertEquals(TalkerAliasTracker.Assignment.CURRENT_CALL, tracker.received(ALIAS_A, null, mTime));
        assertEquals(ALIAS_A, getShownAlias());
        mIdentifiers.update(RADIO_B);
        tracker.callActivity(mTime);
        assertNull(getShownAlias());
    }

    @Test
    public void nextTalkerAliasBelongsToTheNextCall()
    {
        //P25 Phase 2 L3Harris: the alias arrives just before the next call's Push-To-Talk
        mTracker = new TalkerAliasTracker(mIdentifiers, () -> mManager, TalkerAliasTracker.BetweenCalls.NEXT_TALKER);
        talk(RADIO_A);
        endCall();

        assertEquals(TalkerAliasTracker.Assignment.NEXT_CALL, mTracker.received(ALIAS_B, null, mTime));
        assertNull(getStoredAlias(RADIO_A), "Not stored for the previous call's talker");
        assertNull(getShownAlias(), "Not shown until the next call starts");

        talk(RADIO_B);
        assertEquals(ALIAS_B, getShownAlias());
        assertEquals(ALIAS_B, getStoredAlias(RADIO_B));
    }

    @Test
    public void nextTalkerAliasBeforeACallWithoutARadio()
    {
        mTracker = new TalkerAliasTracker(mIdentifiers, () -> mManager, TalkerAliasTracker.BetweenCalls.NEXT_TALKER);
        mTracker.received(ALIAS_B, null, mTime);
        mTracker.callActivity(mTime + 50); //Voice before the talker is identified
        assertEquals(ALIAS_B, getShownAlias());

        talk(RADIO_B);
        assertEquals(ALIAS_B, getShownAlias());
        assertEquals(ALIAS_B, getStoredAlias(RADIO_B));
    }

    @Test
    public void nextTalkerAliasExpires()
    {
        mTracker = new TalkerAliasTracker(mIdentifiers, () -> mManager, TalkerAliasTracker.BetweenCalls.NEXT_TALKER);
        mTracker.received(ALIAS_B, null, mTime);
        mTime += TalkerAliasTracker.NEXT_CALL_WINDOW_MS + 1;
        talk(RADIO_A);
        assertNull(getShownAlias(), "Too long before the call to belong to it");
        assertNull(getStoredAlias(RADIO_A));
    }
}
