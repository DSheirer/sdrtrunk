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

package io.github.dsheirer.alias;

import io.github.dsheirer.alias.id.radio.P25FullyQualifiedRadio;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.radio.RadioRange;
import io.github.dsheirer.alias.id.talkgroup.P25FullyQualifiedTalkgroup;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupRange;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25FullyQualifiedTalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.protocol.Protocol;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class P25AliasTest
{
    @Test
    void aliasP25Talkgroup()
    {
        String correctAliasName = "Alias Talkgroup 1";
        AliasList aliasList = new AliasList("Test Alias List");

        Alias aliasTalkgroup1 = new Alias();
        aliasTalkgroup1.setName(correctAliasName);
        aliasTalkgroup1.addAliasID(new Talkgroup(Protocol.APCO25, 1));
        aliasList.addAlias(aliasTalkgroup1);

        Alias aliasTalkgroupRange = new Alias();
        aliasTalkgroupRange.setName("Alias Talkgroup Range");
        aliasTalkgroupRange.addAliasID(new TalkgroupRange(Protocol.APCO25, 1, 65535));
        aliasList.addAlias(aliasTalkgroupRange);

        TalkgroupIdentifier talkgroupIdentifier1 = APCO25Talkgroup.create(1);

        List<Alias> aliases = aliasList.getAliases(talkgroupIdentifier1);
        assertEquals(1, aliases.size(), "Expected 1 matching alias");
        assertEquals(correctAliasName, aliases.getFirst().getName(), "Unexpected alias name");
    }

    /**
     * Tests that a fully qualified P25 talkgroup aliases correctly when there is a matching fully qualified talkgroup
     * alias ID along with a simple talkgroup that has the same talkgroup value, that is shadowing the fully qualified
     * version.
     */
    @Test
    void aliasP25FullyQualifiedTalkgroup()
    {
        int wacn = 100;
        int system = 200;
        int originalGroup = 300;
        int aliasGroup = 1;
        String correctAliasName = "Alias Fully Qualified Talkgroup 1";

        AliasList aliasList = new AliasList("Test Alias List");

        Alias aliasTalkgroup1 = new Alias();
        aliasTalkgroup1.setName("Alias Talkgroup 1");
        aliasTalkgroup1.addAliasID(new Talkgroup(Protocol.APCO25, aliasGroup)); //Shadows the fully qualified variant
        aliasList.addAlias(aliasTalkgroup1);

        Alias aliasFullyQualifiedTalkgroup1 = new Alias();
        aliasFullyQualifiedTalkgroup1.setName(correctAliasName);
        aliasFullyQualifiedTalkgroup1.addAliasID(new P25FullyQualifiedTalkgroup(wacn, system, originalGroup));
        aliasList.addAlias(aliasFullyQualifiedTalkgroup1);

        //Identifier transmitted over the air that we want to alias
        APCO25FullyQualifiedTalkgroupIdentifier p25FQTG1 = APCO25FullyQualifiedTalkgroupIdentifier.createTo(aliasGroup, wacn, system, originalGroup);

        List<Alias> aliases = aliasList.getAliases(p25FQTG1);
        assertEquals(1, aliases.size(), "Expected 1 matching alias");
        assertEquals(correctAliasName, aliases.getFirst().getName(), "Unexpected alias name");
    }

    /**
     * Tests that a fully qualified P25 talkgroup aliases correctly to a simple talkgroup alias for the local talkgroup
     * value, when the user has not explicitly added a fully qualified talkgroup alias ID..
     */
    @Test
    void aliasP25FullyQualifiedTalkgroupToBasicTalkgroupAlias()
    {
        int wacn = 100;
        int system = 200;
        int originalGroup = 300;
        int aliasGroup = 1;
        String correctAliasName = "Alias Talkgroup 1";

        AliasList aliasList = new AliasList("Test Alias List");

        Alias aliasTalkgroup1 = new Alias();
        aliasTalkgroup1.setName("Alias Talkgroup 1");
        aliasTalkgroup1.addAliasID(new Talkgroup(Protocol.APCO25, aliasGroup));
        aliasList.addAlias(aliasTalkgroup1);

        //Identifier transmitted over the air that we want to alias
        APCO25FullyQualifiedTalkgroupIdentifier p25FQTG1 = APCO25FullyQualifiedTalkgroupIdentifier.createTo(aliasGroup, wacn, system, originalGroup);

        List<Alias> aliases = aliasList.getAliases(p25FQTG1);
        assertEquals(0, aliases.size(), "Expected 0 matching alias");
    }

    /**
     * Tests that a fully qualified P25 talkgroup aliases correctly to a simple talkgroup alias for the local talkgroup
     * value, when the user has not explicitly added a fully qualified talkgroup alias ID..
     */
    @Test
    void aliasP25FullyQualifiedTalkgroupToTalkgroupRangeAlias()
    {
        int wacn = 100;
        int system = 200;
        int originalGroup = 300;
        int aliasGroup = 1;
        String correctAliasName = "Alias Talkgroup 1";

        AliasList aliasList = new AliasList("Test Alias List");

        Alias aliasTalkgroupRange = new Alias();
        aliasTalkgroupRange.setName(correctAliasName);
        aliasTalkgroupRange.addAliasID(new TalkgroupRange(Protocol.APCO25, 1, 0xFFFF));
        aliasList.addAlias(aliasTalkgroupRange);

        //Identifier transmitted over the air that we want to alias
        APCO25FullyQualifiedTalkgroupIdentifier p25FQTG1 = APCO25FullyQualifiedTalkgroupIdentifier.createTo(aliasGroup, wacn, system, originalGroup);

        List<Alias> aliases = aliasList.getAliases(p25FQTG1);
        assertEquals(0, aliases.size(), "Expected 0 matching aliases");
    }

    @Test
    void aliasP25Radio()
    {
        String correctAliasName = "Alias Radio 1";
        AliasList aliasList = new AliasList("Test Alias List");

        Alias correctAlias = new Alias();
        correctAlias.setName(correctAliasName);
        correctAlias.addAliasID(new Radio(Protocol.APCO25, 1));
        aliasList.addAlias(correctAlias);

        Alias aliasRadioRange = new Alias();
        aliasRadioRange.setName("Alias Radio Range");
        aliasRadioRange.addAliasID(new RadioRange(Protocol.APCO25, 1, 0xFFFFFF));
        aliasList.addAlias(aliasRadioRange);

        RadioIdentifier radioIdentifier1 = APCO25RadioIdentifier.createFrom(1);

        List<Alias> aliases = aliasList.getAliases(radioIdentifier1);
        assertEquals(1, aliases.size(), "Expected 1 matching alias");
        assertEquals(correctAliasName, aliases.getFirst().getName(), "Unexpected alias name");
    }

    /**
     * Tests that a fully qualified P25 radio aliases correctly when there is a matching fully qualified radio
     * alias ID along with a simple radio that has the same radio value, that is shadowing the fully qualified
     * version.
     */
    @Test
    void aliasP25FullyQualifiedRadio()
    {
        int wacn = 100;
        int system = 200;
        int originalRadio = 300;
        int aliasRadio = 1;
        String correctAliasName = "Alias Fully Qualified Radio 1";

        AliasList aliasList = new AliasList("Test Alias List");

        Alias aliasRadio1 = new Alias();
        aliasRadio1.setName("Alias Radio 1");
        aliasRadio1.addAliasID(new Radio(Protocol.APCO25, aliasRadio)); //Shadows the fully qualified variant
        aliasList.addAlias(aliasRadio1);

        Alias aliasFullyQualifiedRadio1 = new Alias();
        aliasFullyQualifiedRadio1.setName(correctAliasName);
        aliasFullyQualifiedRadio1.addAliasID(new P25FullyQualifiedRadio(wacn, system, originalRadio));
        aliasList.addAlias(aliasFullyQualifiedRadio1);

        //Identifier transmitted over the air that we want to alias
        APCO25FullyQualifiedRadioIdentifier p25FQR1 = APCO25FullyQualifiedRadioIdentifier.createFrom(aliasRadio, wacn, system, originalRadio);

        List<Alias> aliases = aliasList.getAliases(p25FQR1);
        assertEquals(1, aliases.size(), "Expected 1 matching alias");
        assertEquals(correctAliasName, aliases.getFirst().getName(), "Unexpected alias name");
    }

    /**
     * Tests that a fully qualified P25 radio aliases correctly to a simple radio alias for the local radio
     * value, when the user has not explicitly added a fully qualified radio alias ID.
     */
    @Test
    void aliasP25FullyQualifiedRadioToBasicRadioAlias()
    {
        int wacn = 100;
        int system = 200;
        int originalRadio = 300;
        int aliasRadio = 1;
        String correctAliasName = "Alias Radio 1";

        AliasList aliasList = new AliasList("Test Alias List");

        Alias aliasRadio1 = new Alias();
        aliasRadio1.setName(correctAliasName);
        aliasRadio1.addAliasID(new Radio(Protocol.APCO25, aliasRadio));
        aliasList.addAlias(aliasRadio1);

        //Identifier transmitted over the air that we want to alias
        APCO25FullyQualifiedRadioIdentifier p25FQTG1 = APCO25FullyQualifiedRadioIdentifier.createFrom(aliasRadio, wacn, system, originalRadio);

        List<Alias> aliases = aliasList.getAliases(p25FQTG1);
        assertEquals(0, aliases.size(), "Expected 0 matching aliases");
    }

    /**
     * Tests that a fully qualified P25 radio aliases correctly to a radio range alias for the local radio
     * value, when the user has not explicitly added a fully qualified radio alias ID.
     */
    @Test
    void aliasP25FullyQualifiedRadioToRadioRangeAlias()
    {
        int wacn = 100;
        int system = 200;
        int originalRadio = 300;
        int aliasRadio = 1;
        String correctAliasName = "Alias Radio Range";

        AliasList aliasList = new AliasList("Test Alias List");

        Alias aliasTalkgroup1 = new Alias();
        aliasTalkgroup1.setName(correctAliasName);
        aliasTalkgroup1.addAliasID(new RadioRange(Protocol.APCO25, 1, 0xFFFFFF));
        aliasList.addAlias(aliasTalkgroup1);

        //Identifier transmitted over the air that we want to alias
        APCO25FullyQualifiedRadioIdentifier p25FQTG1 = APCO25FullyQualifiedRadioIdentifier.createFrom(aliasRadio, wacn, system, originalRadio);

        List<Alias> aliases = aliasList.getAliases(p25FQTG1);
        assertEquals(0, aliases.size(), "Expected 0 matching aliases");
    }
}
