package io.github.dsheirer.alias;

import java.awt.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AliasDisplayColorTest
{
    @Test
    void distinguishesUnsetColorFromExplicitBlack()
    {
        Alias alias = new Alias("Radio");
        assertFalse(alias.hasDisplayColor());

        alias.setColor(Color.BLACK.getRGB());
        assertTrue(alias.hasDisplayColor());
    }
}
