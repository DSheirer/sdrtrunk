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
package io.github.dsheirer.database;

import io.github.dsheirer.database.call.CallSearchCriteria;
import io.github.dsheirer.database.call.IdentifierFilter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Portable SQL helpers shared by the calls and events repositories.
 */
public final class SqlSupport
{
    private SqlSupport()
    {
    }

    /**
     * WHERE clause for a TO or FROM identifier filter.
     * @param prefix of the identifier columns (to or from)
     * @param filter to apply
     * @param formClause that restricts an alias filter to the identifier forms that the alias identifiers apply to
     * @param patchedTable to also match the talkgroups patched into a patch group (TO filter), or null
     * @param patchedKey column of the patched table that references the id of the outer row (alias c)
     * @param parameters to add the parameter values to
     */
    public static String getIdentifierClause(String prefix, IdentifierFilter filter, String formClause,
                                              String patchedTable, String patchedKey, List<Object> parameters)
    {
        String valueColumn = prefix + "_value";
        String fqColumn = prefix + "_fq";
        String patched = patchedTable != null ? "EXISTS (SELECT 1 FROM " + patchedTable + " p WHERE p." + patchedKey + " = c.id AND " :
            null;

        if(filter.isAlias())
        {
            List<String> matches = new ArrayList<>();
            List<Object> matchParameters = new ArrayList<>();

            for(IdentifierFilter.ValueRange range: filter.ranges())
            {
                matches.add("(" + fqColumn + " IS NULL AND protocol = ? AND " + valueColumn + " BETWEEN ? AND ?)");
                matchParameters.add(range.protocol().name());
                matchParameters.add((long)range.min());
                matchParameters.add((long)range.max());

                if(patched != null)
                {
                    matches.add("(protocol = ? AND " + patched + "p.fq IS NULL AND p.talkgroup BETWEEN ? AND ?))");
                    matchParameters.add(range.protocol().name());
                    matchParameters.add((long)range.min());
                    matchParameters.add((long)range.max());
                }
            }

            for(IdentifierFilter.FullyQualifiedAddress address: filter.fullyQualified())
            {
                matches.add("(protocol = ? AND " + fqColumn + " = ?)");
                matchParameters.add(address.protocol().name());
                matchParameters.add(address.address());

                if(patched != null)
                {
                    matches.add("(protocol = ? AND " + patched + "p.fq = ?))");
                    matchParameters.add(address.protocol().name());
                    matchParameters.add(address.address());
                }
            }

            if(matches.isEmpty())
            {
                return "1 = 0";
            }

            parameters.add(filter.aliasList() != null ? filter.aliasList().toLowerCase(Locale.ROOT) : "");
            parameters.addAll(matchParameters);
            return "(LOWER(alias_list) = ? AND " + formClause + " AND (" + String.join(" OR ", matches) + "))";
        }

        if(filter.isExactValue())
        {
            parameters.add(filter.exactValue());

            if(patched != null)
            {
                parameters.add(filter.exactValue());
                return "(" + valueColumn + " = ? OR " + patched + "p.talkgroup = ?))";
            }

            return valueColumn + " = ?";
        }

        Long value = CallSearchCriteria.parseValue(filter.text());
        parameters.add(getLikePattern(filter.text()));

        if(value != null)
        {
            parameters.add(value);

            if(patched != null)
            {
                parameters.add(value);
                return "(LOWER(" + prefix + "_id) LIKE ? ESCAPE '!' OR " + valueColumn + " = ? OR " + patched +
                    "p.talkgroup = ?))";
            }

            return "(LOWER(" + prefix + "_id) LIKE ? ESCAPE '!' OR " + valueColumn + " = ?)";
        }

        return "LOWER(" + prefix + "_id) LIKE ? ESCAPE '!'";
    }

    /**
     * Case-insensitive 'contains' LIKE pattern with the LIKE wildcard characters escaped.
     */
    public static String getLikePattern(String text)
    {
        String escaped = text.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }

    public static void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException
    {
        for(int x = 0; x < parameters.size(); x++)
        {
            Object parameter = parameters.get(x);

            if(parameter instanceof String s)
            {
                statement.setString(x + 1, s);
            }
            else if(parameter instanceof Integer i)
            {
                statement.setInt(x + 1, i);
            }
            else if(parameter instanceof Long l)
            {
                statement.setLong(x + 1, l);
            }
            else
            {
                throw new IllegalArgumentException("Unsupported parameter type: " + parameter);
            }
        }
    }

    public static void setString(PreparedStatement statement, int index, String value, int maxLength)
        throws SQLException
    {
        if(value == null)
        {
            statement.setNull(index, Types.VARCHAR);
        }
        else
        {
            statement.setString(index, value.length() > maxLength ? value.substring(0, maxLength) : value);
        }
    }

    public static void setLong(PreparedStatement statement, int index, Long value) throws SQLException
    {
        if(value == null)
        {
            statement.setNull(index, Types.BIGINT);
        }
        else
        {
            statement.setLong(index, value);
        }
    }

    public static Long getLong(ResultSet rs, String column) throws SQLException
    {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
