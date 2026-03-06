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

package io.github.dsheirer.module.decode.nxdn.layer3.call;

/**
 * Reassembles short and packet data
 */
public class PacketDataAssembler
{
    //TODO: implement the assembler and change the return type of the header/data block methods to return either
    // the assembled message or null.

    /**
     * Process a short data header on control or traffic channel
     * @param header to process
     */
    public void process(ShortDataCallRequestHeader header)
    {
        System.out.println("Short Data Call Header received: " + header);
    }

    /**
     * Process a short data block on control or traffic channel
     * @param data to process
     */
    public void process(ShortDataCallBlock data)
    {
        System.out.println("Short Data Call Block received: " + data);
    }

    /**
     * Process a short data call encryption initialization vector.
     * @param iv to process
     */
    public void process(ShortDataInitializationVector iv)
    {
        System.out.println("Short Data Initialization Vector received: " + iv);
    }

    /**
     * Process a data call header (on the traffic channel)
     * @param header to process
     */
    public void process(DataCallHeader header)
    {
        System.out.println("Data Call Header received: " + header);
    }

    /**
     * Process a data call block (on the traffic channel)
     * @param data to process
     */
    public void process(DataCallBlock data)
    {
        System.out.println("Data Call Block received: " + data);
    }
}
