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
package io.github.dsheirer.database.call;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Call type derived from the stored TO and FROM identifier forms
 */
public class CallTypeTest
{
    @Test
    public void callTypes()
    {
        assertEquals(CallType.GROUP, CallType.from("TALKGROUP", "RADIO"));
        assertEquals(CallType.PATCH, CallType.from("PATCH_GROUP", "RADIO"));
        assertEquals(CallType.PRIVATE, CallType.from("RADIO", "RADIO"));
        assertEquals(CallType.TELEPHONE, CallType.from("TELEPHONE_NUMBER", "RADIO"), "radio to telephone");
        assertEquals(CallType.TELEPHONE, CallType.from("RADIO", "TELEPHONE_NUMBER"), "telephone to radio");
        assertEquals(CallType.UNKNOWN, CallType.from(null, null), "no TO identifier");

        CallRecord call = new CallRecord();
        call.setToForm(CallRecord.FORM_RADIO);
        call.setFromForm(CallRecord.FORM_RADIO);
        assertEquals("Private", call.getCallType().toString());
    }
}
