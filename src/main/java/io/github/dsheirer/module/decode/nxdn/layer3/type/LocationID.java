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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNCategory;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNSite;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNSystem;
import java.util.ArrayList;
import java.util.List;

/**
 * Location field containing category, system and site values.
 */
public class LocationID
{
    private static final IntField CATEGORY = IntField.length2(0);
    private static final IntField SYSTEM_GLOBAL = IntField.range(2, 11);
    private static final IntField SYSTEM_REGIONAL = IntField.range(2, 15);
    private static final IntField SYSTEM_LOCAL = IntField.range(2, 18);
    private static final IntField SITE_GLOBAL = IntField.range(12, 23);
    private static final IntField SITE_REGIONAL = IntField.range(16, 23);
    private static final IntField SITE_LOCAL = IntField.range(19, 23);

    private final LocationCategory mLocationCategory;
    private final NXDNCategory mCategory;
    private final NXDNSystem mSystem;
    private final NXDNSite mSiteOrIntegrator;
    private final boolean mPartial;

    /**
     * Used by SystemID sub-class.
     * @param integrator code
     * @param system code
     */
    protected LocationID(int integrator, int system)
    {
        mLocationCategory = LocationCategory.TYPE_D;
        mCategory = NXDNCategory.create(mLocationCategory);
        mSystem = new NXDNSystem(system);
        mSiteOrIntegrator = new NXDNSite(integrator);
        mPartial = false;
    }

    /**
     * Constructs an instance of a full Location ID field.
     * @param message containing the location ID field
     * @param offset to the start of the field.
     */
    public LocationID(CorrectedBinaryMessage message, int offset)
    {
        this(message, offset, false);
    }

    /**
     * Constructs an instance
     * @param message containing the location ID field
     * @param offset to the start of the field.
     * @param partial set to true to indicate this is a partial location ID that does not contain a site ID value.
     */
    public LocationID(CorrectedBinaryMessage message, int offset, boolean partial)
    {
        mPartial = partial;
        mLocationCategory = LocationCategory.fromValue(message.getInt(CATEGORY, offset));
        mCategory = NXDNCategory.create(mLocationCategory);

        switch(mLocationCategory)
        {
            case LOCAL:
                mSystem = NXDNSystem.create(message.getInt(SYSTEM_LOCAL, offset));
                if(mPartial)
                {
                    mSiteOrIntegrator = NXDNSite.create(0);
                }
                else
                {
                    mSiteOrIntegrator = NXDNSite.create(message.getInt(SITE_LOCAL, offset));
                }
                break;
            case REGIONAL:
                mSystem = NXDNSystem.create(message.getInt(SYSTEM_REGIONAL, offset));
                if(mPartial)
                {
                    mSiteOrIntegrator = NXDNSite.create(0);
                }
                else
                {
                    mSiteOrIntegrator = NXDNSite.create(message.getInt(SITE_REGIONAL, offset));
                }
                break;
            case GLOBAL:
                mSystem = NXDNSystem.create(message.getInt(SYSTEM_GLOBAL, offset));
                if(mPartial)
                {
                    mSiteOrIntegrator = NXDNSite.create(0);
                }
                else
                {
                    mSiteOrIntegrator = NXDNSite.create(message.getInt(SITE_GLOBAL, offset));
                }
                break;
            default:
                mSystem = NXDNSystem.create(0);
                mSiteOrIntegrator = NXDNSite.create(0);
                break;
        }
    }

    @Override
    public String toString()
    {
        if(mLocationCategory.equals(LocationCategory.TYPE_D))
        {
            return "SYSTEM:" + getSystem();
        }

        return getCategory() + " SYSTEM:" + getSystem() + (mPartial ? "" : " SITE:" + getSiteOrIntegrator());
    }

    /**
     * Location category
     */
    public NXDNCategory getCategory()
    {
        return mCategory;
    }

    /**
     * System identifier
     */
    public NXDNSystem getSystem()
    {
        return mSystem;
    }

    /**
     * Site identifier or for Type-D this is the Integrator code.
     */
    public NXDNSite getSiteOrIntegrator()
    {
        return mSiteOrIntegrator;
    }

    /**
     * List of identifiers for this location
     */
    public List<Identifier> getIdentifiers()
    {
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(getCategory());
        identifiers.add(getSystem());
        if(!mPartial)
        {
            identifiers.add(getSiteOrIntegrator());
        }

        return identifiers;
    }
}
