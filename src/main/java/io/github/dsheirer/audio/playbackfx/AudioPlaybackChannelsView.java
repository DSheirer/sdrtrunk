/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.audio.playbackfx;

import io.github.dsheirer.preference.IPreferenceUpdateListener;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.mixer.MixerChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Audio playback view for one or more audio playback channel views and controllers.
 */
public class AudioPlaybackChannelsView extends HBox implements IPreferenceUpdateListener
{
    @Resource
    private AudioPlaybackController mAudioPlaybackController;
    @Resource
    private UserPreferences mUserPreferences;
    private List<AudioPlaybackChannelView> mAudioPlaybackChannelViews = new ArrayList<>();
    private GridPane mGridPane;
    private PlaybackView mPlaybackView;
    private IPanelRevalidationRequestListener mListener;

    /**
     * Constructs an instance
     */
    public AudioPlaybackChannelsView()
    {
        setPadding(new Insets(0, 3, 0, 2));
        setSpacing(3);
        setMaxWidth(Double.MAX_VALUE);
        //Force the audio spectrum view's image view to (temporarily) shrink to 1 so that we can resize dynamically.
        widthProperty().addListener((observable, oldValue, newValue) -> resetWidth());
    }

    /**
     * Registers a listener to receive requests from this view for Swing revalidation
     * @param listener to receive request.
     */
    public void setPanelRevalidationRequestListener(IPanelRevalidationRequestListener listener)
    {
        mListener = listener;
    }

    /**
     * Implements preference type update listener to be notified if the user changes the playback view preference.
     * @param preferenceType that was updated
     */
    @Override
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.PLAYBACK)
        {
            PlaybackView preferred = mUserPreferences.getPlaybackPreference().getPlaybackView();

            if(mPlaybackView != preferred)
            {
                mPlaybackView = preferred;
                updatePlaybackViews();

                //Since this view is embedded in a JFXPanel, the parent has to nullify the JFXPanel's scene and reset it.
                if(mListener != null)
                {
                    mListener.revalidatePanel();
                }
            }
        }
    }

    /**
     * Resets the audio spectrum view width during resizing.
     */
    public void resetWidth()
    {
        for(AudioPlaybackChannelView view: mAudioPlaybackChannelViews)
        {
            view.resetWidth();
        }
    }

    private void updatePlaybackViews()
    {
        if(!mAudioPlaybackChannelViews.isEmpty())
        {
            for(AudioPlaybackChannelView view: mAudioPlaybackChannelViews)
            {
                getGridPane().getChildren().remove(view);
            }

            mAudioPlaybackChannelViews.clear();
        }

        AudioPlaybackChannelController left = mAudioPlaybackController.getChannelController(MixerChannel.LEFT);
        AudioPlaybackChannelView leftView = switch(mPlaybackView)
        {
            case MINIMAL -> new AudioPlaybackChannelViewMinimal(left);
            case STANDARD -> new AudioPlaybackChannelViewStandard(left);
            default -> new AudioPlaybackChannelViewComplete(left);
        };

        getGridPane().add(leftView, 0, 0);
        mAudioPlaybackChannelViews.add(leftView);

        AudioPlaybackChannelController right = mAudioPlaybackController.getChannelController(MixerChannel.RIGHT);
        AudioPlaybackChannelView rightView = switch(mPlaybackView)
        {
            case MINIMAL -> new AudioPlaybackChannelViewMinimal(right);
            case STANDARD -> new AudioPlaybackChannelViewStandard(right);
            default -> new AudioPlaybackChannelViewComplete(right);
        };
        getGridPane().add(rightView, 1, 0);
        mAudioPlaybackChannelViews.add(rightView);
    }

    /**
     * Post instantiation/startup steps.
     */
    @PostConstruct
    public void postConstruct()
    {
        mPlaybackView = mUserPreferences.getPlaybackPreference().getPlaybackView();

        //Register for notifications of user changes to the audio playback view style
        mUserPreferences.addUpdateListener(this);

        HBox.setHgrow(getGridPane(), Priority.ALWAYS);
        updatePlaybackViews();
        getChildren().add(getGridPane());
    }

    private GridPane getGridPane()
    {
        if(mGridPane == null)
        {
            mGridPane = new GridPane();
            mGridPane.setMaxWidth(Double.MAX_VALUE);
            mGridPane.setHgap(3);

            //Add two columns and give them each 50% of the width
            ColumnConstraints ccLeft = new ColumnConstraints();
            ccLeft.setPercentWidth(100.0 / 2);
            ccLeft.setHgrow(Priority.ALWAYS);
            mGridPane.getColumnConstraints().add(ccLeft);

            ColumnConstraints ccRight = new ColumnConstraints();
            ccRight.setPercentWidth(100.0 / 2);
            ccRight.setHgrow(Priority.ALWAYS);
            mGridPane.getColumnConstraints().add(ccRight);
        }

        return mGridPane;
    }
}
