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

package io.github.dsheirer.gui.playlist.radioreference;

import io.github.dsheirer.gui.playlist.PlaylistEditorRequest;
import io.github.dsheirer.service.radioreference.FrequencyCandidate;

/**
 * Request to show a trunked system site, or a conventional frequency, in the playlist editor's Radio Reference tab
 */
public class ViewRadioReferenceRequest extends PlaylistEditorRequest
{
    private final Integer mSystemId;
    private final Integer mSiteId;
    private final String mSourceKey;
    private final FrequencyCandidate mCandidate;

    private ViewRadioReferenceRequest(Integer systemId, Integer siteId, String sourceKey, FrequencyCandidate candidate)
    {
        mSystemId = systemId;
        mSiteId = siteId;
        mSourceKey = sourceKey;
        mCandidate = candidate;
    }

    /**
     * Request to show a trunked system with the site selected
     * @param systemId radio reference system id
     * @param siteId radio reference site id, or null to show the system only
     */
    public static ViewRadioReferenceRequest site(int systemId, Integer siteId)
    {
        return new ViewRadioReferenceRequest(systemId, siteId, null, null);
    }

    /**
     * Request to show a conventional frequency in the county or agency that lists it
     * @param sourceKey of the county or agency that lists the frequency: county:123 or agency:789
     * @param candidate frequency to select
     */
    public static ViewRadioReferenceRequest frequency(String sourceKey, FrequencyCandidate candidate)
    {
        return new ViewRadioReferenceRequest(null, null, sourceKey, candidate);
    }

    /**
     * Radio reference system id, or null for a conventional frequency
     */
    public Integer getSystemId()
    {
        return mSystemId;
    }

    /**
     * Radio reference site id, or null
     */
    public Integer getSiteId()
    {
        return mSiteId;
    }

    /**
     * Key of the county or agency that lists the conventional frequency, or null for a trunked system
     */
    public String getSourceKey()
    {
        return mSourceKey;
    }

    /**
     * Conventional frequency to select, or null for a trunked system
     */
    public FrequencyCandidate getCandidate()
    {
        return mCandidate;
    }

    @Override
    public TabName getTabName()
    {
        return TabName.RADIOREFERENCE;
    }
}
