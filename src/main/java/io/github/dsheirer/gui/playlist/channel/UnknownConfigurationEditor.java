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

package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.config.RecordConfiguration;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import javafx.scene.control.TitledPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unknown protocol channel configuration editor
 */
public class UnknownConfigurationEditor extends ChannelConfigurationEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(UnknownConfigurationEditor.class);
    private TitledPane mDecoderPane;

    /**
     * Constructs an instance
     * @param aliasModel
     */
    public UnknownConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager,
                                      UserPreferences userPreferences, IFilterProcessor filterProcessor)
    {
        super(playlistManager, tunerManager, userPreferences, filterProcessor);
        getTitledPanesBox().getChildren().add(getDecoderPane());
    }

    private TitledPane getDecoderPane()
    {
        if(mDecoderPane == null)
        {
            mDecoderPane = new TitledPane();
            mDecoderPane.setText("Decoder: Unknown Protocol");
            mDecoderPane.setExpanded(false);
            mDecoderPane.setDisable(true);
        }

        return mDecoderPane;
    }

    @Override
    public DecoderType getDecoderType()
    {
        return null;
    }

    @Override
    protected void setDecoderConfiguration(DecodeConfiguration config)
    {
    }

    @Override
    protected void saveDecoderConfiguration()
    {
    }

    @Override
    protected void setEventLogConfiguration(EventLogConfiguration config)
    {
    }

    @Override
    protected void saveEventLogConfiguration()
    {
    }

    @Override
    protected void setAuxDecoderConfiguration(AuxDecodeConfiguration config)
    {
    }

    @Override
    protected void saveAuxDecoderConfiguration()
    {
    }

    @Override
    protected void setRecordConfiguration(RecordConfiguration config)
    {
    }

    @Override
    protected void saveRecordConfiguration()
    {
        //Audio recording is handled in the NBFM decoder config
    }

    @Override
    protected void setSourceConfiguration(SourceConfiguration config)
    {
    }

    @Override
    protected void saveSourceConfiguration()
    {
    }
}
