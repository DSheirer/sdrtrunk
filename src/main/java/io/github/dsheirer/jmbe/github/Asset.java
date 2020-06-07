/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.jmbe.github;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * GitHub Asset
 */
public class Asset
{
    private JsonObject mJsonObject;

    public Asset(JsonObject jsonObject)
    {
        mJsonObject = jsonObject;
    }

    public String getName()
    {
        return getTag("name");
    }

    /**
     * URL to download this asset
     */
    public String getDownloadUrl()
    {
        return getTag("browser_download_url");
    }

    @Override
    public String toString()
    {
        return getName();
    }

    /**
     * Access the string contents of a json tag
     * @param tagName to access
     * @return string value or null
     */
    private String getTag(String tagName)
    {
        if(mJsonObject != null)
        {
            JsonElement element = mJsonObject.get(tagName);

            if(!element.isJsonNull())
            {
                return element.toString().replace("\"", "");
            }
        }

        return null;
    }
}
