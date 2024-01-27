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
package io.github.dsheirer.gui;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelException;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.log.ApplicationLog;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.record.AudioRecordingManager;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"io.github.dsheirer"})
@EnableJpaRepositories("io.github.dsheirer.audio.call")
@EntityScan("io.github.dsheirer.audio.call")
public class SDRTrunk
{
    private final static Logger mLog = LoggerFactory.getLogger(SDRTrunk.class);
    @Resource
    private ApplicationLog mApplicationLog;
    @Resource
    private AudioRecordingManager mAudioRecordingManager;
    @Resource
    private ChannelModel mChannelModel;
    @Resource
    private ChannelProcessingManager mChannelProcessingManager;
    @Resource
    private TunerManager mTunerManager;
    @Resource
    private UserPreferences mUserPreferences;
    @Resource //SpringUiFactory does not instantiate this instance when headless=true
    private SDRTrunkUI mSDRTrunkUI;

    /**
     * Constructs an instance of the SDRTrunk application
     */
    public SDRTrunk()
    {
    }


    @PostConstruct
    public void postConstruct()
    {
        ThreadPool.logSettings();

        //Log current properties setting
        SystemProperties.getInstance().logCurrentSettings();

        if(GraphicsEnvironment.isHeadless())
        {
            mLog.info("starting main application in headless mode");
            autoStartChannels();
        }
        else
        {
            mLog.info("starting main application with gui");
            if(mSDRTrunkUI != null)
            {
                EventQueue.invokeLater(() -> mSDRTrunkUI.setVisible(true));
            }
            else
            {
                mLog.error("SDRTrunk user interface is null - can't start UI");
            }
        }
    }

    /**
     * Starts processing any channel configurations that are designated to auto-start
     */
    private void autoStartChannels()
    {
        List<Channel> channels = mChannelModel.getAutoStartChannels();
        for(Channel channel: channels)
        {
            try
            {
                mLog.info("Auto-starting channel " + channel.getName());
                mChannelProcessingManager.start(channel);
            }
            catch(ChannelException ce)
            {
                mLog.error("Channel: " + channel.getName() + " auto-start failed: " + ce.getMessage());
            }
        }
    }

    /**
     * Performs shutdown operations
     */
    @PreDestroy
    public void preDestroy()
    {
        mLog.info("Application shutdown started ...");
        mLog.info("Stopping channels ...");
        mChannelProcessingManager.shutdown();
        mAudioRecordingManager.stop();
        mLog.info("Stopping tuners ...");
        mTunerManager.stop();
        mLog.info("Shutdown complete.");
    }

    /**
     * Monitors the SDRTrunkUI window for shutdown so that we can terminate the overall SDRTrunk application.
     */
    public class ShutdownMonitor extends WindowAdapter
    {
        @Override
        public void windowClosing(WindowEvent e)
        {
            preDestroy();
        }
    }

    /**
     * Launch the application.
     */
    public static void main(String[] args)
    {
        boolean headless = GraphicsEnvironment.isHeadless();

        //Set the user's preferred location for the database prior to starting the application
//        String dbpath = new UserPreferences().getDirectoryPreference().getDirectoryApplicationRoot().toString();
        String dbpath = new UserPreferences().getDirectoryPreference().getDirectoryDatabase().getParent().toString();
        System.setProperty("derby.system.home", dbpath);

        ConfigurableApplicationContext context = new SpringApplicationBuilder(SDRTrunk.class)
                .bannerMode(Banner.Mode.OFF)
                .headless(headless)
                .web(WebApplicationType.NONE)
                .registerShutdownHook(true)
                .run(args);
    }
}
