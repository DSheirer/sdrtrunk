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

package io.github.dsheirer.gui;

import io.github.dsheirer.audio.broadcast.BroadcastStatusPanel;
import io.github.dsheirer.audio.playback.AudioPanel;
import io.github.dsheirer.channel.details.ChannelDetailPanel;
import io.github.dsheirer.channel.metadata.ChannelMetadataPanel;
import io.github.dsheirer.channel.metadata.NowPlayingPanel;
import io.github.dsheirer.controller.ControllerPanel;
import io.github.dsheirer.gui.power.ChannelPowerPanel;
import io.github.dsheirer.module.decode.event.DecodeEventPanel;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerViewPanel;
import io.github.dsheirer.spectrum.FrequencyOverlayPanel;
import io.github.dsheirer.spectrum.OverlayPanel;
import io.github.dsheirer.spectrum.SpectralDisplayPanel;
import io.github.dsheirer.spectrum.SpectrumFrame;
import io.github.dsheirer.spectrum.SpectrumPanel;
import io.github.dsheirer.spectrum.WaterfallPanel;
import java.awt.GraphicsEnvironment;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

/**
 * Spring configuration factory for Swing UI components that require dependency injection (@Resource)
 *
 * Note: all bean creator methods are also annotated as @Lazy so that Spring doesn't auto-instantiate these beans when
 * we're running in a headless environment.
 */
@Configuration
public class SpringSwingBeanFactory
{
    /**
     * Constructs an instance
     */
    public SpringSwingBeanFactory()
    {
    }

    /**
     * Constructs the primary UI instance when running in non-headless mode.
     */
    @Bean
    @Lazy
    public SDRTrunkUI getSDRTrunkUI()
    {
        if(GraphicsEnvironment.isHeadless())
        {
            return null;
        }

        return new SDRTrunkUI();
    }

    /**
     * Constructs a new spectrum frame
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Lazy
    public SpectrumFrame createSpectrumFrame()
    {
        return new SpectrumFrame();
    }

    /**
     * Constructs the spectral display panel
     */
    @Bean
    @Lazy
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SpectralDisplayPanel getSpectralDisplayPanel()
    {
        return new SpectralDisplayPanel();
    }

    /**
     * Constructs the spectrum panel
     *
     * Note: editor is used in multiple places so we create a new instance for each (ie scope=prototype)
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Lazy
    public SpectrumPanel getSpectrumPanel()
    {
        return new SpectrumPanel();
    }

    /**
     * Constructs the waterfall panel
     *
     * Note: editor is used in multiple places so we create a new instance for each (ie scope=prototype)
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Lazy
    public WaterfallPanel getWaterfallPanel()
    {
        return new WaterfallPanel();
    }

    /**
     * Constructs the overlay panel
     *
     * Note: editor is used in multiple places so we create a new instance for each (ie scope=prototype)
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Lazy
    public OverlayPanel getOverlayPanel()
    {
        return new OverlayPanel();
    }

    /**
     * Constructs the frequency overlay panel
     *
     * Note: editor is used in multiple places so we create a new instance for each (ie scope=prototype)
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Lazy
    public FrequencyOverlayPanel getFrequencyOverlayPanel()
    {
        return new FrequencyOverlayPanel();
    }

    /**
     * Creates a broadcast (ie streaming) status panel
     */
    @Bean
    @Lazy
    public BroadcastStatusPanel getBroadcastStatusPanel()
    {
        return new BroadcastStatusPanel();
    }

    /**
     * Creates the controller panel
     */
    @Bean
    @Lazy
    public ControllerPanel controllerPanel()
    {
        return new ControllerPanel();
    }

    /**
     * Creates the now playing panel
     */
    @Bean
    @Lazy
    public NowPlayingPanel getNowPlayingPanel()
    {
        return new NowPlayingPanel();
    }

    /**
     * Creates the channel metadata panel
     */
    @Bean
    @Lazy
    public ChannelMetadataPanel getChannelMetadataPanel()
    {
        return new ChannelMetadataPanel();
    }

    /**
     * Creates the channel detail panel
     */
    @Bean
    @Lazy
    public ChannelDetailPanel getChannelDetailPanel()
    {
        return new ChannelDetailPanel();
    }

    /**
     * Creates the channel power panel
     */
    @Bean
    @Lazy
    public ChannelPowerPanel getChannelPowerPanel()
    {
        return new ChannelPowerPanel();
    }

    /**
     * Creates the decode event panel
     */
    @Bean
    @Lazy
    public DecodeEventPanel getDecodeEventPanel()
    {
        return new DecodeEventPanel();
    }

    /**
     * Creates an audio panel
     */
    @Bean
    @Lazy
    public AudioPanel getAudioPanel()
    {
        return new AudioPanel();
    }

    /**
     * Creates discovered tuner editor
     */
    @Bean
    @Lazy
    public DiscoveredTunerEditor getDiscoveredTunerEditor()
    {
        return new DiscoveredTunerEditor();
    }

    /**
     * Creates tuner view panel
     */
    @Bean
    @Lazy
    public TunerViewPanel getTunerViewPanel()
    {
        return new TunerViewPanel();
    }
}
