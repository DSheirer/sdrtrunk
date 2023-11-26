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

package io.github.dsheirer.audio.playbackfx;

import java.util.Arrays;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Audio spectrum view provides visual display of the frequency content of the replay audio.
 */
public class AudioSpectrumView extends VBox implements AudioSpectrumListener
{
    private ObjectProperty<MediaPlayer> mMediaPlayer = new SimpleObjectProperty<>();
    private static final int AUDIO_SPECTRUM_FFT_BANDS = 64;
    private static final int HEIGHT = 64; //64-FFT bins plotted with one pixel per bin
    private Color mColor = Color.YELLOW;
    private PixelFormat mPixelFormat;
    private WritableImage mWritableImage;
    private ImageView mImageView;
    private int mColorIntensity = 10;
    private int mTimeAxisOffset = 0;
    private int mPreviousPlaybackLine;
    private byte[] mPlaybackLine;
    private byte[] mClearedPlaybackLine;

    /**
     * Constructs an instance
     */
    public AudioSpectrumView()
    {
        setHeight(64);
        setBackground(Background.fill(Color.BLACK));
        updateColor();
        getChildren().add(getImageView());

        //Monitor the width property to dynamically resize the writable image
        widthProperty().addListener((observable, oldValue, newWidth) -> {
            mWritableImage = new WritableImage(newWidth.intValue(), HEIGHT);
            mImageView.setImage(mWritableImage);
        });

        //Allow user to click within the spectrum area to seek playback to the click location
        setOnMouseClicked(event -> {
            double requestedPlayback = (mTimeAxisOffset + event.getX()) / 20.0;
            MediaPlayer mediaPlayer = mediaPlayerProperty().get();
            if(mediaPlayer != null)
            {
                mediaPlayer.seek(Duration.seconds(requestedPlayback));
            }
        });

        //Monitor the media player to register and deregister from audio spectrum results and DFT bin sizing.
        mediaPlayerProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue != null)
            {
                oldValue.setAudioSpectrumListener(null);
                reset();
            }

            if(newValue != null)
            {
                //Register to receive audio spectrum FFT bins
                newValue.setAudioSpectrumListener(AudioSpectrumView.this::spectrumDataUpdate);
                //Change the FFT bin setting from default 128 bins
                newValue.setAudioSpectrumNumBands(AUDIO_SPECTRUM_FFT_BANDS);
            }
        });
    }

    /**
     * Resets the width when the parent container is resizing.  Sets the width to 1 so that when new FFT bins come
     * in, the image can be updated to the current width.
     */
    public void resetWidth()
    {
        mWritableImage = new WritableImage(1, HEIGHT);
        mImageView.setImage(mWritableImage);
    }

    /**
     * Media player property.  Bind this property to a media player so that it can register to receive audio spectrum
     * data and control the FFT bins setting.
     */
    public ObjectProperty<MediaPlayer> mediaPlayerProperty()
    {
        return mMediaPlayer;
    }

    /**
     * Resets the spectral view
     */
    public void reset()
    {
        mWritableImage = null;
        getImageView().setImage(getWritableImage());
    }

    /**
     * Image view for displaying the writable image.
     */
    private ImageView getImageView()
    {
        if(mImageView == null)
        {
            mImageView = new ImageView(getWritableImage());
        }

        return mImageView;
    }

    /**
     * Writable image for plotting FFT spectrum results.
     */
    private WritableImage getWritableImage()
    {
        if(mWritableImage == null)
        {
            mWritableImage = new WritableImage(1, HEIGHT); //Initial width ... gets reset once visible.
        }

        return mWritableImage;
    }

    /**
     * Updates the pixel format color array when the base color changes.
     */
    private void updateColor()
    {
        int[] argbColors = new int[256];
        int red = (int)(mColor.getRed() * 255);
        int green = (int)(mColor.getGreen() * 255);
        int blue = (int)(mColor.getBlue() * 255);
        int rgb = ((red & 0xFF) << 16) + ((green & 0xFF) << 8) + (blue & 0xFF);
        for(int alpha = 0; alpha < 256; alpha++)
        {
            int tempAlpha = alpha * mColorIntensity;
            if(tempAlpha > 255)
            {
                tempAlpha = 255;
            }
            argbColors[alpha] = ((tempAlpha & 0xFF) << 24) + rgb;
        }

        mPixelFormat = PixelFormat.createByteIndexedInstance(argbColors);
    }

    /**
     * Implements the audio spectrum listener interface to receive FFT bin results during audio playback.
     *
     * @param timestamp timestamp of the event in seconds.
     * @param duration duration for which the spectrum was computed in seconds.
     * @param magnitudes array containing the non-positive spectrum magnitude in decibels
     * (dB) for each band.
     * The size of the array equals the number of bands and should be considered
     * to be read-only.
     * @param phases array containing the phase in the range
     * [<code>Math.PI</code>,&nbsp;<code>Math.PI</code>] for each band.
     * The size of the array equals the number of bands and should be considered
     * to be read-only.
     */
    @Override
    public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases)
    {
        if(mWritableImage.getWidth() != getWidth())
        {
            mWritableImage = new WritableImage((int)getWidth(), HEIGHT);
            mImageView.setImage(mWritableImage);
        }

        if(mPlaybackLine == null || mPlaybackLine.length != magnitudes.length)
        {
            mPlaybackLine = new byte[magnitudes.length];
            Arrays.fill(mPlaybackLine, (byte)(0xFF & 255));
            mClearedPlaybackLine = new byte[magnitudes.length]; //Default to all zeros
        }

        int timeAxis = (int)Math.floor(timestamp * 10) * 2;

        //Detect when time axis offset is non-zero and playback resets to zero-ish so we can reset.
        if(timeAxis < mTimeAxisOffset)
        {
            mTimeAxisOffset = 0;
        }
        else
        {
            timeAxis -= mTimeAxisOffset;
        }


        //If the playback time exceeds the width of the display, shift the displayed content to the left by 25% and
        //continue rendering.  This will make for an animated left scrolling audio playback.
        if(timeAxis > (getWritableImage().getWidth() - 3))
        {
            int offset = (int)(getWidth() / 4);

            //Shift the current image to the left by the offset
            mWritableImage.getPixelWriter().setPixels(0, 0, (int)(mWritableImage.getWidth() - offset),
                    HEIGHT, mWritableImage.getPixelReader(), offset, 0);

            mWritableImage.getPixelWriter().setPixels((int)mWritableImage.getWidth() - offset, 0, offset, HEIGHT,
                    mPixelFormat, new byte[offset * HEIGHT], 0, 1);

            mTimeAxisOffset += offset;
            timeAxis -= offset;
        }

        final byte[] values = new byte[magnitudes.length];
        for(int x = 0; x < magnitudes.length; x++)
        {
            float pixelValues = magnitudes[x];

            if(pixelValues <= -60f)
            {
                values[magnitudes.length - x - 1] = 0;
            }
            else
            {
                values[magnitudes.length - x - 1] = (byte)((int)((60.0f + pixelValues) / 60.0f * 256.0) & 0xFF);
            }
        }

        //Clear the previous playback line
        if(mPreviousPlaybackLine < getWritableImage().getWidth())
        {
            getWritableImage().getPixelWriter().setPixels(mPreviousPlaybackLine, 0, 1, mClearedPlaybackLine.length,
                    mPixelFormat, mClearedPlaybackLine, 0, 1);
        }
        getWritableImage().getPixelWriter().setPixels(timeAxis++, 0, 1, values.length, mPixelFormat, values, 0, 1);
        getWritableImage().getPixelWriter().setPixels(timeAxis++, 0, 1, values.length, mPixelFormat, values, 0, 1);
        getWritableImage().getPixelWriter().setPixels(timeAxis, 0, 1, mPlaybackLine.length, mPixelFormat, mPlaybackLine, 0, 1);
        mPreviousPlaybackLine = timeAxis;
    }
}
