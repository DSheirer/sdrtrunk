/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.alias.action.beep;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.action.AliasActionType;
import io.github.dsheirer.alias.action.RecurringAction;
import io.github.dsheirer.audio.AudioFormats;
import io.github.dsheirer.gui.preference.playback.ToneFrequency;
import io.github.dsheirer.gui.preference.playback.ToneUtil;
import io.github.dsheirer.gui.preference.playback.ToneVolume;
import io.github.dsheirer.message.IMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class BeepAction extends RecurringAction
{
	private static final Logger mLog = LoggerFactory.getLogger(BeepAction.class);
	private byte[] mToneBytes;

	public BeepAction()
	{
		setInterval(Interval.ONCE);
		float[] tone = ToneUtil.getTone(ToneFrequency.F700, ToneVolume.V10, 1000);

		/* Little-endian byte buffer */
		ByteBuffer buffer = ByteBuffer.allocate(tone.length * 2).order(ByteOrder.LITTLE_ENDIAN);

		ShortBuffer shortBuffer = buffer.asShortBuffer();

		for(float sample : tone)
		{
			shortBuffer.put((short) (sample * Short.MAX_VALUE));
		}

		mToneBytes = buffer.array();
	}

	@JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
	@Override
	public AliasActionType getType()
	{
		return AliasActionType.BEEP;
	}

	@Override
	public void performAction(Alias alias, IMessage message )
	{
		DataLine.Info info = new DataLine.Info(Clip.class, AudioFormats.PCM_SIGNED_8000_HZ_16_BIT_MONO);

		if(!AudioSystem.isLineSupported(info))
		{
			mLog.error("Audio clip playback is not supported on this system");
			return;
		}

		try
		{
			Clip clip = (Clip)AudioSystem.getLine(info);
			clip.open(AudioFormats.PCM_SIGNED_8000_HZ_16_BIT_MONO, mToneBytes, 0, mToneBytes.length);
			clip.start();
		}
		catch(Exception e)
		{
			mLog.error("Error attempting to play audio test tone");
		}
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Beep");

		if(getInterval() != null)
		{
			switch(getInterval())
			{
				case ONCE:
					sb.append(" Once");
					break;
				case DELAYED_RESET:
					sb.append(" Once, Reset After ").append(getPeriod()).append(" Seconds");
					break;
				case UNTIL_DISMISSED:
					sb.append(" Every ").append(getPeriod()).append(" Seconds Until Dismissed");
					break;
			}
		}

		return sb.toString();
	}
}
