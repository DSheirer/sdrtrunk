/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.alias.identifier;

import io.github.dsheirer.alias.id.nac.Nac;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.util.converter.IntegerStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Editor for P25 Network Access Code (NAC) alias identifiers
 */
public class NacEditor extends IdentifierEditor<Nac>
{
    private static final Logger mLog = LoggerFactory.getLogger(NacEditor.class);
    private Spinner<Integer> mNacSpinner;
    private Label mHexLabel;

    /**
     * Constructs an instance
     */
    public NacEditor()
    {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);

        Label typeLabel = new Label("NAC (0-4095)");
        GridPane.setConstraints(typeLabel, 0, 0);
        gridPane.getChildren().add(typeLabel);

        GridPane.setConstraints(getNacSpinner(), 1, 0);
        gridPane.getChildren().add(getNacSpinner());

        mHexLabel = new Label("(x000)");
        GridPane.setConstraints(mHexLabel, 2, 0);
        gridPane.getChildren().add(mHexLabel);

        getChildren().add(gridPane);
    }

    @Override
    public void setItem(Nac item)
    {
        super.setItem(item);
        getNacSpinner().getValueFactory().setValue(item.getNac());
        updateHexLabel(item.getNac());
        modifiedProperty().set(false);
    }

    @Override
    public void save()
    {
        //no-op
    }

    @Override
    public void dispose()
    {
        //no-op
    }

    /**
     * Updates the hex display label
     */
    private void updateHexLabel(int value)
    {
        mHexLabel.setText("(x" + String.format("%03X", value) + ")");
    }

    /**
     * Spinner for NAC value entry
     * @return spinner
     */
    private Spinner<Integer> getNacSpinner()
    {
        if(mNacSpinner == null)
        {
            mNacSpinner = new Spinner<>();
            mNacSpinner.setEditable(true);
            mNacSpinner.setPrefWidth(100);

            SpinnerValueFactory<Integer> factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 4095, 0);
            mNacSpinner.setValueFactory(factory);

            // Add text formatter to handle invalid input
            TextFormatter<Integer> formatter = new TextFormatter<>(new IntegerStringConverter(), 0, change -> {
                String newText = change.getControlNewText();
                if(newText.isEmpty())
                {
                    return change;
                }
                try
                {
                    int value = Integer.parseInt(newText);
                    if(value >= 0 && value <= 4095)
                    {
                        return change;
                    }
                }
                catch(NumberFormatException e)
                {
                    // Also allow hex input
                    if(newText.toLowerCase().startsWith("x") || newText.toLowerCase().startsWith("0x"))
                    {
                        try
                        {
                            String hexPart = newText.toLowerCase().startsWith("0x") ? 
                                newText.substring(2) : newText.substring(1);
                            int value = Integer.parseInt(hexPart, 16);
                            if(value >= 0 && value <= 4095)
                            {
                                return change;
                            }
                        }
                        catch(NumberFormatException ex)
                        {
                            // Fall through to return null
                        }
                    }
                }
                return null;
            });
            mNacSpinner.getEditor().setTextFormatter(formatter);

            mNacSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
                if(getItem() != null && newValue != null)
                {
                    getItem().setNac(newValue);
                    updateHexLabel(newValue);
                    modifiedProperty().set(true);
                }
            });
        }

        return mNacSpinner;
    }
}