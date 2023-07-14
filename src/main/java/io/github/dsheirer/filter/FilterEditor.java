/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.filter;

import java.awt.Component;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

/**
 * Filter editor
 * @param <T> item type for editing
 */
public class FilterEditor<T> extends JFrame
{
    private FilterEditorPanel<T> mEditorPanel;

    /**
     * Constructor
     * @param title for the editor window frame
     * @param owner to register the popup location
     * @param filterSet to use initially
     */
    public FilterEditor(String title, Component owner, FilterSet<T> filterSet)
    {
        if(filterSet == null)
        {
            throw new IllegalArgumentException("Unable to construct FilterEditor - FilterSet cannot be null");
        }
        setTitle(title);
        setSize(600, 400);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new MigLayout("", "[grow,fill]", "[grow,fill][]"));
        mEditorPanel = new FilterEditorPanel<>(filterSet);
        JScrollPane scroller = new JScrollPane(mEditorPanel);
        scroller.setViewportView(mEditorPanel);
        add(scroller, "wrap");
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        add(close);
    }

    /**
     * Updates this editor with the filterset.
     * @param filterSet to use in this editor.
     */
    public void updateFilterSet(FilterSet<T> filterSet)
    {
        mEditorPanel.updateFilterSet(filterSet);
    }
}
