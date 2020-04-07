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

package io.github.dsheirer.identifier.tone;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.common.base.Joiner;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * A sequentially ordered list of tones
 */
public class ToneSequence
{
    private ObservableList<Tone> mTones = FXCollections.observableArrayList(Tone.extractor());

    /**
     * Constructs an instance
     * @param tones for the instance
     */
    public ToneSequence(List<Tone> tones)
    {
        mTones.setAll(tones);
    }

    /**
     * Constructs an instance
     */
    public ToneSequence()
    {
    }

    /**
     * Creates a deep copy of this tone sequence
     */
    public ToneSequence copyOf()
    {
        ToneSequence toneSequence = new ToneSequence();
        for(Tone tone: mTones)
        {
            toneSequence.addTone(tone.copyOf());
        }
        return toneSequence;
    }

    public ObservableList<Tone> tonesProperty()
    {
        return mTones;
    }

    /**
     * List of tones
     */
    @JacksonXmlProperty(isAttribute = false, localName = "tone")
    public List<Tone> getTones()
    {
        return mTones;
    }

    /**
     * Sets the tones for this list
     */
    public void setTones(List<Tone> tones)
    {
        if(tones != null)
        {
            mTones.setAll(tones);
        }
    }

    @JsonIgnore
    public boolean hasTones()
    {
        return !mTones.isEmpty();
    }

    /**
     * Clears all tones from this sequence
     */
    public void clear()
    {
        mTones.clear();
    }

    /**
     * Adds a tone to this sequence
     */
    public void addTone(Tone tone)
    {
        if(tone != null)
        {
            mTones.add(tone);
        }
    }

    /**
     * Removes the tone from this sequence
     */
    public void removeTone(Tone tone)
    {
        if(tone != null)
        {
            mTones.remove(tone);
        }
    }

    @Override
    public String toString()
    {
        if(mTones.isEmpty())
        {
            return "Tones: (empty)";
        }

        return Joiner.on(",").join(mTones);
    }

    /**
     * Indicates if this sequence of tones is contained in the argument tone sequence.  This method is used for
     * matching this sequence to another sequence, where this sequence is either a full or partial sequence contained
     * in the argument sequence.
     *
     * @param other to check for containment
     * @return true if contained
     */
    public boolean isContainedIn(ToneSequence other)
    {
        if(other == null || other.getTones().isEmpty())
        {
            return false;
        }

        List<Tone> otherTones = other.getTones();

        int pointer = 0;

        for(Tone thisTone: mTones)
        {
            boolean matches = false;

            for(int x = pointer; x < otherTones.size(); x++)
            {
                if(thisTone.isContainedIn(otherTones.get(x)))
                {
                    pointer = x + 1;
                    matches = true;
                    break;
                }
            }

            if(!matches)
            {
                return false;
            }
        }

        return true;
    }
}
