/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist;

/**
 * Interface for editors that use alias list controls that can trigger when the playlist manager updates the observable
 * set of alias list names and cause the editor to prompt the user to save changes.
 *
 * Implementors should clear any currently selected/editing element that could be impacted by a behind-the-scenes update
 * to the list of alias list names such as delete or rename operations.
 */
public interface IAliasListRefreshListener
{
    /**
     * Preparate foran alias list refresh.  For alias editors, this can be a clear the currently selected
     * alias so that if the value is changed, the editor doesn't think that the user has modified the alias and
     * prompt the user to save the changes.
     */
    void prepareForAliasListRefresh();
}
