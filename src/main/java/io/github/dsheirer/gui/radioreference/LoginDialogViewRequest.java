/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.gui.radioreference;

import io.github.dsheirer.rrapi.type.AuthorizationInformation;
import io.github.dsheirer.sample.Listener;

/**
 * Request to show the radio reference service login dialog
 */
public class LoginDialogViewRequest
{
    private Listener<AuthorizationInformation> mListener;

    /**
     * Constructs an instance.
     * @param listener to receive authorization information results
     */
    public LoginDialogViewRequest(Listener<AuthorizationInformation> listener)
    {
        mListener = listener;
    }

    /**
     * Listerer to receive login credentials
     * @return listener
     */
    public Listener<AuthorizationInformation> getListener()
    {
        return mListener;
    }
}
