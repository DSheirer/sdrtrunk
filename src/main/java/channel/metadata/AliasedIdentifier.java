/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package channel.metadata;

import alias.Alias;

public class AliasedIdentifier
{
    private String mIdentifier;
    private Alias mAlias;

    /**
     * Identifier and corresponding optional alias
     */
    public AliasedIdentifier()
    {
    }

    /**
     * Resets alias and value to null
     *
     * Package Private - should only be changed within MutableMetadata
     */
    void reset()
    {
        mIdentifier = null;
        mAlias = null;
    }

    /**
     * Identifier
     */
    public String getIdentifier()
    {
        return mIdentifier;
    }

    /**
     * Sets the identifier to the argument value
     *
     * Package Private - should only be changed within MutableMetadata
     */
    void setIdentifier(String identifier)
    {
        mIdentifier = identifier;
    }

    /**
     * Indicates if there is a non-null, non-empty identifier
     */
    public boolean hasIdentifier()
    {
        return mIdentifier != null && !mIdentifier.isEmpty();
    }

    /**
     * Alias for the identifier.
     */
    public Alias getAlias()
    {
        return mAlias;
    }

    /**
     * Sets the alias to the argument
     *
     * Package Private - should only be changed within MutableMetadata
     */
    void setAlias(Alias alias)
    {
        mAlias = alias;
    }

    /**
     * Indicates if there is an alias corresponding to the identifier
     */
    public boolean hasAlias()
    {
        return mAlias != null;
    }

    public AliasedIdentifier copyOf()
    {
        AliasedIdentifier copy = new AliasedIdentifier();

        copy.setAlias(mAlias);

        if(mIdentifier != null)
        {
            copy.setIdentifier(new String(mIdentifier));
        }

        return copy;
    }
}
