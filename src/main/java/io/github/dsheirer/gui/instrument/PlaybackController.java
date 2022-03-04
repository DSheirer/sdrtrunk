/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.gui.instrument;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.gui.instrument.decoder.AbstractDecoderPane;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.IControllableFileSource;
import io.github.dsheirer.source.IFrameLocationListener;
import io.github.dsheirer.source.wave.ComplexWaveSource;
import io.github.dsheirer.source.wave.RealWaveSource;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

public class PlaybackController extends HBox implements IFrameLocationListener
{
    private final static Logger mLog = LoggerFactory.getLogger(PlaybackController.class);

    private Button mRewindButton;
    private TextField mPlaybackPositionText;
    private Button mPlay1Button;
    private Button mPlay10Button;
    private Button mPlay30Button;
    private Button mPlay100Button;
    private Button mPlay1000Button;
    private Button mPlay2000Button;
    private HBox mControlsBox;
    private Label mFileLabel;
    private EventHandler<ActionEvent> mPlayEventHandler;
    private AbstractDecoderPane mSampleRateListener;

    private IControllableFileSource mControllableFileSource;
    private Broadcaster<float[]> mRealBufferBroadcaster = new Broadcaster<>();
    private Broadcaster<INativeBuffer> mNativeBufferBroadcaster = new Broadcaster<>();

    public PlaybackController()
    {
        super(10);

        getChildren().addAll(getControlsBox(), getFileLabel());

        disableControls();
    }

    public void setSampleRateListener(AbstractDecoderPane decoderPane)
    {
        mSampleRateListener = decoderPane;

        if(mSampleRateListener != null && mControllableFileSource != null)
        {
            mSampleRateListener.setSampleRate(mControllableFileSource.getSampleRate());
        }
    }

    public void load(File file)
    {
        if(file != null && file.isFile())
        {
            if(ComplexWaveSource.supports(file))
            {
                try
                {
                    mControllableFileSource = new ComplexWaveSource(file);
                    mControllableFileSource.setListener(this);
                    ((ComplexWaveSource)mControllableFileSource).setListener(mNativeBufferBroadcaster);
                    mControllableFileSource.open();

                    if(mSampleRateListener != null)
                    {
                        mSampleRateListener.setSampleRate(mControllableFileSource.getSampleRate());
                    }
                }
                catch(UnsupportedAudioFileException e)
                {
                    mLog.error("Unsupported Audio File Type [" + (file != null ? file.getAbsolutePath() : "null") + "]");

                    Alert alert = new Alert(Alert.AlertType.ERROR, "The file type is unsupported.", ButtonType.OK);
                    alert.show();
                    return;
                }
                catch(IOException ioe)
                {
                    mLog.error("Error opening file [" + (file != null ? file.getAbsolutePath() : "null") + "]", ioe);

                    Alert alert = new Alert(Alert.AlertType.ERROR, "There was an error opening the file",
                        ButtonType.OK);
                    alert.show();
                    return;
                }

                enableControls();
            }
            else if(RealWaveSource.supports(file))
            {
                try
                {
                    mControllableFileSource = new RealWaveSource(file);
                    mControllableFileSource.setListener(this);
                    ((RealWaveSource)mControllableFileSource).setListener(mRealBufferBroadcaster);
                    mControllableFileSource.open();

                    if(mSampleRateListener != null)
                    {
                        mSampleRateListener.setSampleRate(mControllableFileSource.getSampleRate());
                    }
                }
                catch(UnsupportedAudioFileException e)
                {
                    mLog.error("Unsupported Audio File Type [" + (file != null ? file.getAbsolutePath() : "null") + "]");

                    Alert alert = new Alert(Alert.AlertType.ERROR, "The file type is unsupported.", ButtonType.OK);
                    alert.show();
                    return;
                }
                catch(IOException ioe)
                {
                    mLog.error("Error opening file [" + (file != null ? file.getAbsolutePath() : "null") + "]", ioe);

                    Alert alert = new Alert(Alert.AlertType.ERROR, "There was an error opening the file",
                        ButtonType.OK);
                    alert.show();
                    return;
                }

                enableControls();
            }

            getFileLabel().setText(file.getName());
        }
    }

    public void close()
    {
        if(mControllableFileSource != null)
        {
            try
            {
                mControllableFileSource.close();
            }
            catch(IOException ioe)
            {
                mLog.error("Error closing file", ioe);
            }
        }

        disableControls();
    }

    /**
     * Adds listener to receive real buffers from this playback
     */
    public void addRealListener(Listener<float[]> listener)
    {
        mRealBufferBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving real buffers from this playback
     */
    public void removeRealListener(Listener<float[]> listener)
    {
        mRealBufferBroadcaster.removeListener(listener);
    }

    /**
     * Adds listener to receive complex buffers from this playback
     */
    public void addComplexListener(Listener<INativeBuffer> listener)
    {
        mNativeBufferBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving compex buffers from this playback
     */
    public void removeComplexListener(Listener<INativeBuffer> listener)
    {
        mNativeBufferBroadcaster.removeListener(listener);
    }

    /**
     * Sets all of the playback controls to the specified enabled state.
     */
    private void disableControls()
    {
        getRewindButton().setDisable(true);
        getPlaybackPositionText().setDisable(true);
        getPlay1Button().setDisable(true);
        getPlay10Button().setDisable(true);
        getPlay30Button().setDisable(true);
        getPlay100Button().setDisable(true);
        getPlay1000Button().setDisable(true);
        getPlay2000Button().setDisable(true);
    }

    /**
     * Sets all of the playback controls to the specified enabled state.
     */
    private void enableControls()
    {
        getRewindButton().setDisable(false);
        getPlaybackPositionText().setDisable(false);
        getPlay1Button().setDisable(false);
        getPlay10Button().setDisable(false);
        getPlay30Button().setDisable(false);
        getPlay100Button().setDisable(false);
        getPlay1000Button().setDisable(false);
        getPlay2000Button().setDisable(false);
    }

    private HBox getControlsBox()
    {
        if(mControlsBox == null)
        {
            mControlsBox = new HBox();

            mControlsBox.getChildren().addAll(getRewindButton(), getPlaybackPositionText(), getPlay1Button(),
                getPlay10Button(), getPlay30Button(), getPlay100Button(), getPlay1000Button(), getPlay2000Button());
        }

        return mControlsBox;
    }


    private  Button getRewindButton()
    {
        if(mRewindButton == null)
        {
            mRewindButton = new Button();
            IconNode iconNode = new IconNode(FontAwesome.FAST_BACKWARD);
            iconNode.setIconSize(10);
            mRewindButton.setGraphic(iconNode);
        }

        return mRewindButton;
    }

    private  Button getPlay1Button()
    {
        if(mPlay1Button == null)
        {
            mPlay1Button = new Button("1");
            mPlay1Button.setUserData(1);
            mPlay1Button.setOnAction(getPlaybackEventHandler());
            IconNode iconNode = new IconNode(FontAwesome.PLAY);
            iconNode.setIconSize(10);
            mPlay1Button.setGraphic(iconNode);
        }

        return mPlay1Button;
    }

    private  Button getPlay10Button()
    {
        if(mPlay10Button == null)
        {
            mPlay10Button = new Button("10");
            mPlay10Button.setUserData(10);
            mPlay10Button.setOnAction(getPlaybackEventHandler());
            IconNode iconNode = new IconNode(FontAwesome.FAST_FORWARD);
            iconNode.setIconSize(10);
            mPlay10Button.setGraphic(iconNode);
        }

        return mPlay10Button;
    }

    private  Button getPlay30Button()
    {
        if(mPlay30Button == null)
        {
            mPlay30Button = new Button("30");
            mPlay30Button.setUserData(30);
            mPlay30Button.setOnAction(getPlaybackEventHandler());
            IconNode iconNode = new IconNode(FontAwesome.FAST_FORWARD);
            iconNode.setIconSize(10);
            mPlay30Button.setGraphic(iconNode);
        }

        return mPlay30Button;
    }

    private  Button getPlay100Button()
    {
        if(mPlay100Button == null)
        {
            mPlay100Button = new Button("100");
            mPlay100Button.setUserData(100);
            mPlay100Button.setOnAction(getPlaybackEventHandler());
            IconNode iconNode = new IconNode(FontAwesome.FAST_FORWARD);
            iconNode.setIconSize(10);
            mPlay100Button.setGraphic(iconNode);
        }

        return mPlay100Button;
    }

    private  Button getPlay1000Button()
    {
        if(mPlay1000Button == null)
        {
            mPlay1000Button = new Button("1000");
            mPlay1000Button.setUserData(1000);
            mPlay1000Button.setOnAction(getPlaybackEventHandler());
            IconNode iconNode = new IconNode(FontAwesome.FAST_FORWARD);
            iconNode.setIconSize(10);
            mPlay1000Button.setGraphic(iconNode);
        }

        return mPlay1000Button;
    }

    private  Button getPlay2000Button()
    {
        if(mPlay2000Button == null)
        {
            mPlay2000Button = new Button("2000");
            mPlay2000Button.setUserData(2000);
            mPlay2000Button.setOnAction(getPlaybackEventHandler());
            IconNode iconNode = new IconNode(FontAwesome.FAST_FORWARD);
            iconNode.setIconSize(10);
            mPlay2000Button.setGraphic(iconNode);
        }

        return mPlay2000Button;
    }

    private EventHandler<ActionEvent> getPlaybackEventHandler()
    {
        if(mPlayEventHandler == null)
        {
            mPlayEventHandler = new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    int count = (int)((Button)event.getSource()).getUserData();

                    if(mControllableFileSource != null)
                    {
                        try
                        {
                            mControllableFileSource.next(count, true);
                        }
                        catch(IOException ioe)
                        {
                            mLog.error("Error while playing samples");
                        }
                    }

                }
            };
        }

        return mPlayEventHandler;
    }

    private  TextField getPlaybackPositionText()
    {
        if(mPlaybackPositionText == null)
        {
            mPlaybackPositionText = new TextField("0");
            mPlaybackPositionText.setAlignment(Pos.CENTER);
            mPlaybackPositionText.setPrefWidth(80);
        }

        return mPlaybackPositionText;
    }

    private  Label getFileLabel()
    {
        if(mFileLabel == null)
        {
            mFileLabel = new Label();
        }

        return mFileLabel;
    }

    @Override
    public void frameLocationUpdated(int location)
    {
        getPlaybackPositionText().setText(String.valueOf(location));
    }

    @Override
    public void frameLocationReset()
    {
        mLog.info("Frame location reset ... ignoring?");
    }
}
