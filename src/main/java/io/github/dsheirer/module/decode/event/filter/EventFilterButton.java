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

package io.github.dsheirer.module.decode.event.filter;

import com.jidesoft.swing.JideButton;
import io.github.dsheirer.filter.FilterEditorPanel;
import io.github.dsheirer.filter.FilterSet;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

/**
 * Event filter button that includes split button functionality to allow user to select filter items.
 *
 * @param <T> type of filter.
 */
public class EventFilterButton<T> extends JideButton
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs an instance
     * @param dialogTitle for the dialog/panel that appears
     * @param filterSet to include in the editor panel.
     */
    public EventFilterButton(String dialogTitle, FilterSet<T> filterSet)
    {
        this("Filter", dialogTitle, filterSet);
    }

    /**
     * Constructs an instance
     * @param buttonLabel to use on the button
     * @param dialogTitle for the dialog/panel that appears
     * @param filterSet to include in the editor panel.
     */
    public EventFilterButton(String buttonLabel, String dialogTitle, FilterSet<T> filterSet)
    {
        super(buttonLabel);
        addActionListener(new EventFilterActionHandler(dialogTitle, filterSet));
    }

    /**
     * Action handler for the button
     */
    public class EventFilterActionHandler implements ActionListener
    {
        private String mTitle;
        private FilterSet<T> mFilterSet;

        /**
         * Constructs an instance
         * @param title for this panel
         * @param filterSet to edit
         */
        public EventFilterActionHandler(String title, FilterSet<T> filterSet)
        {
            mTitle = title;
            mFilterSet = filterSet;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            final JFrame editor = new JFrame();
            editor.setTitle(mTitle);
            editor.setLocationRelativeTo(EventFilterButton.this);
            editor.setSize(600, 400);
            editor.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            editor.setLayout(new MigLayout("", "[grow,fill]", "[grow,fill][][]"));
            FilterEditorPanel<T> panel = new FilterEditorPanel<T>(mFilterSet);
            JScrollPane scroller = new JScrollPane(panel);
            scroller.setViewportView(panel);
            editor.add(scroller, "wrap");
            JButton close = new JButton("Close");
            close.addActionListener(e1 -> editor.dispose());
            editor.add(close);
            EventQueue.invokeLater(() -> editor.setVisible(true));
        }
    }
}