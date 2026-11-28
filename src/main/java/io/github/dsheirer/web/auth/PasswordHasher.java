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

package io.github.dsheirer.web.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing with PBKDF2-HMAC-SHA256 from the JDK, so that neither sdrtrunk nor the watchdog needs a crypto
 * library.
 *
 * Encoded form: {@code pbkdf2-sha256$<iterations>$<base64 salt>$<base64 hash>}.  The algorithm and iteration count are
 * part of the encoded hash, so the iteration count can be raised, or another algorithm added, without invalidating
 * stored hashes; {@link #needsRehash(String)} reports hashes made with older parameters so they can be replaced at the
 * next successful sign-in.
 */
public final class PasswordHasher
{
    public static final String ALGORITHM_ID = "pbkdf2-sha256";
    /**
     * OWASP password storage recommendation for PBKDF2-HMAC-SHA256
     */
    public static final int DEFAULT_ITERATIONS = 600_000;
    public static final int MIN_PASSWORD_LENGTH = 10;
    public static final int MAX_PASSWORD_LENGTH = 256;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TEMPORARY_ALPHABET = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final int mIterations;
    private final String mDummyHash;

    /**
     * Constructs an instance with the default iteration count
     */
    public PasswordHasher()
    {
        this(DEFAULT_ITERATIONS);
    }

    /**
     * Constructs an instance
     * @param iterations PBKDF2 iteration count for new hashes (tests use a small count)
     */
    public PasswordHasher(int iterations)
    {
        mIterations = iterations;
        mDummyHash = hash("not a real password " + RANDOM.nextLong());
    }

    /**
     * Hashes the password with a new random salt.
     * @return encoded hash
     */
    public String hash(String password)
    {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, mIterations);
        Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
        return ALGORITHM_ID + "$" + mIterations + "$" + encoder.encodeToString(salt) + "$" + encoder.encodeToString(hash);
    }

    /**
     * Checks the password against an encoded hash, in time that doesn't depend on where the hashes differ.
     * @param password to check
     * @param encoded hash from {@link #hash(String)}, or null to spend the same time as a real check and return
     * false, so that a sign-in for an unknown account takes as long as one for a known account
     * @return true if the password matches
     */
    public boolean verify(String password, String encoded)
    {
        if(encoded == null)
        {
            verify(password, mDummyHash);
            return false;
        }

        String[] parts = encoded.split("\\$");

        if(parts.length != 4 || !ALGORITHM_ID.equals(parts[0]))
        {
            return false;
        }

        try
        {
            int iterations = Integer.parseInt(parts[1]);
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] salt = decoder.decode(parts[2]);
            byte[] expected = decoder.decode(parts[3]);
            byte[] actual = pbkdf2(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        }
        catch(IllegalArgumentException e)
        {
            return false;
        }
    }

    /**
     * Indicates if the encoded hash uses other parameters than new hashes, so it should be replaced.
     */
    public boolean needsRehash(String encoded)
    {
        return encoded == null || !encoded.startsWith(ALGORITHM_ID + "$" + mIterations + "$");
    }

    /**
     * Checks a new password against the password rules.
     * @return null if the password is acceptable, or the reason it isn't
     */
    public static String checkPasswordRules(String password)
    {
        if(password == null || password.length() < MIN_PASSWORD_LENGTH)
        {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters";
        }

        if(password.length() > MAX_PASSWORD_LENGTH)
        {
            return "Password must be at most " + MAX_PASSWORD_LENGTH + " characters";
        }

        if(password.isBlank())
        {
            return "Password can't be only spaces";
        }

        return null;
    }

    /**
     * Creates a random temporary password that is easy to read and type: no 0/O, 1/l/I.
     */
    public static String createTemporaryPassword()
    {
        StringBuilder sb = new StringBuilder();

        for(int x = 0; x < 16; x++)
        {
            if(x > 0 && x % 4 == 0)
            {
                sb.append('-');
            }

            sb.append(TEMPORARY_ALPHABET.charAt(RANDOM.nextInt(TEMPORARY_ALPHABET.length())));
        }

        return sb.toString();
    }

    /**
     * Creates a random, URL safe secret with the number of random bytes.
     */
    public static String createSecret(int bytes)
    {
        byte[] secret = new byte[bytes];
        RANDOM.nextBytes(secret);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    }

    /**
     * Hex SHA-256 of the text, for high-entropy secrets (API tokens) that don't need a slow hash.
     */
    public static String sha256(String text)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        }
        catch(NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-256 isn't available", e);
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations)
    {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);

        try
        {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        }
        catch(NoSuchAlgorithmException | InvalidKeySpecException e)
        {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 isn't available", e);
        }
        finally
        {
            spec.clearPassword();
        }
    }
}
