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

package io.github.dsheirer.source.tuner.ui;

import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Titled card in a tuner editor that lays out labelled controls in rows: a label column, a control column that
 * grows, and an optional trailing column (e.g. a value label or a button).
 */
public class TunerSection extends VBox
{
    private final Label mTitle;
    private final GridPane mGrid = new GridPane();
    private int mRow = 0;

    /**
     * Constructs an instance
     * @param title for the section
     */
    public TunerSection(String title)
    {
        getStyleClass().add("tuner-section");
        mTitle = new Label(title);
        mTitle.getStyleClass().add("tuner-section-title");

        mGrid.getStyleClass().add("tuner-section-grid");
        ColumnConstraints labels = new ColumnConstraints();
        labels.setHalignment(HPos.LEFT);
        labels.setMinWidth(Label.USE_PREF_SIZE);
        ColumnConstraints controls = new ColumnConstraints();
        controls.setHgrow(Priority.ALWAYS);
        controls.setFillWidth(true);
        ColumnConstraints trailing = new ColumnConstraints();
        trailing.setMinWidth(Label.USE_PREF_SIZE);
        mGrid.getColumnConstraints().addAll(labels, controls, trailing);

        getChildren().addAll(mTitle, mGrid);
    }

    /**
     * Title label of the section.
     */
    public Label getTitleLabel()
    {
        return mTitle;
    }

    /**
     * Adds a row with a text label and a control.
     * @return the label that was created, so that it can be hidden along with the control
     */
    public Label addRow(String label, Node control)
    {
        Label labelNode = createLabel(label);
        addRow(labelNode, control, null);
        return labelNode;
    }

    /**
     * Adds a row with a text label, a control and a trailing node.
     * @return the label that was created
     */
    public Label addRow(String label, Node control, Node trailing)
    {
        Label labelNode = createLabel(label);
        addRow(labelNode, control, trailing);
        return labelNode;
    }

    /**
     * Adds a row with a label node (may be null), a control and an optional trailing node.
     */
    public void addRow(Node label, Node control, Node trailing)
    {
        if(label != null)
        {
            GridPane.setValignment(label, VPos.CENTER);
            mGrid.add(label, 0, mRow);
            bindManaged(label);
        }

        if(control != null)
        {
            if(trailing == null)
            {
                mGrid.add(control, 1, mRow, 2, 1);
            }
            else
            {
                mGrid.add(control, 1, mRow);
            }

            bindManaged(control);
        }

        if(trailing != null)
        {
            mGrid.add(trailing, 2, mRow);
            bindManaged(trailing);
        }

        mRow++;
    }

    /**
     * Adds a control (e.g. a check box) without a label, aligned with the other controls.
     */
    public void addControl(Node control)
    {
        addRow((Node)null, control, null);
    }

    /**
     * Adds a node that spans the full width of the section.
     */
    public void addFullRow(Node node)
    {
        mGrid.add(node, 0, mRow, 3, 1);
        bindManaged(node);
        mRow++;
    }

    /**
     * Adds a row of nodes laid out left to right, under the control column.
     */
    public HBox addFlowRow(Node... nodes)
    {
        HBox box = new HBox(nodes);
        box.getStyleClass().add("tuner-flow-row");
        box.setAlignment(Pos.CENTER_LEFT);
        addRow((Node)null, box, null);
        return box;
    }

    /**
     * Creates a row label.
     */
    public static Label createLabel(String text)
    {
        Label label = new Label(text);
        label.getStyleClass().add("tuner-row-label");
        return label;
    }

    /**
     * Hides nodes from the layout when they are not visible.
     */
    private static void bindManaged(Node node)
    {
        if(!node.managedProperty().isBound())
        {
            node.managedProperty().bind(node.visibleProperty());
        }
    }
}
