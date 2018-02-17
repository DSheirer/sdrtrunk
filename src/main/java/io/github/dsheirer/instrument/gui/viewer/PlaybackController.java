/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.instrument.gui.viewer;

import io.github.dsheirer.instrument.gui.viewer.decoder.DecoderPane;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexBuffer;
import io.github.dsheirer.source.IControllableFileSource;
import io.github.dsheirer.source.IFrameLocationListener;
import io.github.dsheirer.source.wave.ComplexWaveSource;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

public class PlaybackController extends HBox implements IFrameLocationListener, Listener<ComplexBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(PlaybackController.class);

    private Button mRewindButton;
    private TextField mPlaybackPositionText;
    private Button mPlay1Button;
    private Button mPlay10Button;
    private Button mPlay100Button;
    private Button mPlay1000Button;
    private HBox mControlsBox;
    private Label mFileLabel;
    private EventHandler<ActionEvent> mPlayEventHandler;
    private DecoderPane mSampleRateListener;

    private IControllableFileSource mControllableFileSource;
    private Broadcaster<ComplexBuffer> mComplexBufferBroadcaster = new Broadcaster<>();

    public PlaybackController()
    {
        super(10);

        getChildren().addAll(getControlsBox(), getFileLabel());

        setControlsEnabled(false);
    }

    public void setSampleRateListener(DecoderPane decoderPane)
    {
        mSampleRateListener = decoderPane;

        if(mSampleRateListener != null && mControllableFileSource != null)
        {
            mSampleRateListener.setSampleRate(mControllableFileSource.getSampleRate());
        }
    }

    public void load(File file)
    {
        mLog.debug("loading:" + file.getAbsolutePath());
        if(file != null && file.isFile())
        {
            try
            {
                mControllableFileSource = new ComplexWaveSource(file);
                mControllableFileSource.setListener(this);
                ((ComplexWaveSource)mControllableFileSource).setListener((Listener<ComplexBuffer>)this);
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
                mLog.error("Error opening file [" + (file != null ? file.getAbsolutePath() : "null") + "]");

                Alert alert = new Alert(Alert.AlertType.ERROR, "There was an error opening the file",
                    ButtonType.OK);
                alert.show();
                return;
            }

            getFileLabel().setText(file.getName());
            setControlsEnabled(true);
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

        setControlsEnabled(false);
    }

    /**
     * Adds listener to receive complex buffers from this playback
     */
    public void addListener(Listener<ComplexBuffer> listener)
    {
        mComplexBufferBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving complex buffers from this playback
     */
    public void removeListener(Listener<ComplexBuffer> listener)
    {
        mComplexBufferBroadcaster.removeListener(listener);
    }

    /**
     * Sets all of the playback controls to the specified enabled state.
     */
    private void setControlsEnabled(boolean enabled)
    {
        getRewindButton().setDisable(!enabled);
        getPlaybackPositionText().setDisable(!enabled);
        getPlay1Button().setDisable(!enabled);
        getPlay10Button().setDisable(!enabled);
        getPlay100Button().setDisable(!enabled);
        getPlay1000Button().setDisable(!enabled);
    }

    private HBox getControlsBox()
    {
        if(mControlsBox == null)
        {
            mControlsBox = new HBox();

            mControlsBox.getChildren().addAll(getRewindButton(), getPlaybackPositionText(), getPlay1Button(),
                getPlay10Button(), getPlay100Button(), getPlay1000Button());
        }

        return mControlsBox;
    }


    private  Button getRewindButton()
    {
        if(mRewindButton == null)
        {
            mRewindButton = new Button();
            mRewindButton.setGraphic(new FontIcon(FontAwesome.FAST_BACKWARD));
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
            mPlay1Button.setGraphic(new FontIcon(FontAwesome.PLAY));
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
            mPlay10Button.setGraphic(new FontIcon(FontAwesome.FAST_FORWARD));
        }

        return mPlay10Button;
    }

    private  Button getPlay100Button()
    {
        if(mPlay100Button == null)
        {
            mPlay100Button = new Button("100");
            mPlay100Button.setUserData(100);
            mPlay100Button.setOnAction(getPlaybackEventHandler());
            mPlay100Button.setGraphic(new FontIcon(FontAwesome.FAST_FORWARD));
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
            mPlay1000Button.setGraphic(new FontIcon(FontAwesome.FAST_FORWARD));
        }

        return mPlay1000Button;
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

    @Override
    public void receive(ComplexBuffer complexBuffer)
    {
        mComplexBufferBroadcaster.broadcast(complexBuffer);
    }
}
