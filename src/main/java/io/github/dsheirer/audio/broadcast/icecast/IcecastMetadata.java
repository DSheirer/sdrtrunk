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
package io.github.dsheirer.audio.broadcast.icecast;

import com.google.common.base.Joiner;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.icecast.IcecastConfiguration;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.StringLoader;
import io.pebbletemplates.pebble.template.PebbleTemplate;

public class IcecastMetadata
{
    private final static Logger mLog = LoggerFactory.getLogger(IcecastMetadata.class);
    private AliasModel mAliasModel;
    private IcecastConfiguration mIcecastConfiguration;
    private PebbleEngine mEngine;

    public IcecastMetadata(IcecastConfiguration icecastConfiguration, AliasModel aliasModel)
    {
        mAliasModel = aliasModel;
        mIcecastConfiguration = icecastConfiguration;
        mEngine = new PebbleEngine.Builder().loader(new StringLoader()).autoEscaping(false).build();
    }

    /**
     * Creates the title for a metadata update
     * @param identifierCollection object
     * @param time as ms since epoch
     */
    public String getTitle(IdentifierCollection identifierCollection, long time)
    {

        if(identifierCollection != null)
        {
            Map<String, Object> params = new HashMap<>();

            AliasList aliasList = mAliasModel.getAliasList(identifierCollection);

            List<String> from = new ArrayList<>();
            List<String> to = identifierCollection.getToIdentifiers().stream().map(i -> formatIdentifier(i, aliasList)).toList();
            List<String> tone = new ArrayList<>();
            List<String> site = identifierCollection.getIdentifiers(Form.SITE).stream().map(i -> i.toString()).toList();
            List<String> system = identifierCollection.getIdentifiers(Form.SYSTEM).stream().map(i -> i.toString()).toList();

            for(Identifier identifier: identifierCollection.getFromIdentifiers())
            {
                switch(identifier.getForm())
                {
                    case TONE:
                        tone.add(formatIdentifier(identifier, aliasList));
                        break;
                    default:
                        from.add(formatIdentifier(identifier, aliasList));
                }
            }

            params.put("FROM", Joiner.on("; ").skipNulls().join(from).trim());
            params.put("TIME", formatTime(mIcecastConfiguration.getMetadataTimeFormat(), time));
            params.put("TO", Joiner.on("; ").skipNulls().join(to).trim());
            params.put("TONE", Joiner.on("; ").skipNulls().join(tone).trim());
            params.put("SITE", Joiner.on("; ").skipNulls().join(site).trim());
            params.put("SYSTEM", Joiner.on("; ").skipNulls().join(system).trim());

            return renderTemplate(mIcecastConfiguration.getMetadataFormat(), params);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("TIME", formatTime(mIcecastConfiguration.getMetadataTimeFormat(), System.currentTimeMillis()));
        return renderTemplate(mIcecastConfiguration.getMetadataIdleMessage(), params);
    }

    /**
     * @param title
     * @return title formatted as inline icecast metadata
     */
    public static String formatInline(String title)
    {
        title = "StreamTitle='" + title + "';";
        int chunks = (int)Math.ceil((title.length() + 1) / 16.0d);
        int nulls = (chunks * 16) - title.length();
        char[] padding = new char[nulls];

        StringBuilder sb = new StringBuilder();
        sb.append((char)chunks).append(title).append(padding);
        return sb.toString();
    }

    /**
     * @param identifier object
     * @param aliasList object
     * @return identifier formatted per configuration
     */
    private String formatIdentifier(Identifier identifier, AliasList aliasList)
    {
        Map<String, Object> params = new HashMap<>();

        params.put("ID", identifier.toString());

        List<Alias> aliases = aliasList.getAliases(identifier);

        if(aliases != null && !aliases.isEmpty())
        {
            params.put("ALIAS", Joiner.on(", ").skipNulls().join(aliases).trim());
            params.put("ALIAS_LIST", Joiner.on(", ").skipNulls().join(aliases.stream().map(alias -> alias.getAliasListName()).toArray()).trim());
            params.put("GROUP", Joiner.on(", ").skipNulls().join(aliases.stream().map(alias -> alias.getGroup()).toArray()).trim());
        }

        if((!mIcecastConfiguration.getMetadataTalkgroupFormat().isBlank())&&((identifier.getForm() == Form.PATCH_GROUP)||(identifier.getForm() == Form.TALKGROUP)))
        {
            return renderTemplate(mIcecastConfiguration.getMetadataTalkgroupFormat(), params);
        }
        else if((!mIcecastConfiguration.getMetadataRadioFormat().isBlank())&&(identifier.getForm() == Form.RADIO))
        {
            return renderTemplate(mIcecastConfiguration.getMetadataRadioFormat(), params);
        }
        else if((!mIcecastConfiguration.getMetadataToneFormat().isBlank())&&(identifier.getForm() == Form.TONE))
        {
            return renderTemplate(mIcecastConfiguration.getMetadataToneFormat(), params);
        }

        return renderTemplate(mIcecastConfiguration.getMetadataDefaultFormat(), params);
    }

    /**
     * @param format to use with SimpleDataFormat
     * @param time as ms since epoch
     * @return formatted time as a string
     */
    private String formatTime(String format, long time)
    {
        try {
            return new SimpleDateFormat(format).format(new Date(time));
        }
        catch (IllegalArgumentException e)
        {
            mLog.warn("Invalid metadata time format: " + format);
        }
        return "";
    }

    /**
     * @param templateString Pebble template as a string
     * @param params to use for replacement
     * @return rendered template as a string
     */
    private String renderTemplate(String templateString, Map<String, Object> params)
    {
        try
        {
            PebbleTemplate template = mEngine.getTemplate(templateString);
            Writer writer = new StringWriter();
            template.evaluate(writer, params);
            return writer.toString();
        }
        catch (io.pebbletemplates.pebble.error.PebbleException pe)
        {
            mLog.warn("Invalid metadata format: " + pe.getMessage());
        }
        catch (IOException e)
        {
            mLog.warn("Error processing metadata template: ", e);
        }

        return "";
    }

}