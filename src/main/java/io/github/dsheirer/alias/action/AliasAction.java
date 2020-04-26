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
package io.github.dsheirer.alias.action;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.action.beep.BeepAction;
import io.github.dsheirer.alias.action.clip.ClipAction;
import io.github.dsheirer.alias.action.script.ScriptAction;
import io.github.dsheirer.message.IMessage;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Callback;

/**
 * Alias action defines an action to execute when an alias is detected active.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BeepAction.class, name="beepAction"),
    @JsonSubTypes.Type(value = ClipAction.class, name = "clipAction"),
    @JsonSubTypes.Type(value = RecurringAction.class, name = "recurringAction"),
    @JsonSubTypes.Type(value = ScriptAction.class, name = "scriptAction")
})
@JacksonXmlRootElement(localName = "action")
public abstract class AliasAction
{
    private SimpleStringProperty mValueProperty = new SimpleStringProperty();

    public AliasAction()
    {
    }

    /**
     * String property representation of this alias ID
     */
    @JsonIgnore
    public SimpleStringProperty valueProperty()
    {
        return mValueProperty;
    }

    /**
     * Updates the value property for this alias ID.  Note: this method is intended to be invoked by all subclasses
     * each time that any of the subclass member variable values change.
     */
    public void updateValueProperty()
    {
        valueProperty().set(toString());

        //Hack: harmless, but the list extractor does not consistently update unless we do this.
        valueProperty().get();
    }

    @JsonIgnore
    public abstract AliasActionType getType();

    /**
     * Task to execute when an alias action is defined.  The message argument is
     * the original message containing one or more aliases that have an alias
     * action attached.  The alias argument is the parent alias containing the
     * alias action.
     */
    public abstract void execute(Alias alias, IMessage message);

    /**
     * Dismiss a persistent alias action
     */
    public abstract void dismiss(boolean reset);

    /**
     * Creates an observable property extractor for use with observable lists to detect changes internal to this object.
     */
    public static Callback<AliasAction,Observable[]> extractor()
    {
        return (AliasAction aliasAction) -> new Observable[] {aliasAction.valueProperty()};
    }
}
