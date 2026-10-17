/*
 * *****************************************************************************
 * Copyright (C) 2026 Dennis Sheirer
 * ****************************************************************************
 */
package io.github.dsheirer.channel.state;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StateTest
{
    @Test
    void encryptedStateCanRecoverToClearCallAndData()
    {
        assertTrue(State.ENCRYPTED.canChangeTo(State.ACTIVE));
        assertTrue(State.ENCRYPTED.canChangeTo(State.CALL));
        assertTrue(State.ENCRYPTED.canChangeTo(State.DATA));
    }
}
