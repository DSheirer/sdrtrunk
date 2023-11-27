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

import io.github.dsheirer.alias.id.talkgroup.P25FullyQualifiedTalkgroup;
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
 * Editor for P25 fully qualified talkgroup alias identifiers
 */
public class P25FullyQualifiedTalkgroupEditor extends IdentifierEditor<P25FullyQualifiedTalkgroup>
{
    private static final Logger mLog = LoggerFactory.getLogger(P25FullyQualifiedTalkgroupEditor.class);

    private UserPreferences mUserPreferences;
    private Label mProtocolLabel;
    private TextField mWacnField;
    private TextField mSystemField;
    private TextField mTalkgroupField;
    private TextFormatter<Integer> mWacnTextFormatter;
    private TextFormatter<Integer> mSystemTextFormatter;
    private TextFormatter<Integer> mTalkgroupTextFormatter;
    private WacnValueChangeListener mWacnValueChangeListener = new WacnValueChangeListener();
    private SystemValueChangeListener mSystemValueChangeListener = new SystemValueChangeListener();
    private TalkgroupValueChangeListener mTalkgroupValueChangeListener = new TalkgroupValueChangeListener();

    /**
     * Constructs an instance
     * @param userPreferences for determining user preferred talkgroup formats
     */
    public P25FullyQualifiedTalkgroupEditor(UserPreferences userPreferences)
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

        Label radioLabel = new Label("Talkgroup");
        GridPane.setHalignment(radioLabel, HPos.RIGHT);
        GridPane.setConstraints(radioLabel, 5, 0);
        gridPane.getChildren().add(radioLabel);

        GridPane.setConstraints(getTalkgroupField(), 6, 0);
        gridPane.getChildren().add(getTalkgroupField());

        getChildren().add(gridPane);
    }

    @Override
    public void setItem(P25FullyQualifiedTalkgroup item)
    {
        super.setItem(item);

        P25FullyQualifiedTalkgroup fqt = getItem();

        getProtocolLabel().setDisable(fqt == null);
        getWacnField().setDisable(fqt == null);
        getSystemField().setDisable(fqt == null);
        getTalkgroupField().setDisable(fqt == null);

        if(fqt != null)
        {
            getProtocolLabel().setText(fqt.getProtocol().toString());
            updateTextFormatter();
        }
        else
        {
            getWacnField().setText(null);
            getSystemField().setText(null);
            getTalkgroupField().setText(null);
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
        if(mTalkgroupTextFormatter != null)
        {
            mTalkgroupTextFormatter.valueProperty().removeListener(mTalkgroupValueChangeListener);
        }

        IntegerFormat format = mUserPreferences.getTalkgroupFormatPreference().getTalkgroupFormat(getItem().getProtocol());

        if(format == IntegerFormat.DECIMAL && (mTalkgroupTextFormatter == null || !(mTalkgroupTextFormatter instanceof IntegerFormatter)))
        {
            mWacnTextFormatter = new IntegerFormatter(0,0xFFFFF);
            mSystemTextFormatter = new IntegerFormatter(0,0xFFF);
            mTalkgroupTextFormatter = new IntegerFormatter(0,0xFFFF);

            mWacnField.setTooltip(new Tooltip("Format: 0 - 1048575"));
            mSystemField.setTooltip(new Tooltip("Format: 0 - 4095"));
            mTalkgroupField.setTooltip(new Tooltip("Format: 0 - 65535"));
        }
        else if(format == IntegerFormat.HEXADECIMAL && (mTalkgroupTextFormatter == null || !(mTalkgroupTextFormatter instanceof HexFormatter)))
        {
            mWacnTextFormatter = new HexFormatter(0,0xFFFFF);
            mSystemTextFormatter = new HexFormatter(0,0xFFF);
            mTalkgroupTextFormatter = new HexFormatter(0,0xFFFFFF);

            mWacnField.setTooltip(new Tooltip("Format: 0 - FFFFF"));
            mSystemField.setTooltip(new Tooltip("Format: 0 - FFF"));
            mTalkgroupField.setTooltip(new Tooltip("Format: 0 - FFFF"));
        }

        mWacnField.setTextFormatter(mWacnTextFormatter);
        mSystemField.setTextFormatter(mSystemTextFormatter);
        mTalkgroupField.setTextFormatter(mTalkgroupTextFormatter);

        mWacnTextFormatter.setValue(getItem() != null ? getItem().getWacn() : null);
        mSystemTextFormatter.setValue(getItem() != null ? getItem().getSystem() : null);
        mTalkgroupTextFormatter.setValue(getItem() != null ? getItem().getValue() : null);

        mWacnTextFormatter.valueProperty().addListener(mWacnValueChangeListener);
        mSystemTextFormatter.valueProperty().addListener(mSystemValueChangeListener);
        mTalkgroupTextFormatter.valueProperty().addListener(mTalkgroupValueChangeListener);
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

    private TextField getTalkgroupField()
    {
        if(mTalkgroupField == null)
        {
            mTalkgroupField = new TextField();
            mTalkgroupField.setTextFormatter(mTalkgroupTextFormatter);
        }

        return mTalkgroupField;
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

    public class TalkgroupValueChangeListener implements ChangeListener<Integer>
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
