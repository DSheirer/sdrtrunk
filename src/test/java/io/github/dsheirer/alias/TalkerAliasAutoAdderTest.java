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

import io.github.dsheirer.alias.TalkerAliasAutoAdder.Decision;
import io.github.dsheirer.alias.id.radio.P25FullyQualifiedRadio;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.identifier.alias.P25TalkerAliasIdentifier;
import io.github.dsheirer.identifier.alias.TalkerAliasManager;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.protocol.Protocol;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for automatically adding talker aliases to an alias list.  The JavaFX toolkit isn't running, so the alias model
 * updates run on the calling thread.
 */
public class TalkerAliasAutoAdderTest
{
    private static final String LIST = "Test List";
    private static final RadioIdentifier RADIO = (RadioIdentifier)APCO25RadioIdentifier.createFrom(1234);

    private AliasModel mAliasModel;
    private TalkerAliasManager mManager;

    @BeforeEach
    public void setup()
    {
        mAliasModel = new AliasModel();
        mManager = new TalkerAliasManager();
        mManager.setTalkerAliasListener(new TalkerAliasAutoAdder(mAliasModel, LIST));
    }

    private void decode(RadioIdentifier radio, String alias)
    {
        mManager.update(radio, P25TalkerAliasIdentifier.create(alias));
    }

    private List<Alias> aliases()
    {
        return mAliasModel.getAliases().stream().filter(a -> LIST.equals(a.getAliasListName())).toList();
    }

    private Alias alias(RadioIdentifier radio)
    {
        return TalkerAliasAutoAdder.findExactMatch(aliases(), TalkerAliasAutoAdder.createAliasId(radio));
    }

    @Test
    public void createsAliasForRadio()
    {
        decode(RADIO, "ENGINE 12");
        assertEquals(1, aliases().size());
        Alias alias = alias(RADIO);
        assertEquals("ENGINE 12", alias.getName());
        assertEquals(TalkerAliasAutoAdder.ALIAS_GROUP, alias.getGroup());
        assertTrue(alias.getAliasIdentifiers().get(0) instanceof Radio radio && radio.getValue() == 1234 &&
            radio.getProtocol() == Protocol.APCO25);

        decode(RADIO, "ENGINE 12");
        assertEquals(1, aliases().size(), "no duplicate alias");
    }

    @Test
    public void fragmentNeverReplacesFullAlias()
    {
        decode(RADIO, "ENGINE 12 OFFICER");

        for(int x = 0; x < 5; x++)
        {
            decode(RADIO, "ENGINE 12 OFF");
            decode(RADIO, "ENG");
        }

        assertEquals("ENGINE 12 OFFICER", alias(RADIO).getName());
    }

    @Test
    public void fullAliasReplacesStoredFragment()
    {
        decode(RADIO, "ENGINE 1");
        assertEquals("ENGINE 1", alias(RADIO).getName());
        decode(RADIO, "ENGINE 12 OFFICER");
        assertEquals("ENGINE 12 OFFICER", alias(RADIO).getName());
    }

    @Test
    public void differentAliasRequiresConfirmation()
    {
        decode(RADIO, "ENGINE 12");
        decode(RADIO, "LADDER 3");
        assertEquals("ENGINE 12", alias(RADIO).getName(), "a single decode doesn't rename");
        decode(RADIO, "LADDER 3");
        assertEquals("LADDER 3", alias(RADIO).getName(), "confirmed decode renames");
    }

    @Test
    public void fragmentsOfNewAliasDontInterruptConfirmation()
    {
        decode(RADIO, "ENGINE 12");
        decode(RADIO, "LADDER 3 CAPTAIN");
        decode(RADIO, "LADDER");
        decode(RADIO, "LADDER 3 CAPTAIN");
        assertEquals("LADDER 3 CAPTAIN", alias(RADIO).getName());
    }

    @Test
    public void interleavedDifferentAliasesDontConfirm()
    {
        decode(RADIO, "ENGINE 12");
        decode(RADIO, "LADDER 3");
        decode(RADIO, "RESCUE 5");
        decode(RADIO, "LADDER 3");
        assertEquals("ENGINE 12", alias(RADIO).getName());
    }

    @Test
    public void multiIdentifierAliasIsLeftAlone()
    {
        Alias alias = new Alias("Portables");
        alias.setAliasListName(LIST);
        alias.setGroup(TalkerAliasAutoAdder.ALIAS_GROUP);
        alias.addAliasID(new Radio(Protocol.APCO25, 1234));
        alias.addAliasID(new Radio(Protocol.APCO25, 5678));
        mAliasModel.addAlias(alias);

        decode(RADIO, "ENGINE 12");
        decode(RADIO, "ENGINE 12");
        assertEquals(1, aliases().size());
        assertEquals("Portables", alias.getName());
    }

    @Test
    public void userAliasOutsideManagedGroupIsPreserved()
    {
        Alias alias = new Alias("Custom Name");
        alias.setAliasListName(LIST);
        alias.setGroup("Apparatus");
        alias.addAliasID(new Radio(Protocol.APCO25, 1234));
        io.github.dsheirer.alias.id.priority.Priority priority = new io.github.dsheirer.alias.id.priority.Priority(1);
        alias.addAliasID(priority);
        mAliasModel.addAlias(alias);

        decode(RADIO, "ENGINE 12");
        decode(RADIO, "ENGINE 12");
        assertEquals(1, aliases().size());
        assertEquals(alias, alias(RADIO));
        assertEquals("Custom Name", alias.getName());
        assertEquals("Apparatus", alias.getGroup());
        assertTrue(alias.getAliasIdentifiers().contains(priority));
    }

    @Test
    public void ungroupedUserAliasIsPreserved()
    {
        Alias alias = new Alias("Custom Name");
        alias.setAliasListName(LIST);
        alias.addAliasID(new Radio(Protocol.APCO25, 1234));
        mAliasModel.addAlias(alias);

        decode(RADIO, "ENGINE 12");
        decode(RADIO, "ENGINE 12");
        assertEquals(1, aliases().size());
        assertEquals("Custom Name", alias.getName());
        assertNull(alias.getGroup());
    }

    @Test
    public void managedAliasForRadioIsRenamedAndKeepsAudioSettings()
    {
        Alias alias = new Alias("Old Name");
        alias.setAliasListName(LIST);
        alias.setGroup(TalkerAliasAutoAdder.ALIAS_GROUP);
        alias.addAliasID(new Radio(Protocol.APCO25, 1234));
        io.github.dsheirer.alias.id.priority.Priority priority = new io.github.dsheirer.alias.id.priority.Priority(1);
        alias.addAliasID(priority);
        mAliasModel.addAlias(alias);

        decode(RADIO, "ENGINE 12");
        assertEquals("Old Name", alias.getName());
        decode(RADIO, "ENGINE 12");
        assertEquals(1, aliases().size());
        assertEquals("ENGINE 12", alias.getName());
        assertEquals(TalkerAliasAutoAdder.ALIAS_GROUP, alias.getGroup());
        assertTrue(alias.getAliasIdentifiers().contains(priority));
    }

    @Test
    public void movingAliasOutOfManagedGroupProtectsCustomName()
    {
        decode(RADIO, "ENGINE 12");
        Alias alias = alias(RADIO);
        alias.setGroup("Apparatus");
        alias.setName("Custom Name");

        decode(RADIO, "LADDER 3");
        decode(RADIO, "LADDER 3");
        assertEquals(1, aliases().size());
        assertEquals("Custom Name", alias.getName());
        assertEquals("Apparatus", alias.getGroup());

        //Moving it back opts it into automatic maintenance again.
        alias.setGroup(TalkerAliasAutoAdder.ALIAS_GROUP);
        decode(RADIO, "RESCUE 5");
        decode(RADIO, "RESCUE 5");
        assertEquals("RESCUE 5", alias.getName());
    }

    @Test
    public void talkgroupAliasWithSameValueIsIgnored()
    {
        Alias alias = new Alias("Dispatch");
        alias.setAliasListName(LIST);
        alias.addAliasID(new Talkgroup(Protocol.APCO25, 1234));
        mAliasModel.addAlias(alias);

        decode(RADIO, "ENGINE 12");
        assertEquals(2, aliases().size());
        assertEquals("Dispatch", alias.getName());
    }

    @Test
    public void fullyQualifiedRadio()
    {
        RadioIdentifier roaming = APCO25FullyQualifiedRadioIdentifier.createFrom(1234, 0xBEE00, 0x123, 777);
        decode(roaming, "ROAMER");
        Alias alias = alias(roaming);
        assertTrue(alias.getAliasIdentifiers().get(0) instanceof P25FullyQualifiedRadio);
        assertNull(alias(RADIO), "local radio with the same value isn't aliased");
    }

    /**
     * Fully qualified form of RADIO, with a local address that matches the radio ID.
     */
    private static final RadioIdentifier QUALIFIED = APCO25FullyQualifiedRadioIdentifier.createFrom(1234, 0xBEE00,
        0x123, 1234);

    private void assertSingleAliasForBothForms(String name)
    {
        assertEquals(1, aliases().size(), "one alias for both radio identifier forms");
        Alias alias = aliases().get(0);
        assertEquals(name, alias.getName());
        assertEquals(2, alias.getAliasIdentifiers().size());
        AliasList aliasList = mAliasModel.getAliasList(LIST);
        assertEquals(List.of(alias), aliasList.getAliases(RADIO), "alias list finds the simple radio");
        assertEquals(List.of(alias), aliasList.getAliases(QUALIFIED), "alias list finds the fully qualified radio");
    }

    @Test
    public void fullyQualifiedRadioAddedToSimpleRadioAlias()
    {
        decode(RADIO, "ENGINE 12");
        decode(QUALIFIED, "ENGINE 12");
        assertSingleAliasForBothForms("ENGINE 12");
        decode(QUALIFIED, "ENGINE 12");
        decode(RADIO, "ENGINE 12");
        assertSingleAliasForBothForms("ENGINE 12");
    }

    @Test
    public void simpleRadioAddedToFullyQualifiedRadioAlias()
    {
        decode(QUALIFIED, "ENGINE 12");
        decode(RADIO, "ENGINE 12");
        assertSingleAliasForBothForms("ENGINE 12");
    }

    @Test
    public void mergedAliasIsStillRenamed()
    {
        decode(RADIO, "ENGINE 1");
        decode(QUALIFIED, "ENGINE 12 OFFICER");
        assertSingleAliasForBothForms("ENGINE 12 OFFICER");
        decode(RADIO, "LADDER 3");
        decode(RADIO, "LADDER 3");
        assertSingleAliasForBothForms("LADDER 3");
        decode(QUALIFIED, "RESCUE 5");
        decode(QUALIFIED, "RESCUE 5");
        assertSingleAliasForBothForms("RESCUE 5");
    }

    @Test
    public void roamingRadioIsNotAddedToHomeRadioIdAlias()
    {
        RadioIdentifier home = APCO25RadioIdentifier.createFrom(777);
        decode(home, "LOCAL 777");
        decode(APCO25FullyQualifiedRadioIdentifier.createFrom(1234, 0xBEE00, 0x123, 777), "ROAMER");
        assertEquals(2, aliases().size());
        assertEquals("LOCAL 777", alias(home).getName());
        assertEquals(1, alias(home).getAliasIdentifiers().size());
    }

    @Test
    public void simpleRadioIsNotAddedToAmbiguousFullyQualifiedAliases()
    {
        decode(QUALIFIED, "ENGINE 12");
        decode(APCO25FullyQualifiedRadioIdentifier.createFrom(1234, 0xBEE01, 0x456, 1234), "OTHER 12");
        decode(RADIO, "ENGINE 12");
        assertEquals(3, aliases().size());
        assertEquals(1, alias(RADIO).getAliasIdentifiers().size());
    }

    private Alias userAlias(Radio radio)
    {
        Alias alias = new Alias("Custom Name");
        alias.setAliasListName(LIST);
        alias.setGroup("Apparatus");
        alias.addAliasID(radio);
        mAliasModel.addAlias(alias);
        return alias;
    }

    @Test
    public void userAliasIsGivenFullyQualifiedRadio()
    {
        Alias alias = userAlias(new Radio(Protocol.APCO25, 1234));
        decode(QUALIFIED, "ENGINE 12");
        decode(QUALIFIED, "ENGINE 12");
        assertSingleAliasForBothForms("Custom Name");
        assertEquals("Apparatus", alias.getGroup());
    }

    @Test
    public void userAliasIsGivenSimpleRadio()
    {
        Alias alias = userAlias(new P25FullyQualifiedRadio(0xBEE00, 0x123, 1234));
        decode(RADIO, "ENGINE 12");
        decode(RADIO, "ENGINE 12");
        assertSingleAliasForBothForms("Custom Name");
        assertEquals("Apparatus", alias.getGroup());
    }

    @Test
    public void multiIdentifierUserAliasIsNotGivenFullyQualifiedRadio()
    {
        Alias alias = userAlias(new Radio(Protocol.APCO25, 1234));
        alias.addAliasID(new Talkgroup(Protocol.APCO25, 100));
        mAliasModel.getAliasList(LIST).updateAlias(alias);
        decode(QUALIFIED, "ENGINE 12");
        assertEquals(2, aliases().size());
        assertEquals(2, alias.getAliasIdentifiers().size());
    }

    @Test
    public void decide()
    {
        assertEquals(Decision.UPDATE, TalkerAliasAutoAdder.decide(null, "A", 1));
        assertEquals(Decision.UNCHANGED, TalkerAliasAutoAdder.decide("ABC", "ABC", 1));
        assertEquals(Decision.FRAGMENT, TalkerAliasAutoAdder.decide("ABCDEF", "ABC", 5));
        assertEquals(Decision.UPDATE, TalkerAliasAutoAdder.decide("ABC", "ABCDEF", 1));
        assertEquals(Decision.AWAIT_CONFIRMATION, TalkerAliasAutoAdder.decide("ABC", "XYZ", 1));
        assertEquals(Decision.UPDATE, TalkerAliasAutoAdder.decide("ABC", "XYZ", 2));
        assertEquals(Decision.AWAIT_CONFIRMATION, TalkerAliasAutoAdder.decide("ABC", "X�Z", 5));
    }

    @Test
    public void normalize()
    {
        assertEquals("JOS", TalkerAliasAutoAdder.normalize("JOS�"));
        assertEquals("ENGINE 12", TalkerAliasAutoAdder.normalize(" ENGINE 12\u0000\u0000  "));
        assertNull(TalkerAliasAutoAdder.normalize("\u0000 "));
    }
}
