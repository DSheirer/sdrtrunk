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

import io.github.dsheirer.gui.instrument.decoder.AbstractDecoderPane;
import io.github.dsheirer.gui.instrument.decoder.DecoderPaneFactory;
import io.github.dsheirer.gui.instrument.decoder.RealDecoderPane;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.p25.phase1.P25P1Decoder;
import javafx.scene.layout.BorderPane;

import java.io.File;

public class ViewerDesktop extends BorderPane
{
    private PlaybackController mPlaybackController;
    private AbstractDecoderPane mDecoderPane = DecoderPaneFactory.getDefaultPane();

    public ViewerDesktop()
    {
        setBottom(getPlaybackController());
        setCenter(getDecoderPane());
    }

    public void load(File file)
    {
        close();
        getPlaybackController().load(file);
    }

    public void setDecoder(DecoderType decoderType)
    {
        setDecoderPane(DecoderPaneFactory.getDecoderPane(decoderType));
    }

    public void setP25Phase1Decoder(P25P1Decoder.Modulation modulation)
    {
        setDecoderPane(DecoderPaneFactory.getP25P1DecoderPane(modulation));
    }

    private void setDecoderPane(AbstractDecoderPane decoderPane)
    {
        if(getDecoderPane() instanceof RealDecoderPane)
        {
            getPlaybackController().removeRealListener(getDecoderPane());
        }
        else
        {
            getPlaybackController().removeComplexListener(getDecoderPane());
        }
        getChildren().remove(getDecoderPane());
        mDecoderPane = decoderPane;
        setCenter(getDecoderPane());

        if(getDecoderPane() instanceof RealDecoderPane)
        {
            getPlaybackController().addRealListener(getDecoderPane());
        }
        else
        {
            getPlaybackController().addComplexListener(getDecoderPane());
        }
        getPlaybackController().setSampleRateListener(getDecoderPane());
    }

    /**
     * Decoder pane
     */
    public AbstractDecoderPane getDecoderPane()
    {
        if(mDecoderPane == null)
        {
            mDecoderPane = DecoderPaneFactory.getDefaultPane();
        }

        return mDecoderPane;
    }

    public void close()
    {
        getPlaybackController().close();
    }

    private PlaybackController getPlaybackController()
    {
        if(mPlaybackController == null)
        {
            mPlaybackController = new PlaybackController();
        }

        return mPlaybackController;
    }
}
