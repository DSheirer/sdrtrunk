/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.alias.id.tone;

import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.identifier.tone.Tone;
import io.github.dsheirer.identifier.tone.ToneSequence;
import javafx.collections.ListChangeListener;

import java.util.List;

/**
 * Tone sequence alias identifier.  This is used for matching tone sequences produced by the AMBE audio CODEC.  Some
 * examples include DTMF dialed numbers, KNOX tones, 2-Tone, Tone-outs, etc.
 */
public class TonesID extends AliasID implements ListChangeListener<Tone>
{
    private ToneSequence mToneSequence;

    public TonesID()
    {
        setToneSequence(new ToneSequence());
        //Empty serialization constructor
    }

    /**
     * Constructs an instance with the specified tone sequence
     */
    public TonesID(ToneSequence toneSequence)
    {
        setToneSequence(toneSequence);
    }

    @Override
    public AliasIDType getType()
    {
        return AliasIDType.TONES;
    }

    /**
     * List of tones that define this identifier
     */

    public ToneSequence getToneSequence()
    {
        return mToneSequence;
    }

    /**
     * Sets the tone sequence(s) for this ID
     */
    public void setToneSequence(ToneSequence toneSequence)
    {
        if(mToneSequence != null)
        {
            mToneSequence.tonesProperty().removeListener(this);
        }

        mToneSequence = toneSequence;
        mToneSequence.tonesProperty().addListener(this);
    }

    /**
     * Indicates if the other id is a tones identifier and has the exact same sequence of tones.  However, it does not
     * compare each of the tone duration value.
     * @param id to check for match
     * @return true if the identifiers match
     */
    @Override
    public boolean matches(AliasID id)
    {
        boolean match = true;

        if(mToneSequence.getTones().isEmpty() || !(id instanceof TonesID))
        {
            match = false;
        }
        else
        {
            List<Tone> otherTones = ((TonesID)id).getToneSequence().getTones();
            List<Tone> thisTones = mToneSequence.getTones();

            if(thisTones.size() == otherTones.size())
            {
                for(int x = 0; x < thisTones.size(); x++)
                {
                    if(thisTones.get(x).getAmbeTone() != otherTones.get(x).getAmbeTone())
                    {
                        match = false;
                        continue;
                    }
                }
            }
            else
            {
                match = false;
            }
        }

        return match;
    }

    @Override
    public boolean isValid()
    {
        return getToneSequence() != null && getToneSequence().hasTones();
    }

    @Override
    public boolean isAudioIdentifier()
    {
        //This is not an audio identifier in the context that this method is used for.
        return false;
    }

    @Override
    public String toString()
    {
        if(getToneSequence() != null)
        {
            return "Tones: " + getToneSequence().toString();
        }

        return "Tones: (empty)";
    }

    @Override
    public void onChanged(Change<? extends Tone> c)
    {
        updateValueProperty();
    }
}
