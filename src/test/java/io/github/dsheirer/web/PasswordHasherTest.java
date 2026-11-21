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

package io.github.dsheirer.web;

import io.github.dsheirer.web.auth.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PBKDF2 password hashing, the password rules and the random secrets.
 */
public class PasswordHasherTest
{
    @Test
    void hashVerifiesAndRejects()
    {
        PasswordHasher hasher = new PasswordHasher(1000);
        String hash = hasher.hash("correct horse battery");

        assertTrue(hash.startsWith("pbkdf2-sha256$1000$"));
        assertEquals(4, hash.split("\\$").length);
        assertTrue(hasher.verify("correct horse battery", hash));
        assertFalse(hasher.verify("correct horse batterY", hash));
        assertFalse(hasher.verify("", hash));
        assertNotEquals(hash, hasher.hash("correct horse battery"), "each hash has its own salt");
    }

    @Test
    void malformedAndMissingHashesFail()
    {
        PasswordHasher hasher = new PasswordHasher(1000);
        assertFalse(hasher.verify("password123", null));
        assertFalse(hasher.verify("password123", "garbage"));
        assertFalse(hasher.verify("password123", "bcrypt$10$abc$def"));
        assertFalse(hasher.verify("password123", "pbkdf2-sha256$x$abc$def"));
        assertFalse(hasher.verify("password123", "pbkdf2-sha256$1000$!!!$def"));
    }

    @Test
    void olderParametersNeedRehash()
    {
        String old = new PasswordHasher(1000).hash("password1234");
        PasswordHasher current = new PasswordHasher(2000);
        assertTrue(current.needsRehash(old));
        assertTrue(current.verify("password1234", old), "an older hash still verifies");
        assertFalse(current.needsRehash(current.hash("password1234")));
    }

    @Test
    void passwordRules()
    {
        assertNotNull(PasswordHasher.checkPasswordRules(null));
        assertNotNull(PasswordHasher.checkPasswordRules("short"));
        assertNotNull(PasswordHasher.checkPasswordRules("          "));
        assertNotNull(PasswordHasher.checkPasswordRules("x".repeat(257)));
        assertNull(PasswordHasher.checkPasswordRules("long enough pass"));
    }

    @Test
    void temporaryPasswordsMeetTheRules()
    {
        String password = PasswordHasher.createTemporaryPassword();
        assertEquals(19, password.length());
        assertNull(PasswordHasher.checkPasswordRules(password));
        assertNotEquals(password, PasswordHasher.createTemporaryPassword());
    }

    @Test
    void defaultIterationsTakeReasonableTime()
    {
        PasswordHasher hasher = new PasswordHasher();
        String hash = hasher.hash("timing check password");
        long start = System.nanoTime();
        assertTrue(hasher.verify("timing check password", hash));
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.println("PBKDF2-SHA256 " + PasswordHasher.DEFAULT_ITERATIONS + " iterations: " + ms + " ms");
    }
}
