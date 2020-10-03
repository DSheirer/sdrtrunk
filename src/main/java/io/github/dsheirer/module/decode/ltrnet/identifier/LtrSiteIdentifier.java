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

package io.github.dsheirer.module.decode.ltrnet.identifier;

import io.github.dsheirer.identifier.site.SiteIdentifier;
import io.github.dsheirer.protocol.Protocol;

public class LtrSiteIdentifier extends SiteIdentifier
{
    public LtrSiteIdentifier(int site)
    {
        super(site);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.LTR_NET;
    }

    public static LtrSiteIdentifier create(int site)
    {
        return new LtrSiteIdentifier(site);
    }
}
