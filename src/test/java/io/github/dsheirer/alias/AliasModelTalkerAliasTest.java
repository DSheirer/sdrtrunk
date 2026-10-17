package io.github.dsheirer.alias;

import io.github.dsheirer.alias.id.radio.P25FullyQualifiedRadio;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class AliasModelTalkerAliasTest
{
    @Test
    void createsAndUpdatesExactRadioAlias()
    {
        AliasModel model = new AliasModel();
        Alias created = model.storeTalkerAlias("County", DMRRadio.createFrom(1201), "Engine 1");

        assertEquals("Engine 1", created.getName());
        Radio radio = assertInstanceOf(Radio.class, created.getAliasIdentifiers().getFirst());
        assertEquals(1201, radio.getValue());
        assertEquals(1, model.getAliases().size());

        Alias updated = model.storeTalkerAlias("County", DMRRadio.createFrom(1201), "Engine One");
        assertSame(created, updated);
        assertEquals("Engine One", created.getName());
        assertEquals(1, model.getAliases().size());
    }

    @Test
    void preservesFullyQualifiedP25Identity()
    {
        AliasModel model = new AliasModel();
        APCO25FullyQualifiedRadioIdentifier radio = APCO25FullyQualifiedRadioIdentifier.createFrom(
            1234, 0xABCDE, 0x123, 567890);

        Alias alias = model.storeTalkerAlias("Statewide", radio, "Portable 12");
        P25FullyQualifiedRadio stored = assertInstanceOf(P25FullyQualifiedRadio.class,
            alias.getAliasIdentifiers().getFirst());
        assertEquals(0xABCDE, stored.getWacn());
        assertEquals(0x123, stored.getSystem());
        assertEquals(567890, stored.getValue());
    }
}
