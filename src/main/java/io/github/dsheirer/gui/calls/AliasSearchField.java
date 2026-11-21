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
package io.github.dsheirer.gui.calls;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.database.call.IdentifierFilter;
import javafx.css.PseudoClass;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Search field for the TO (talkgroup) or FROM (radio) identifier of a call.  As the user types, a list of matching
 * aliases from the current alias lists drops down.  Selecting an alias filters the calls by the alias's talkgroup or
 * radio identifiers.  Typing without selecting an alias filters by the identifier text or value.
 */
public class AliasSearchField extends TextField
{
    private static final PseudoClass ALIAS_SELECTED = PseudoClass.getPseudoClass("alias-selected");
    private static final int MAX_SUGGESTIONS = 25;

    /**
     * Identifier kind that the field searches
     */
    public enum Kind
    {
        TALKGROUP,
        RADIO
    }

    private final Kind mKind;
    private final AliasModel mAliasModel;
    private final ContextMenu mSuggestions = new ContextMenu();
    private final Runnable mSearchAction;
    private IdentifierFilter mAliasFilter;
    private boolean mSettingText;

    /**
     * Constructs an instance
     * @param kind of identifier to search
     * @param aliasModel for the alias suggestions, or null for text search only
     * @param prompt text
     * @param searchAction invoked when the user selects an alias or presses enter
     */
    public AliasSearchField(Kind kind, AliasModel aliasModel, String prompt, Runnable searchAction)
    {
        mKind = kind;
        mAliasModel = aliasModel;
        mSearchAction = searchAction;
        setPromptText(prompt);
        getStyleClass().add("calls-alias-search");
        mSuggestions.setAutoHide(true);
        updateTooltip();

        textProperty().addListener((observable, oldValue, newValue) -> {
            if(!mSettingText)
            {
                mAliasFilter = null;
                pseudoClassStateChanged(ALIAS_SELECTED, false);
                updateTooltip();
                showSuggestions(newValue);
            }
        });

        focusedProperty().addListener((observable, wasFocused, focused) -> {
            if(!focused)
            {
                mSuggestions.hide();
            }
        });

        setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ESCAPE)
            {
                mSuggestions.hide();
            }
            else if(event.getCode() == KeyCode.DOWN && !mSuggestions.getItems().isEmpty())
            {
                if(!mSuggestions.isShowing())
                {
                    mSuggestions.show(this, Side.BOTTOM, 0, 0);
                }

                mSuggestions.getSkin().getNode().lookupAll(".menu-item").stream().findFirst()
                    .ifPresent(node -> node.requestFocus());
                event.consume();
            }
        });

        setOnAction(event -> {
            mSuggestions.hide();
            mSearchAction.run();
        });
    }

    /**
     * Current search filter: the selected alias, or the typed text.
     */
    public IdentifierFilter getFilter()
    {
        return mAliasFilter != null ? mAliasFilter : IdentifierFilter.text(getText());
    }

    /**
     * Clears the text and any selected alias
     */
    public void reset()
    {
        mSettingText = true;
        setText("");
        mSettingText = false;
        mAliasFilter = null;
        pseudoClassStateChanged(ALIAS_SELECTED, false);
        updateTooltip();
        mSuggestions.hide();
    }

    /**
     * Selects the alias as the search filter
     */
    public void select(Alias alias)
    {
        mAliasFilter = mKind == Kind.TALKGROUP ? IdentifierFilter.talkgroupAlias(alias) :
            IdentifierFilter.radioAlias(alias);
        mSettingText = true;
        setText(alias.getName());
        positionCaret(getText().length());
        mSettingText = false;
        pseudoClassStateChanged(ALIAS_SELECTED, true);
        updateTooltip();
        mSuggestions.hide();
        mSearchAction.run();
    }

    private void updateTooltip()
    {
        String what = mKind == Kind.TALKGROUP ? "talkgroup" : "radio";

        if(mAliasFilter != null)
        {
            setTooltip(new Tooltip("Alias [" + mAliasFilter.aliasName() + "] in alias list [" +
                mAliasFilter.aliasList() + "]: calls with the alias " + what + " identifiers"));
        }
        else
        {
            setTooltip(new Tooltip("Type a " + what + " alias name and select it from the list, or type part of the " +
                what + " identifier or its exact value"));
        }
    }

    /**
     * Aliases (with identifiers of this field's kind) whose name contains the text, sorted by name.
     */
    List<Alias> getMatchingAliases(String text)
    {
        List<Alias> matches = new ArrayList<>();

        if(mAliasModel == null || text == null || text.isBlank())
        {
            return matches;
        }

        String lower = text.trim().toLowerCase(Locale.ROOT);

        for(Alias alias: new ArrayList<>(mAliasModel.aliasList()))
        {
            if(alias.getName() != null && alias.getName().toLowerCase(Locale.ROOT).contains(lower) &&
                (mKind == Kind.TALKGROUP ? IdentifierFilter.hasTalkgroups(alias) : IdentifierFilter.hasRadios(alias)))
            {
                matches.add(alias);
            }
        }

        //Names that start with the text first, then alphabetical
        matches.sort(Comparator.comparing((Alias alias) -> !alias.getName().toLowerCase(Locale.ROOT)
                .startsWith(lower)).thenComparing(alias -> alias.getName().toLowerCase(Locale.ROOT))
            .thenComparing(alias -> alias.getAliasListName() != null ? alias.getAliasListName() : ""));
        return matches;
    }

    private void showSuggestions(String text)
    {
        List<Alias> matches = getMatchingAliases(text);
        mSuggestions.getItems().clear();

        if(matches.isEmpty() || !isFocused())
        {
            mSuggestions.hide();
            return;
        }

        for(Alias alias: matches.subList(0, Math.min(MAX_SUGGESTIONS, matches.size())))
        {
            Label label = new Label(alias.getName() + "   (" + (alias.getAliasListName() != null ?
                alias.getAliasListName() : "no list") + ")");
            CustomMenuItem item = new CustomMenuItem(label, true);
            item.setOnAction(event -> select(alias));
            mSuggestions.getItems().add(item);
        }

        if(matches.size() > MAX_SUGGESTIONS)
        {
            CustomMenuItem more = new CustomMenuItem(new Label((matches.size() - MAX_SUGGESTIONS) +
                " more - keep typing to narrow the list"), false);
            more.setDisable(true);
            mSuggestions.getItems().add(more);
        }

        if(!mSuggestions.isShowing())
        {
            mSuggestions.show(this, Side.BOTTOM, 0, 0);
        }
    }
}
