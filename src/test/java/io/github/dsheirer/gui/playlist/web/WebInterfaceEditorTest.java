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

package io.github.dsheirer.gui.playlist.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WebInterfaceEditorTest
{
    @Test
    void formatsListenerAddressOnce()
    {
        assertEquals("http://0.0.0.0:10000/", WebInterfaceEditor.formatActiveAddress("0.0.0.0", 10000));
        assertEquals("http://0.0.0.0:10000/",
            WebInterfaceEditor.formatActiveAddress("0.0.0.0:0.0.0.0", 10000));
        assertEquals("http://0.0.0.0:10000/",
            WebInterfaceEditor.formatActiveAddress("http://0.0.0.0:10000/", 10000));
    }
}
