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

package io.github.dsheirer.gui.playlist.alias.identifier;

import io.github.dsheirer.alias.id.radio.P25FullyQualifiedRadio;
import io.github.dsheirer.gui.control.HexFormatter;
import io.github.dsheirer.gui.control.IntegerFormatter;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.identifier.IntegerFormat;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Editor for P25 fully qualified radio alias identifiers
 */
public class P25FullyQualifiedRadioIdEditor extends IdentifierEditor<P25FullyQualifiedRadio>
{
    private static final Logger mLog = LoggerFactory.getLogger(P25FullyQualifiedRadioIdEditor.class);

    private UserPreferences mUserPreferences;
    private Label mProtocolLabel;
    private TextField mWacnField;
    private TextField mSystemField;
    private TextField mRadioField;
    private TextFormatter<Integer> mWacnTextFormatter;
    private TextFormatter<Integer> mSystemTextFormatter;
    private TextFormatter<Integer> mRadioTextFormatter;
    private WacnValueChangeListener mWacnValueChangeListener = new WacnValueChangeListener();
    private SystemValueChangeListener mSystemValueChangeListener = new SystemValueChangeListener();
    private RadioValueChangeListener mRadioValueChangeListener = new RadioValueChangeListener();

    /**
     * Constructs an instance
     * @param userPreferences for determining user preferred talkgroup formats
     */
    public P25FullyQualifiedRadioIdEditor(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;

        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(3);

        GridPane.setConstraints(getProtocolLabel(), 0, 0);
        gridPane.getChildren().add(getProtocolLabel());

        Label valueLabel = new Label("WACN");
        GridPane.setHalignment(valueLabel, HPos.RIGHT);
        GridPane.setConstraints(valueLabel, 1, 0);
        gridPane.getChildren().add(valueLabel);

        GridPane.setConstraints(getWacnField(), 2, 0);
        gridPane.getChildren().add(getWacnField());

        Label systemLabel = new Label("System");
        GridPane.setHalignment(systemLabel, HPos.RIGHT);
        GridPane.setConstraints(systemLabel, 3, 0);
        gridPane.getChildren().add(systemLabel);

        GridPane.setConstraints(getSystemField(), 4, 0);
        gridPane.getChildren().add(getSystemField());

        Label radioLabel = new Label("Radio ID");
        GridPane.setHalignment(radioLabel, HPos.RIGHT);
        GridPane.setConstraints(radioLabel, 5, 0);
        gridPane.getChildren().add(radioLabel);

        GridPane.setConstraints(getRadioField(), 6, 0);
        gridPane.getChildren().add(getRadioField());

        getChildren().add(gridPane);
    }

    @Override
    public void setItem(P25FullyQualifiedRadio item)
    {
        super.setItem(item);

        P25FullyQualifiedRadio fqr = getItem();

        getProtocolLabel().setDisable(fqr == null);
        getWacnField().setDisable(fqr == null);
        getSystemField().setDisable(fqr == null);
        getRadioField().setDisable(fqr == null);

        if(fqr != null)
        {
            getProtocolLabel().setText(fqr.getProtocol().toString());
            updateTextFormatter();
        }
        else
        {
            getWacnField().setText(null);
            getSystemField().setText(null);
            getRadioField().setText(null);
        }

        modifiedProperty().set(false);
    }

    private void updateTextFormatter()
    {
        if(mWacnTextFormatter != null)
        {
            mWacnTextFormatter.valueProperty().removeListener(mWacnValueChangeListener);
        }
        if(mSystemTextFormatter != null)
        {
            mSystemTextFormatter.valueProperty().removeListener(mSystemValueChangeListener);
        }
        if(mRadioTextFormatter != null)
        {
            mRadioTextFormatter.valueProperty().removeListener(mRadioValueChangeListener);
        }

        IntegerFormat format = mUserPreferences.getTalkgroupFormatPreference().getTalkgroupFormat(getItem().getProtocol());

        if(format == IntegerFormat.DECIMAL && (mRadioTextFormatter == null || !(mRadioTextFormatter instanceof IntegerFormatter)))
        {
            mWacnTextFormatter = new IntegerFormatter(0,0xFFFFF);
            mSystemTextFormatter = new IntegerFormatter(0,0xFFF);
            mRadioTextFormatter = new IntegerFormatter(0,0xFFFFFF);

            mWacnField.setTooltip(new Tooltip("Format: 0 - 1048575"));
            mSystemField.setTooltip(new Tooltip("Format: 0 - 4095"));
            mRadioField.setTooltip(new Tooltip("Format: 0 - 16777215"));
        }
        else if(format == IntegerFormat.HEXADECIMAL && (mRadioTextFormatter == null || !(mRadioTextFormatter instanceof HexFormatter)))
        {
            mWacnTextFormatter = new HexFormatter(0,0xFFFFF);
            mSystemTextFormatter = new HexFormatter(0,0xFFF);
            mRadioTextFormatter = new HexFormatter(0,0xFFFFFF);

            mWacnField.setTooltip(new Tooltip("Format: 0 - FFFFF"));
            mSystemField.setTooltip(new Tooltip("Format: 0 - FFF"));
            mRadioField.setTooltip(new Tooltip("Format: 0 - FFFFFF"));
        }

        mWacnField.setTextFormatter(mWacnTextFormatter);
        mSystemField.setTextFormatter(mSystemTextFormatter);
        mRadioField.setTextFormatter(mRadioTextFormatter);

        mWacnTextFormatter.setValue(getItem() != null ? getItem().getWacn() : null);
        mSystemTextFormatter.setValue(getItem() != null ? getItem().getSystem() : null);
        mRadioTextFormatter.setValue(getItem() != null ? getItem().getValue() : null);

        mWacnTextFormatter.valueProperty().addListener(mWacnValueChangeListener);
        mSystemTextFormatter.valueProperty().addListener(mSystemValueChangeListener);
        mRadioTextFormatter.valueProperty().addListener(mRadioValueChangeListener);
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

    private Label getProtocolLabel()
    {
        if(mProtocolLabel == null)
        {
            mProtocolLabel = new Label();
        }

        return mProtocolLabel;
    }

    private TextField getWacnField()
    {
        if(mWacnField == null)
        {
            mWacnField = new TextField();
            mWacnField.setTextFormatter(mWacnTextFormatter);
        }

        return mWacnField;
    }

    private TextField getSystemField()
    {
        if(mSystemField == null)
        {
            mSystemField = new TextField();
            mSystemField.setTextFormatter(mSystemTextFormatter);
        }

        return mSystemField;
    }

    private TextField getRadioField()
    {
        if(mRadioField == null)
        {
            mRadioField = new TextField();
            mRadioField.setTextFormatter(mRadioTextFormatter);
        }

        return mRadioField;
    }

    public class WacnValueChangeListener implements ChangeListener<Integer>
    {
        @Override
        public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue)
        {
            if(getItem() != null)
            {
                getItem().setWacn(newValue != null ? newValue : 0);
                modifiedProperty().set(true);
            }
        }
    }

    public class SystemValueChangeListener implements ChangeListener<Integer>
    {
        @Override
        public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue)
        {
            if(getItem() != null)
            {
                getItem().setSystem(newValue != null ? newValue : 0);
                modifiedProperty().set(true);
            }
        }
    }

    public class RadioValueChangeListener implements ChangeListener<Integer>
    {
        @Override
        public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue)
        {
            if(getItem() != null)
            {
                getItem().setValue(newValue != null ? newValue : 0);
                modifiedProperty().set(true);
            }
        }
    }
}
