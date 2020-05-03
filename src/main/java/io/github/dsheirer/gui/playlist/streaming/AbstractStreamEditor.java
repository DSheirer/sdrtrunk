/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.streaming;

import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * Base class for broadcast configuration editors.
 */
public abstract class AbstractStreamEditor<T extends BroadcastConfiguration> extends AbstractBroadcastEditor<T>
{
    private TextField mHostTextField;
    private TextField mUnMaskedPasswordTextField;
    private PasswordField mMaskedPasswordTextField;
    private CheckBox mShowPasswordCheckBox;
    private IntegerTextField mPortTextField;
    private IntegerTextField mDelayTextField;
    private IntegerTextField mMaxAgeTextField;

    /**
     * Constructs an instance
     */
    public AbstractStreamEditor(PlaylistManager playlistManager)
    {
        super(playlistManager);
    }

    @Override
    public void setItem(T item)
    {
        super.setItem(item);

        getHostTextField().setDisable(item == null);
        getPortTextField().setDisable(item == null);
        getMaskedPasswordTextField().setDisable(item == null);
        getUnMaskedPasswordTextField().setDisable(item == null);
        getMaxAgeTextField().setDisable(item == null);
        getDelayTextField().setDisable(item == null);

        if(item != null)
        {
            getHostTextField().setText(item.getHost());
            getPortTextField().set(item.getPort());
            getMaskedPasswordTextField().setText(item.getPassword());
            getMaxAgeTextField().set((int)(item.getMaximumRecordingAge() / 1000)); //Convert millis to seconds
            getDelayTextField().set((int)(item.getDelay() / 1000)); //Convert millis to seconds
        }
        else
        {
            getHostTextField().setText(null);
            getPortTextField().set(0);
            getMaskedPasswordTextField().setText(null);
            getMaxAgeTextField().set(0);
            getDelayTextField().set(0);
        }

        modifiedProperty().set(false);
    }

    @Override
    public void dispose()
    {
    }

    @Override
    public void save()
    {
        BroadcastConfiguration configuration = getItem();

        if(configuration != null)
        {
            configuration.setHost(getHostTextField().getText());
            configuration.setPort(getPortTextField().get());
            configuration.setPassword(getMaskedPasswordTextField().getText());
            configuration.setDelay(getDelayTextField().get() * 1000); //Convert seconds to millis
            configuration.setMaximumRecordingAge(getMaxAgeTextField().get() * 1000); //Convert seconds to millis
        }

        super.save();
    }

    protected abstract GridPane getEditorPane();

    protected TextField getHostTextField()
    {
        if(mHostTextField == null)
        {
            mHostTextField = new TextField();
            mHostTextField.setDisable(true);
            mHostTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mHostTextField;
    }

    protected TextField getUnMaskedPasswordTextField()
    {
        if(mUnMaskedPasswordTextField == null)
        {
            mUnMaskedPasswordTextField = new TextField();
            mUnMaskedPasswordTextField.setDisable(true);
            mUnMaskedPasswordTextField.visibleProperty().bind(getShowPasswordCheckBox().selectedProperty());
            mUnMaskedPasswordTextField.textProperty().bindBidirectional(getMaskedPasswordTextField().textProperty());
        }

        return mUnMaskedPasswordTextField;
    }

    protected PasswordField getMaskedPasswordTextField()
    {
        if(mMaskedPasswordTextField == null)
        {
            mMaskedPasswordTextField = new PasswordField();
            mMaskedPasswordTextField.setDisable(true);
            mMaskedPasswordTextField.visibleProperty().bind(getShowPasswordCheckBox().selectedProperty().not());
            mMaskedPasswordTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mMaskedPasswordTextField;
    }

    protected CheckBox getShowPasswordCheckBox()
    {
        if(mShowPasswordCheckBox == null)
        {
            mShowPasswordCheckBox = new CheckBox("Show");
        }

        return mShowPasswordCheckBox;
    }

    protected IntegerTextField getPortTextField()
    {
        if(mPortTextField == null)
        {
            mPortTextField = new IntegerTextField();
            mPortTextField.setDisable(true);
            mPortTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mPortTextField;
    }

    protected IntegerTextField getDelayTextField()
    {
        if(mDelayTextField == null)
        {
            mDelayTextField = new IntegerTextField();
            mDelayTextField.setDisable(true);
            mDelayTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mDelayTextField;
    }

    protected IntegerTextField getMaxAgeTextField()
    {
        if(mMaxAgeTextField == null)
        {
            mMaxAgeTextField = new IntegerTextField();
            mMaxAgeTextField.setDisable(true);
            mMaxAgeTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mMaxAgeTextField;
    }
}
