/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.ip.mototrbo.ars;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.ip.mototrbo.ars.identifier.ARSDevice;
import io.github.dsheirer.module.decode.ip.mototrbo.ars.identifier.ARSPassword;
import io.github.dsheirer.module.decode.ip.mototrbo.ars.identifier.ARSUser;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Automatic Registration Service - Device Registration
 */
public class DeviceRegistration extends ARSHeader
{
    private final static Logger mLog = LoggerFactory.getLogger(DeviceRegistration.class);

    private static final int SECOND_HEADER_EXTENSION_FLAG = 24;
    private static final int[] EVENT = {25, 26};
    private static final int[] ENCODING = {27, 28, 29, 30, 31};
    private static final int DEVICE_IDENTIFIER_START = 24;
    private static final int DEVICE_IDENTIFIER_START_EXTENDED_HEADER = 32;

    private static final int[] BYTE_VALUE = {0, 1, 2, 3, 4, 5, 6, 7};

    private ARSDevice mDevice;
    private ARSUser mUser;
    private ARSPassword mPassword;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param message containing the header
     * @param offset to the header within the message
     */
    public DeviceRegistration(BinaryMessage message, int offset)
    {
        super(message, offset);
        parsePayload();
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("DEVICE REGISTRATION");

        if(isValid())
        {
            if(hasHeaderExtension())
            {
                sb.append(isInitialEvent() ? "-INITIAL" : "-REFRESH");
            }

            if(hasDevice())
            {
                sb.append(" DEVICE:").append(getDevice());
            }

            if(hasUser())
            {
                sb.append(" USER:").append(getUser());
            }

            if(hasPassword())
            {
                sb.append(" PW:").append(getPassword());
            }
        }
        else
        {
            sb.append(" - ERROR INVALID MESSAGE LENGTH");
        }
        return sb.toString();
    }

    /**
     * Indicates if this is an initial registration event (true) or refresh event (false)
     */
    public boolean isInitialEvent()
    {
        return getMessage().getInt(EVENT, getOffset()) == 1;
    }

    /**
     * ARS User Id
     * @return user or null
     */
    public ARSUser getUser()
    {
        return mUser;
    }

    /**
     * Indicates if this packet has a user Id
     */
    public boolean hasUser()
    {
        return mUser != null;
    }

    /**
     * ARS Password
     * @return password or null
     */
    public ARSPassword getPassword()
    {
        return mPassword;
    }

    /**
     * Indicates if this packet has a password
     */
    public boolean hasPassword()
    {
        return mPassword != null;
    }

    /**
     * ARS Device
      * @return device or null
     */
    public ARSDevice getDevice()
    {
        return mDevice;
    }

    /**
     * Indicates if this packet has a device Id
     */
    public boolean hasDevice()
    {
        return mDevice != null;
    }

    /**
     * Device, user and password values contained in the registration packet.
     */
    private void parsePayload()
    {
        int pointer = getOffset();

        if(hasHeaderExtension())
        {
            pointer += DEVICE_IDENTIFIER_START_EXTENDED_HEADER;
        }
        else
        {
            pointer += DEVICE_IDENTIFIER_START;
        }

        int deviceIdentifierSize = getMessage().getInt(BYTE_VALUE, pointer);

        pointer += 8;

        if(deviceIdentifierSize > 0 && deviceIdentifierSize < 8)
        {
            StringBuilder sb = new StringBuilder();

            for(int x = 0; x < deviceIdentifierSize; x++)
            {
                sb.append(getCharacter(pointer));
                pointer += 8;
            }

            mDevice = ARSDevice.createFrom(sb.toString());
        }

        int userIdentifierSize = getMessage().getInt(BYTE_VALUE, pointer);
        pointer += 8;

        if(userIdentifierSize > 0 && userIdentifierSize < 8)
        {
            StringBuilder sb = new StringBuilder();

            for(int x = 0; x < userIdentifierSize; x++)
            {
                sb.append(getCharacter(pointer));
                pointer += 8;
            }

            mUser = ARSUser.createFrom(sb.toString());
        }

        int passwordSize = getMessage().getInt(BYTE_VALUE, pointer);
        pointer += 8;

        if(passwordSize > 0 && passwordSize < 8)
        {
            StringBuilder sb = new StringBuilder();

            for(int x = 0; x < passwordSize; x++)
            {
                sb.append(getCharacter(pointer));
                pointer += 8;
            }

            mPassword = ARSPassword.createFrom(sb.toString());
        }
    }

    /**
     * Returns a UTF-8 encoded character that starts at the specified offset
     *
     * @param offset of the start of the 8-bit UTF-8 encoded character
     * @return character
     */
    private char getCharacter(int offset)
    {
        return (char)getMessage().getByte(BYTE_VALUE, offset);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();

            if(hasUser())
            {
                mIdentifiers.add(getUser());
            }

            if(hasDevice())
            {
                mIdentifiers.add(getDevice());
            }

            if(hasPassword())
            {
                mIdentifiers.add(getPassword());
            }
        }

        return mIdentifiers;
    }
}
