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

package io.github.dsheirer.gui.settings.radioreference;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.radioreference.Level;
import io.github.dsheirer.gui.playlist.radioreference.LoginDialog;
import io.github.dsheirer.gui.settings.SettingsContext;
import io.github.dsheirer.gui.settings.SettingsPage;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.SettingsSection;
import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.settings.radioreference.AliasListUpdateInterval;
import io.github.dsheirer.settings.radioreference.RadioReferenceSettings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import org.controlsfx.control.ToggleSwitch;

/**
 * Radio Reference settings: the account login, defaults for importing from Radio Reference, and updates for alias
 * lists that track a Radio Reference system.
 */
public class RadioReferenceSettingsPage extends SettingsPage
{
    private final RadioReferenceSettings mRadioReferenceSettings;
    private final RadioReference mRadioReference;
    private ComboBox<AliasListUpdateInterval> mUpdateIntervalComboBox;
    private Button mForgetLoginButton;

    public RadioReferenceSettingsPage(SettingsContext context)
    {
        super(SettingsPageType.RADIO_REFERENCE, context.settings());
        mRadioReferenceSettings = context.settings().getRadioReferenceSettings();
        mRadioReference = context.radioReference();

        if(mRadioReference != null)
        {
            createAccountSection();
            MyEventBus.getGlobalEventBus().register(this);
        }

        createImportSection();

        SettingsSection aliasLists = addSection("Alias Lists");
        aliasLists.addRow("Check tracked systems for talkgroup changes", getUpdateIntervalComboBox())
            .defaultValue(getUpdateIntervalComboBox().valueProperty(),
                RadioReferenceSettings.DEFAULT_ALIAS_LIST_UPDATE_INTERVAL, getUpdateIntervalComboBox()::setValue)
            .help("Alias lists that track a Radio Reference system check the system on this schedule while " +
                "sdrtrunk is running and a Radio Reference login is stored.  Alias lists with Auto-Update enabled " +
                "add or update talkgroup aliases when the system changed.  Use Refresh Now in the playlist editor's " +
                "Aliases tab to update an alias list immediately.");
    }

    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
    }

    /**
     * Login status, and buttons to log in and to forget the stored login
     */
    private void createAccountSection()
    {
        SettingsSection account = addSection("Account");

        Label status = new Label();
        status.setWrapText(true);
        status.textProperty().bind(Bindings.createStringBinding(this::describeLogin,
            mRadioReference.availableProperty(), mRadioReference.premiumAccountProperty(),
            mRadioReference.userNameProperty(), mRadioReference.accountExpiresProperty()));
        account.addRow("Status", status);

        Button login = new Button("Log In...");
        login.setOnAction(event -> {
            LoginDialog dialog = new LoginDialog(getSettings());
            dialog.initOwner(getScene().getWindow());
            dialog.showAndWait().ifPresent(mRadioReference::setAuthorizationInformation);
        });

        mForgetLoginButton = new Button("Forget Stored Login");
        mForgetLoginButton.setOnAction(event -> mRadioReferenceSettings.removeStoredCredentials());
        updateForgetLoginButton();

        account.addRow("Login", login, mForgetLoginButton)
            .help("Logging in here also logs in the playlist editor's Radio Reference tab.  A stored login is used " +
                "automatically when sdrtrunk starts.  Forgetting the stored login doesn't end the current session.");
    }

    /**
     * Describes the current login for the status row
     */
    private String describeLogin()
    {
        if(!mRadioReference.availableProperty().get())
        {
            return "Not logged in";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Logged in as ").append(mRadioReference.userNameProperty().get());

        if(mRadioReference.premiumAccountProperty().get())
        {
            sb.append(" - premium account");
            String expires = mRadioReference.accountExpiresProperty().get();

            if(expires != null && !expires.isEmpty())
            {
                sb.append(", expires ").append(expires);
            }
        }
        else
        {
            sb.append(" - not a premium account.  Importing requires a premium account.");
        }

        return sb.toString();
    }

    private void updateForgetLoginButton()
    {
        mForgetLoginButton.setDisable(!mRadioReferenceSettings.hasStoredCredentials());
    }

    /**
     * Defaults for importing talkgroups and channels from Radio Reference in the playlist editor
     */
    private void createImportSection()
    {
        SettingsSection imports = addSection("Import Defaults");

        ToggleSwitch encrypted = new ToggleSwitch();
        encrypted.setSelected(mRadioReferenceSettings.isEncryptedTalkgroupDoNotMonitor());
        encrypted.selectedProperty().addListener((observable, oldValue, selected) ->
            mRadioReferenceSettings.setEncryptedTalkgroupDoNotMonitor(selected));
        imports.addRow("Set imported encrypted talkgroups to muted", encrypted)
            .defaultValue(encrypted.selectedProperty(), RadioReferenceSettings.DEFAULT_ENCRYPTED_TALKGROUP_DO_NOT_MONITOR,
                encrypted::setSelected);

        ToggleSwitch siteChannel = new ToggleSwitch();
        siteChannel.setSelected(mRadioReferenceSettings.isCreateAndShowChannelEditor());
        siteChannel.selectedProperty().addListener((observable, oldValue, selected) ->
            mRadioReferenceSettings.setCreateAndShowChannelEditor(selected));
        imports.addRow("Go to the channel editor after creating a channel from a system site", siteChannel)
            .defaultValue(siteChannel.selectedProperty(), RadioReferenceSettings.DEFAULT_CREATE_AND_SHOW_CHANNEL_EDITOR,
                siteChannel::setSelected);

        imports.addHelp("Go to the channel editor after creating a channel from an agency frequency:");
        addAgencyRow(imports, "Nationwide agencies", Level.NATIONAL);
        addAgencyRow(imports, "State agencies", Level.STATE);
        addAgencyRow(imports, "County agencies", Level.COUNTY);
    }

    private void addAgencyRow(SettingsSection section, String label, Level level)
    {
        ToggleSwitch toggle = new ToggleSwitch();
        toggle.setSelected(mRadioReferenceSettings.getShowChannelEditor(level));
        toggle.selectedProperty().addListener((observable, oldValue, selected) ->
            mRadioReferenceSettings.setShowChannelEditor(selected, level));
        section.addRow(label, toggle)
            .defaultValue(toggle.selectedProperty(), RadioReferenceSettings.DEFAULT_SHOW_CHANNEL_EDITOR, toggle::setSelected);
    }

    private ComboBox<AliasListUpdateInterval> getUpdateIntervalComboBox()
    {
        if(mUpdateIntervalComboBox == null)
        {
            mUpdateIntervalComboBox = new ComboBox<>();
            mUpdateIntervalComboBox.getItems().addAll(AliasListUpdateInterval.values());
            mUpdateIntervalComboBox.getSelectionModel().select(mRadioReferenceSettings.getAliasListUpdateInterval());
            mUpdateIntervalComboBox.setOnAction(event -> mRadioReferenceSettings
                .setAliasListUpdateInterval(mUpdateIntervalComboBox.getSelectionModel().getSelectedItem()));
        }

        return mUpdateIntervalComboBox;
    }

    @Subscribe
    public void settingsUpdated(SettingsType settingsType)
    {
        if(settingsType == SettingsType.RADIO_REFERENCE)
        {
            Platform.runLater(this::updateForgetLoginButton);
        }
    }
}
