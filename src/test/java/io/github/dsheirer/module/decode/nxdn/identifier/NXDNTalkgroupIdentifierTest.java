/*
 * *****************************************************************************
 * Copyright (C) 2026 Dennis Sheirer
 * ****************************************************************************
 */
package io.github.dsheirer.module.decode.nxdn.identifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NXDNTalkgroupIdentifierTest
{
    @Test
    void acceptsNullGroupButRejectsNegativeValues()
    {
        assertTrue(NXDNTalkgroupIdentifier.createTo(0).isValid());
        assertTrue(NXDNTalkgroupIdentifier.createTo(1).isValid());
        assertFalse(NXDNTalkgroupIdentifier.createTo(-1).isValid());
    }
}
