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

package io.github.dsheirer.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

/**
 * Logback Log Appender that continuously writes to a JavaFX text area
 *
 * Note: this appender doesn't trim/truncate the text in the text area.
 */
public class TextAreaLogAppender extends AppenderBase<ILoggingEvent>
{
    private TextArea mTextArea;

    public TextAreaLogAppender(TextArea textArea, String name)
    {
        mTextArea = textArea;
        setName(name);
    }

    @Override
    protected void append(ILoggingEvent eventObject)
    {
        Platform.runLater(() ->
        {
            mTextArea.appendText("\n" + eventObject.getMessage());
        });
    }
}
