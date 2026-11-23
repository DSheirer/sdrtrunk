/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.radioreference;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AccessCodeFilter;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.dmr.DecodeConfigDMR;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.decode.nxdn.DecodeConfigNXDN;
import io.github.dsheirer.module.decode.nxdn.layer3.type.TransmissionMode;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25Phase1;
import io.github.dsheirer.module.decode.p25.phase1.Modulation;
import io.github.dsheirer.module.decode.squelch.SquelchDecoderConfig;
import io.github.dsheirer.rrapi.type.Site;
import io.github.dsheirer.rrapi.type.SiteFrequency;
import io.github.dsheirer.rrapi.type.System;
import io.github.dsheirer.rrapi.type.SystemInformation;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates playlist channels from radio reference frequencies and trunked system sites.
 */
public final class RadioReferenceChannelFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(RadioReferenceChannelFactory.class);

    /**
     * P25 NACs that accept any NAC ($F7E receive, $F7F repeater) and are never used as a filter.
     */
    private static final int NAC_RECEIVE_ANY = 0xF7E;
    private static final int NAC_REPEAT_ANY = 0xF7F;

    private RadioReferenceChannelFactory()
    {
    }

    /**
     * Creates a conventional channel for a radio reference frequency
     * @param modeDecoderType of the frequency's radio reference mode
     * @param frequency in Hertz
     * @param system name for the channel
     * @param site name for the channel
     * @param name of the channel
     * @return channel, or null when sdrtrunk has no decoder for the mode
     */
    public static Channel conventional(ModeDecoderType modeDecoderType, long frequency, String system, String site,
                                       String name)
    {
        return conventional(modeDecoderType, frequency, system, site, name, null);
    }

    /**
     * Creates a conventional channel for a radio reference frequency, with the frequency's tone applied: a CTCSS or
     * DCS tone enables tone squelch on an analog FM channel.
     * @param modeDecoderType of the frequency's radio reference mode
     * @param frequency in Hertz
     * @param system name for the channel
     * @param site name for the channel
     * @param name of the channel
     * @param tone field of the radio reference frequency, may be null
     * @return channel, or null when sdrtrunk has no decoder for the mode
     */
    public static Channel conventional(ModeDecoderType modeDecoderType, long frequency, String system, String site,
                                       String name, String tone)
    {
        if(modeDecoderType.hasDecoderType())
        {
            Channel channel = new Channel();
            channel.setSystem(system);
            channel.setSite(site);
            channel.setName(name);
            SourceConfigTuner sourceConfigTuner = new SourceConfigTuner();
            sourceConfigTuner.setFrequency(frequency);
            channel.setSourceConfiguration(sourceConfigTuner);
            DecodeConfiguration decodeConfiguration = DecoderFactory.getDecodeConfiguration(modeDecoderType.getDecoderType());

            if(decodeConfiguration instanceof DecodeConfigNBFM && modeDecoderType == ModeDecoderType.FM)
            {
                ((DecodeConfigNBFM)decodeConfiguration).setBandwidth(DecodeConfigNBFM.Bandwidth.BW_25_0);
            }
            else if(decodeConfiguration instanceof DecodeConfigP25Phase1)
            {
                ((DecodeConfigP25Phase1)decodeConfiguration).setModulation(Modulation.C4FM);
            }
            else if(decodeConfiguration instanceof DecodeConfigNXDN nxdn)
            {
                if(modeDecoderType == ModeDecoderType.NXDN48)
                {
                    nxdn.setTransmissionMode(TransmissionMode.M4800);
                }
                else if(modeDecoderType == ModeDecoderType.NXDN96)
                {
                    nxdn.setTransmissionMode(TransmissionMode.M9600);
                }
            }

            applyTone(decodeConfiguration, tone, frequency, name);
            channel.setDecodeConfiguration(decodeConfiguration);
            return channel;
        }

        mLog.warn("Can't create channel configuration for [" + modeDecoderType.name() + "] no supported decoder type");
        return null;
    }

    /**
     * Applies a radio reference tone to the decode configuration when the tone applies to the channel's protocol: a
     * CTCSS or DCS tone enables tone squelch on an analog FM channel, and a NAC (P25), color code (DMR) or RAN (NXDN)
     * enables the channel's access code filter to include only that code.  Tones that don't apply, or that aren't
     * recognized, are ignored.
     */
    static void applyTone(DecodeConfiguration decodeConfiguration, String tone, long frequency, String name)
    {
        RadioReferenceTone parsed = RadioReferenceTone.parse(tone);

        if(parsed == null)
        {
            if(tone != null && !tone.isBlank() && !tone.trim().equalsIgnoreCase("CSQ"))
            {
                mLog.info("Radio Reference import [" + name + "] at [" + frequency + "] - tone [" + tone +
                    "] not recognized - not applied");
            }

            return;
        }

        if(decodeConfiguration instanceof DecodeConfigP25Phase1 && parsed.type() == RadioReferenceTone.Type.NAC &&
            (parsed.accessCode() == NAC_RECEIVE_ANY || parsed.accessCode() == NAC_REPEAT_ANY))
        {
            mLog.info("Radio Reference import [" + name + "] at [" + frequency + "] - NAC [" + parsed +
                "] accepts any NAC - no filter applied");
        }
        else if(decodeConfiguration instanceof DecodeConfigP25Phase1 p25 && parsed.type() == RadioReferenceTone.Type.NAC)
        {
            p25.setNacFilter(includeFilter(parsed.accessCode(), AccessCodeFilter.Format.HEX));
            mLog.info("Radio Reference import [" + name + "] at [" + frequency + "] - NAC filter [" + parsed +
                "] applied");
        }
        else if(decodeConfiguration instanceof DecodeConfigDMR dmr && parsed.type() == RadioReferenceTone.Type.COLOR_CODE)
        {
            dmr.setColorCodeFilter(includeFilter(parsed.accessCode(), AccessCodeFilter.Format.DECIMAL));
            mLog.info("Radio Reference import [" + name + "] at [" + frequency + "] - color code filter [" + parsed +
                "] applied");
        }
        else if(decodeConfiguration instanceof DecodeConfigNXDN nxdn && parsed.type() == RadioReferenceTone.Type.RAN)
        {
            nxdn.setRanFilter(includeFilter(parsed.accessCode(), AccessCodeFilter.Format.DECIMAL));
            mLog.info("Radio Reference import [" + name + "] at [" + frequency + "] - RAN filter [" + parsed +
                "] applied");
        }
        else if(decodeConfiguration instanceof DecodeConfigNBFM nbfm)
        {
            SquelchDecoderConfig squelch = switch(parsed.type())
            {
                case CTCSS -> new SquelchDecoderConfig(SquelchDecoderConfig.SquelchType.CTCSS,
                    parsed.ctcssCode().name());
                case DCS -> new SquelchDecoderConfig(SquelchDecoderConfig.SquelchType.DCS, parsed.dcsCode().name());
                default -> null;
            };

            if(squelch != null)
            {
                List<SquelchDecoderConfig> squelchDecoders = new ArrayList<>();
                squelchDecoders.add(squelch);
                nbfm.setSquelchDecoders(squelchDecoders);
                mLog.info("Radio Reference import [" + name + "] at [" + frequency + "] - tone squelch [" + parsed +
                    "] applied");
            }
        }
    }

    /**
     * Enabled access code filter that includes only the code.
     */
    private static AccessCodeFilter includeFilter(int code, AccessCodeFilter.Format format)
    {
        AccessCodeFilter filter = new AccessCodeFilter(format);
        filter.setMode(AccessCodeFilter.Mode.INCLUDE);
        filter.setValueSet(List.of(code));
        filter.setEnabled(true);
        return filter;
    }

    /**
     * Decoder type for a trunked system site's control channel.  A P25 Phase II site with an FDMA control channel
     * uses the Phase 1 decoder, which follows the Phase II traffic channels.
     * @return decoder type, or null when sdrtrunk can't decode the site's control channel
     */
    public static DecoderType getControlDecoderType(RadioReferenceDecoder decoder, System system, Site site)
    {
        //Motorola Type II control channels with P25 voice channels aren't supported
        if(decoder.isHybridMotorolaP25(system))
        {
            return null;
        }

        DecoderType decoderType = decoder.getDecoderType(system, site);

        if(decoderType == DecoderType.P25_PHASE2 && (site == null || site.getTdmaControlChannel() == 0))
        {
            return DecoderType.P25_PHASE1;
        }

        return decoderType;
    }

    /**
     * Control and alternate control channel frequencies of the site, control channels first
     */
    public static List<Long> getControlFrequencies(Site site)
    {
        List<Long> frequencies = new ArrayList<>();

        if(site != null && site.getSiteFrequencies() != null)
        {
            for(SiteFrequency siteFrequency: site.getSiteFrequencies())
            {
                if(SiteEditor.isControl(siteFrequency) && siteFrequency.getFrequencyHz() > 0)
                {
                    frequencies.add(siteFrequency.getFrequencyHz());
                }
            }

            for(SiteFrequency siteFrequency: site.getSiteFrequencies())
            {
                if(SiteEditor.isAlternate(siteFrequency) && siteFrequency.getFrequencyHz() > 0)
                {
                    frequencies.add(siteFrequency.getFrequencyHz());
                }
            }
        }

        return frequencies;
    }

    /**
     * Creates a channel for each of the site's repeaters on the frequencies, for systems without a control channel
     * (LTR), where each repeater carries its own trunking data.  The channels are named by logical channel number,
     * like the playlist editor's site editor does.
     * @param decoder for the radio reference system details
     * @param system of the site
     * @param systemInformation of the system
     * @param site of the repeaters
     * @param frequencies of the repeaters to create channels for
     * @return channels, or an empty list when sdrtrunk can't decode the system
     */
    public static List<Channel> repeaters(RadioReferenceDecoder decoder, System system,
                                          SystemInformation systemInformation, Site site, List<Long> frequencies)
    {
        List<Channel> channels = new ArrayList<>();
        DecoderType decoderType = decoder.getDecoderType(system, site);

        if(decoderType == null || site == null || site.getSiteFrequencies() == null)
        {
            return channels;
        }

        for(SiteFrequency siteFrequency: site.getSiteFrequencies())
        {
            if(frequencies.contains(siteFrequency.getFrequencyHz()))
            {
                Channel channel = new Channel();
                channel.setSystem(system.getName());
                channel.setSite(site.getDescription() != null && !site.getDescription().isBlank() ?
                    site.getDescription().trim() : "Site " + site.getSiteNumber());
                channel.setName("LCN " + siteFrequency.getLogicalChannelNumber());
                channel.setDecodeConfiguration(decoder.getDecodeConfiguration(decoderType, site, systemInformation));
                SourceConfigTuner sourceConfigTuner = new SourceConfigTuner();
                sourceConfigTuner.setFrequency(siteFrequency.getFrequencyHz());
                channel.setSourceConfiguration(sourceConfigTuner);
                channels.add(channel);
            }
        }

        return channels;
    }

    /**
     * Creates a channel for a trunked system site: the site's control channel decoder, tuned to its control and
     * alternate control channels.
     * @param decoder for the radio reference system details
     * @param system of the site
     * @param systemInformation of the system
     * @param site to create a channel for
     * @return channel, or null when sdrtrunk can't decode the site's control channel or it has none listed
     */
    public static Channel trunkedSite(RadioReferenceDecoder decoder, System system, SystemInformation systemInformation,
                                      Site site)
    {
        return trunkedSite(decoder, system, systemInformation, site, null);
    }

    /**
     * Creates a channel for a trunked system site: the site's control channel decoder, tuned to its control and
     * alternate control channels, starting with the first frequency when it's one of them.
     * @param decoder for the radio reference system details
     * @param system of the site
     * @param systemInformation of the system
     * @param site to create a channel for
     * @param firstFrequency to list first, so the channel starts on it, or null for control channels first
     * @return channel, or null when sdrtrunk can't decode the site's control channel or it has none listed
     */
    public static Channel trunkedSite(RadioReferenceDecoder decoder, System system, SystemInformation systemInformation,
                                      Site site, Long firstFrequency)
    {
        DecoderType decoderType = getControlDecoderType(decoder, system, site);
        List<Long> frequencies = getControlFrequencies(site);

        //A multi-frequency channel starts on its first frequency
        if(firstFrequency != null && frequencies.remove(firstFrequency))
        {
            frequencies.add(0, firstFrequency);
        }

        if(decoderType == null || frequencies.isEmpty())
        {
            return null;
        }

        Channel channel = new Channel();
        channel.setSystem(system.getName());
        String siteName = "Site " + site.getSiteNumber();
        channel.setSite(site.getDescription() != null && !site.getDescription().isBlank() ?
            site.getDescription().trim() : siteName);
        channel.setName(siteName);
        channel.setDecodeConfiguration(decoder.getDecodeConfiguration(decoderType, site, systemInformation));

        if(frequencies.size() == 1)
        {
            SourceConfigTuner sourceConfigTuner = new SourceConfigTuner();
            sourceConfigTuner.setFrequency(frequencies.get(0));
            channel.setSourceConfiguration(sourceConfigTuner);
        }
        else
        {
            SourceConfigTunerMultipleFrequency sourceConfig = new SourceConfigTunerMultipleFrequency();

            for(long frequency: frequencies)
            {
                sourceConfig.addFrequency(frequency);
            }

            channel.setSourceConfiguration(sourceConfig);
        }

        return channel;
    }
}
