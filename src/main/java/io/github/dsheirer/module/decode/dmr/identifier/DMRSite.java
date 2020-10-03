/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.dmr.identifier;

import io.github.dsheirer.identifier.site.SiteIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * DMR Radio Site Identifier
 */
public class DMRSite extends SiteIdentifier
{
    /**
     * Constructs an instance
     * @param site id
     */
    public DMRSite(int site)
    {
        super(site);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.DMR;
    }

    /**
     * Utility method to create a site identifier
     */
    public static DMRSite create(int site)
    {
        return new DMRSite(site);
    }
}
