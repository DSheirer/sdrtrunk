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

package io.github.dsheirer.jmbe;

import io.github.dsheirer.jmbe.github.Release;

/**
 * Request to create a JMBE library from the specified GitHub release version
 */
public class JmbeEditorRequest
{
    private Release mCurrentRelease;
    private boolean mCloseEditor = false;

    /**
     * Constructs an instance to request an edit of the specified release
     * @param release available on GitHub
     */
    public JmbeEditorRequest(Release release)
    {
        mCurrentRelease = release;
    }

    /**
     * Constructs a request to close and dispose of the editor
     * @param close
     */
    public JmbeEditorRequest(boolean close)
    {
        mCloseEditor = close;
    }

    /**
     * Indicates if this is a close (and dispose) editor request
     */
    public boolean isCloseEditorRequest()
    {
        return mCloseEditor;
    }

    /**
     * Release version requested
     */
    public Release getCurrentRelease()
    {
        return mCurrentRelease;
    }
}
