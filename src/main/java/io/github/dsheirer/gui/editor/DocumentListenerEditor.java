/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.gui.editor;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class DocumentListenerEditor<T> extends Editor<T> implements DocumentListener
{
	private static final long serialVersionUID = 1L;

	public DocumentListenerEditor()
	{
	}

    @Override
	public void insertUpdate( DocumentEvent e )
	{
		setModified( true );
	}

	@Override
	public void removeUpdate( DocumentEvent e )
	{
		setModified( true );
	}

	@Override
	public void changedUpdate( DocumentEvent e ) { }
}
