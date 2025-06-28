/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
package io.github.dsheirer.audio.broadcast.icecast;

import com.google.common.base.Joiner;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IcecastMetadata
{
    private final static Logger mLog = LoggerFactory.getLogger(IcecastMetadata.class);

    /**
     * Creates the title for a metadata update
     */
    public static String getTitle(IdentifierCollection identifierCollection, AliasModel AliasModel)
    {
        StringBuilder sb = new StringBuilder();

        if(identifierCollection != null)
        {
            AliasList aliasList = AliasModel.getAliasList(identifierCollection);

            Identifier to = identifierCollection.getIdentifier(IdentifierClass.USER, Form.PATCH_GROUP, Role.TO);

            if(to == null)
            {
                to = identifierCollection.getIdentifier(IdentifierClass.USER, Form.TALKGROUP, Role.TO);
            }

            if(to == null)
            {
                List<Identifier> toIdentifiers = identifierCollection.getIdentifiers(Role.TO);
                if(!toIdentifiers.isEmpty())
                {
                    to = toIdentifiers.get(0);
                }
            }

            if(to != null)
            {
                List<Alias> aliases = aliasList.getAliases(to);

                //Check for 'Stream As Talkgroup' alias and use this instead of the decoded TO value.
                Optional<Alias> streamAs = aliases.stream().filter(alias -> alias.getStreamTalkgroupAlias() != null).findFirst();

                if(streamAs.isPresent())
                {
                    sb.append("TO:").append(streamAs.get().getStreamTalkgroupAlias().getValue());
                }
                else
                {
                    sb.append("TO:").append(to);

                    if(!aliases.isEmpty())
                    {
                        sb.append(" ").append(Joiner.on(", ").skipNulls().join(aliases));
                    }
                }
            }
            else
            {
                sb.append("TO:UNKNOWN");
            }

            Identifier from = identifierCollection.getIdentifier(IdentifierClass.USER, Form.RADIO, Role.FROM);

            if(from == null)
            {
                List<Identifier> fromIdentifiers = identifierCollection.getIdentifiers(Role.FROM);

                if(!fromIdentifiers.isEmpty())
                {
                    from = fromIdentifiers.get(0);
                }
            }

            if(from != null)
            {
                sb.append(" FROM:").append(from);

                List<Alias> aliases = aliasList.getAliases(from);

                if(aliases != null && !aliases.isEmpty())
                {
                    sb.append(" ").append(Joiner.on(", ").skipNulls().join(aliases));
                }
            }
            else
            {
                sb.append(" FROM:UNKNOWN");
            }
        }
        else
        {
            sb.append("Scanning...");
        }

        return sb.toString();
    }

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

}