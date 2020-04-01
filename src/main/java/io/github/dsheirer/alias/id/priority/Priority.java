/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.alias.id.priority;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;

/**
 * Specifies an (audio) priority level for this alias.
 */
public class Priority extends AliasID
{
    public static final int DO_NOT_MONITOR = -1;
    public static final int SELECTED_PRIORITY = 0;
    public static final int MIN_PRIORITY = 1;
    public static final int MAX_PRIORITY = 100;
    public static final int DEFAULT_PRIORITY = 100;

    private int mPriority = DEFAULT_PRIORITY;

    public Priority()
    {
    }

    public Priority(int priority)
    {
        mPriority = priority;
    }

    @Override
    public boolean isAudioIdentifier()
    {
        return true;
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    /**
     * Indicates the associated alias should not be monitored or tracked.
     */
    @JsonIgnore
    public boolean isDoNotMonitor()
    {
        return mPriority == DO_NOT_MONITOR;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "priority")
    public int getPriority()
    {
        return mPriority;
    }

    public void setPriority(int priority)
    {
        mPriority = priority;
        updateValueProperty();
    }

    public String toString()
    {
        return "Audio Priority: " + (isDoNotMonitor() ? "Do Not Monitor" : mPriority);
    }

    @Override
    public boolean matches(AliasID id)
    {
        return false;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public AliasIDType getType()
    {
        return AliasIDType.PRIORITY;
    }
}
