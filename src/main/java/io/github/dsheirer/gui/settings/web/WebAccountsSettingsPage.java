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

package io.github.dsheirer.gui.settings.web;

import io.github.dsheirer.database.account.ApiTokenRecord;
import io.github.dsheirer.database.account.UserRecord;
import io.github.dsheirer.gui.settings.SettingsPage;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.SettingsSection;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.web.WebServerSettings;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.web.WebServer;
import io.github.dsheirer.web.account.AccountService;
import io.github.dsheirer.web.api.ApiException;
import io.github.dsheirer.web.auth.Principal;
import io.github.dsheirer.web.auth.Role;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.controlsfx.control.ToggleSwitch;

/**
 * Web account administration in the settings dialog: approve the accounts that users request on the web sign-in
 * page, see who is active and when each account last used the web page or the API, reset passwords (including the
 * administrator's, so a forgotten password can be recovered here), change roles, enable or disable accounts, and
 * create or revoke the API tokens of an account.
 *
 * Changes are made as {@link Principal#DESKTOP} and recorded in the web audit log.  The accounts are in the call
 * database, so this page works while the web server is off.
 */
public class WebAccountsSettingsPage extends SettingsPage
{
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault());
    private static final String ADDRESS = "desktop";
    private static final Duration REFRESH_INTERVAL = Duration.seconds(15);

    private final AccountService mAccountService;
    private final ObservableList<AccountRow> mRows = FXCollections.observableArrayList();
    private final TableView<AccountRow> mTable = new TableView<>(mRows);
    private final Label mSummary = new Label();
    private final Label mMessage = new Label();
    private final List<Button> mSelectionButtons = new ArrayList<>();
    private Button mApproveButton;
    private Button mEnableButton;
    private Button mDeleteButton;
    private boolean mLoading;

    /**
     * Constructs an instance
     * @param settings to edit
     * @param webServer with the account service, or null when there isn't one
     */
    public WebAccountsSettingsPage(Settings settings, WebServer webServer)
    {
        super(SettingsPageType.WEB_ACCOUNTS, settings);
        mAccountService = webServer != null ? webServer.getAccountService() : null;
        WebServerSettings webSettings = settings.getWebServerSettings();

        SettingsSection requests = addSection("Account Requests");
        ToggleSwitch registration = new ToggleSwitch();
        registration.setSelected(webSettings.isRegistrationEnabled());
        registration.selectedProperty().addListener((observable, oldValue, selected) ->
            webSettings.setRegistrationEnabled(selected));
        requests.addRow("Let users request accounts", registration)
            .defaultValue(registration.selectedProperty(), WebServerSettings.DEFAULT_REGISTRATION_ENABLED,
                registration::setSelected)
            .help("Shows 'Request an account' on the web sign-in page.  A requested account can't sign in until " +
                "you approve it below or on the web Admin page.");

        SettingsSection accounts = addSection("Accounts");
        accounts.addHelp("Accounts for the web page and the web API.  An account is active when it made a request " +
            "in the last " + (AccountService.ACTIVE_WINDOW_MS / 60_000) + " minutes.  Changes made here are " +
            "recorded in the audit log as 'desktop'.");
        mSummary.getStyleClass().add("settings-help");
        mMessage.setWrapText(true);
        mMessage.setMaxWidth(Double.MAX_VALUE);

        createTable();
        accounts.addContent(mSummary);
        accounts.addContent(mTable);
        accounts.addContent(createButtons());
        accounts.addContent(mMessage);

        if(mAccountService == null)
        {
            showMessage("The web server isn't available, so accounts can't be managed.", true);
            mTable.setDisable(true);
        }
        else
        {
            refresh();
            Timeline timeline = new Timeline(new KeyFrame(REFRESH_INTERVAL, event -> {
                if(getScene() != null && getScene().getWindow() != null && getScene().getWindow().isShowing())
                {
                    refresh();
                }
            }));
            timeline.setCycleCount(Animation.INDEFINITE);
            sceneProperty().addListener((observable, oldScene, scene) -> {
                if(scene != null)
                {
                    refresh();
                    timeline.play();
                }
                else
                {
                    timeline.stop();
                }
            });
        }

        updateButtons();
    }

    //----------------------------------------------------------------------------------------------------------------
    // Layout
    //----------------------------------------------------------------------------------------------------------------

    private void createTable()
    {
        mTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        mTable.setPlaceholder(new Label("No accounts"));
        mTable.setPrefHeight(300);
        mTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        mTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, row) -> updateButtons());
        mTable.setRowFactory(table -> {
            TableRow<AccountRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if(event.getClickCount() == 2 && !row.isEmpty() && row.getItem().user().pendingApproval())
                {
                    approve();
                }
            });
            return row;
        });

        mTable.getColumns().add(column("Username", 120, row -> row.user().username()));
        mTable.getColumns().add(column("Name", 120, row -> row.user().displayName() != null ?
            row.user().displayName() : ""));
        mTable.getColumns().add(column("Role", 80, row -> Role.parse(row.user().role()).map(Role::getLabel)
            .orElse(row.user().role())));

        TableColumn<AccountRow,String> status = column("Status", 130, AccountRow::status);
        status.setCellFactory(column -> new TableCell<>()
        {
            @Override
            protected void updateItem(String item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("web-account-active", "web-account-pending", "web-account-disabled");
                AccountRow row = empty || getTableRow() == null ? null : getTableRow().getItem();

                if(row != null)
                {
                        String style = row.user().pendingApproval() ? "web-account-pending" :
                        !row.user().enabled() ? "web-account-disabled" :
                        row.activity().active() ? "web-account-active" : null;

                    if(style != null)
                    {
                        getStyleClass().add(style);
                    }
                }
            }
        });
        mTable.getColumns().add(status);
        TableColumn<AccountRow,Integer> sessions = new TableColumn<>("Sessions");
        sessions.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().activity().sessions()));
        sessions.setPrefWidth(70);
        sessions.setStyle("-fx-alignment: CENTER-RIGHT;");
        mTable.getColumns().add(sessions);
        mTable.getColumns().add(column("Web Page", 110, row -> ago(row.activity().lastWebTime())));
        mTable.getColumns().add(column("API", 110, row -> ago(row.activity().lastApiTime())));
        mTable.getColumns().add(column("Last Sign-in", 110, row -> ago(row.user().lastLoginTime())));
    }

    private static TableColumn<AccountRow,String> column(String title, double width,
                                                          Function<AccountRow,String> value)
    {
        TableColumn<AccountRow,String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private Node createButtons()
    {
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> refresh());
        Button create = new Button("New Account...");
        create.setOnAction(event -> createAccount());

        mApproveButton = selectionButton("Approve...", this::approve);
        Button role = selectionButton("Change Role...", this::changeRole);
        mEnableButton = selectionButton("Disable", this::toggleEnabled);
        Button reset = selectionButton("Reset Password...", this::resetPassword);
        Button token = selectionButton("New API Token...", this::createToken);
        Button tokens = selectionButton("API Tokens...", this::showTokens);
        Button signOut = selectionButton("Sign Out Everywhere...", this::revokeAll);
        mDeleteButton = selectionButton("Delete...", this::delete);

        FlowPane buttons = new FlowPane(8, 8, refresh, create, mApproveButton, role, mEnableButton, reset, token,
            tokens, signOut, mDeleteButton);
        buttons.setPadding(new Insets(8, 0, 4, 0));
        return buttons;
    }

    private Button selectionButton(String text, Runnable action)
    {
        Button button = new Button(text);
        button.setOnAction(event -> action.run());
        mSelectionButtons.add(button);
        return button;
    }

    private void updateButtons()
    {
        AccountRow row = mTable.getSelectionModel().getSelectedItem();
        boolean none = row == null || mAccountService == null;
        mSelectionButtons.forEach(button -> button.setDisable(none));

        if(row != null)
        {
            mApproveButton.setDisable(!row.user().pendingApproval());
            mEnableButton.setText(row.user().enabled() ? "Disable" : "Enable");
            mEnableButton.setDisable(row.user().pendingApproval());
            mDeleteButton.setText(row.user().pendingApproval() ? "Reject..." : "Delete...");
        }
    }

    //----------------------------------------------------------------------------------------------------------------
    // Loading
    //----------------------------------------------------------------------------------------------------------------

    private void refresh()
    {
        if(mAccountService == null || mLoading)
        {
            return;
        }

        mLoading = true;
        run(() -> {
            List<AccountRow> rows = new ArrayList<>();
            long afterId = 0;
            List<UserRecord> page;

            do
            {
                page = mAccountService.listUsers(afterId, 500);
                List<UserRecord> users = page.size() > 500 ? page.subList(0, 500) : page;

                for(UserRecord user: users)
                {
                    rows.add(new AccountRow(user, mAccountService.getActivity(user)));
                }

                afterId = users.isEmpty() ? afterId : users.getLast().id();
            }
            while(page.size() > 500);

            //Requests that wait for approval first, then the accounts in creation order
            rows.sort((a, b) -> Boolean.compare(b.user().pendingApproval(), a.user().pendingApproval()));
            return rows;
        }, this::show, error -> mLoading = false);
    }

    private void show(List<AccountRow> rows)
    {
        mLoading = false;
        Long selectedId = Optional.ofNullable(mTable.getSelectionModel().getSelectedItem())
            .map(row -> row.user().id()).orElse(null);
        mRows.setAll(rows);

        if(selectedId != null)
        {
            mRows.stream().filter(row -> row.user().id() == selectedId).findFirst()
                .ifPresent(row -> mTable.getSelectionModel().select(row));
        }

        long pending = rows.stream().filter(row -> row.user().pendingApproval()).count();
        long active = rows.stream().filter(row -> row.activity().active()).count();
        long accounts = rows.size() - pending;
        mSummary.setText(accounts + (accounts == 1 ? " account, " : " accounts, ") + active + " active now" +
            (pending > 0 ? ", " + pending + (pending == 1 ? " request waits" : " requests wait") +
                " for approval" : ""));
        updateButtons();
    }

    //----------------------------------------------------------------------------------------------------------------
    // Actions
    //----------------------------------------------------------------------------------------------------------------

    private UserRecord selected()
    {
        AccountRow row = mTable.getSelectionModel().getSelectedItem();
        return row != null ? row.user() : null;
    }

    private void approve()
    {
        UserRecord user = selected();

        if(user == null || !user.pendingApproval())
        {
            return;
        }

        chooseRole("Approve Account", "Approve the account request from '" + user.username() + "'",
            AccountService.REGISTERED_ROLE).ifPresent(role -> act(() -> mAccountService.approveUser(Principal.DESKTOP,
            user.id(), role.name(), ADDRESS), result -> showMessage("Approved '" + user.username() + "' as " +
            role.getLabel() + ".", false)));
    }

    private void changeRole()
    {
        UserRecord user = selected();

        if(user == null)
        {
            return;
        }

        Role current = Role.parse(user.role()).orElse(Role.LISTENER);
        chooseRole("Change Role", "Role of '" + user.username() + "'", current)
            .filter(role -> role != current)
            .ifPresent(role -> act(() -> mAccountService.updateUser(Principal.DESKTOP, user.id(), null, role.name(),
                null, ADDRESS), result -> showMessage("'" + user.username() + "' is now " + role.getLabel() + ".",
                false)));
    }

    private void toggleEnabled()
    {
        UserRecord user = selected();

        if(user == null)
        {
            return;
        }

        boolean enable = !user.enabled();
        act(() -> mAccountService.updateUser(Principal.DESKTOP, user.id(), null, null, enable, ADDRESS),
            result -> showMessage((enable ? "Enabled '" : "Disabled '") + user.username() + "'." +
                (enable ? "" : "  Its sessions ended."), false));
    }

    private void resetPassword()
    {
        UserRecord user = selected();

        if(user == null || !confirm("Reset Password", "Give '" + user.username() + "' a new temporary password?",
            "The account's browser sessions end, a lockout is cleared, and a new password must be chosen at the " +
                "next sign-in.  API tokens keep working."))
        {
            return;
        }

        act(() -> mAccountService.resetPassword(Principal.DESKTOP, user.id(), ADDRESS),
            password -> showSecret("Temporary Password", "Temporary password for '" + user.username() + "'",
                password, "Give it to the user, who signs in with it and then chooses a new password.  It is " +
                    "shown only now."));
    }

    private void createToken()
    {
        UserRecord user = selected();

        if(user == null)
        {
            return;
        }

        Dialog<ButtonType> dialog = dialog("New API Token", "API token for '" + user.username() + "'");
        TextField name = new TextField();
        name.setPromptText("For example: scanner script");
        name.setPrefColumnCount(24);
        ComboBox<String> expiry = new ComboBox<>(FXCollections.observableArrayList("Never", "30 days", "90 days",
            "1 year"));
        expiry.getSelectionModel().selectFirst();
        GridPane grid = grid();
        grid.addRow(0, new Label("Name"), name);
        grid.addRow(1, new Label("Expires"), expiry);
        Label help = new Label("The token acts as this account with the permissions of its " +
            Role.parse(user.role()).map(Role::getLabel).orElse(user.role()) + " role.");
        help.setWrapText(true);
        grid.add(help, 0, 2, 2, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(name.textProperty().isEmpty());
        Platform.runLater(name::requestFocus);

        if(dialog.showAndWait().filter(button -> button == ButtonType.OK).isEmpty())
        {
            return;
        }

        Integer days = switch(expiry.getValue())
        {
            case "30 days" -> 30;
            case "90 days" -> 90;
            case "1 year" -> 365;
            default -> null;
        };
        String tokenName = name.getText().trim();
        act(() -> mAccountService.createTokenFor(Principal.DESKTOP, user.id(), tokenName, days, null, ADDRESS),
            created -> showSecret("API Token", "API token '" + tokenName + "' for '" + user.username() + "'",
                created.token(), "Give it to the user.  Scripts send it as the header 'Authorization: Bearer " +
                    "<token>'.  It is shown only now: sdrtrunk stores only its hash."));
    }

    private void showTokens()
    {
        UserRecord user = selected();

        if(user == null)
        {
            return;
        }

        run(() -> mAccountService.listTokens(user.id()), tokens -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("API Tokens");
            dialog.setHeaderText("API tokens of '" + user.username() + "'");
            dialog.initOwner(getScene().getWindow());
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            ObservableList<ApiTokenRecord> items = FXCollections.observableArrayList(tokens);
            TableView<ApiTokenRecord> table = new TableView<>(items);
            table.setPlaceholder(new Label("No API tokens"));
            table.setPrefSize(640, 240);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            table.getColumns().add(tokenColumn("Name", 160, ApiTokenRecord::name));
            table.getColumns().add(tokenColumn("Token", 110, token -> token.prefix() + "..."));
            table.getColumns().add(tokenColumn("Created", 120, token -> format(token.createdTime())));
            table.getColumns().add(tokenColumn("Expires", 120, token -> token.expiresTime() == null ? "Never" :
                (token.isExpired(System.currentTimeMillis()) ? "Expired " : "") + format(token.expiresTime())));
            table.getColumns().add(tokenColumn("Last Used", 110, token -> ago(token.lastUsedTime())));

            Button revoke = new Button("Revoke");
            revoke.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
            revoke.setOnAction(event -> {
                ApiTokenRecord token = table.getSelectionModel().getSelectedItem();

                if(token != null && confirm("Revoke API Token", "Revoke the token '" + token.name() + "'?",
                    "Scripts that use it stop working."))
                {
                    act(() -> {
                        mAccountService.revokeToken(Principal.DESKTOP, user.id(), token.id(), ADDRESS);
                        return token;
                    }, revoked -> {
                        items.remove(revoked);
                        showMessage("Revoked the token '" + revoked.name() + "' of '" + user.username() + "'.",
                            false);
                    });
                }
            });
            HBox buttons = new HBox(8, revoke);
            buttons.setAlignment(Pos.CENTER_LEFT);
            VBox content = new VBox(8, table, buttons);
            dialog.getDialogPane().setContent(content);
            dialog.showAndWait();
        }, null);
    }

    private static TableColumn<ApiTokenRecord,String> tokenColumn(String title, double width,
                                                                  Function<ApiTokenRecord,String> value)
    {
        TableColumn<ApiTokenRecord,String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private void revokeAll()
    {
        UserRecord user = selected();

        if(user == null || !confirm("Sign Out Everywhere", "End all sessions of '" + user.username() + "' and " +
            "delete all of its API tokens?", "Browsers must sign in again and scripts need a new token."))
        {
            return;
        }

        act(() -> mAccountService.revokeAll(Principal.DESKTOP, user.id(), ADDRESS),
            count -> showMessage("Revoked " + count + " sessions and API tokens of '" + user.username() + "'.",
                false));
    }

    private void delete()
    {
        UserRecord user = selected();

        if(user == null)
        {
            return;
        }

        boolean request = user.pendingApproval();

        if(!confirm(request ? "Reject Account Request" : "Delete Account", request ?
            "Reject and delete the account request from '" + user.username() + "'?" :
            "Delete the account '" + user.username() + "'?", request ? null :
            "Its API tokens are deleted and its sessions end.  The audit log keeps its entries."))
        {
            return;
        }

        act(() -> {
            mAccountService.deleteUser(Principal.DESKTOP, user.id(), ADDRESS);
            return user;
        }, deleted -> showMessage((request ? "Rejected '" : "Deleted '") + deleted.username() + "'.", false));
    }

    private void createAccount()
    {
        if(mAccountService == null)
        {
            return;
        }

        Dialog<ButtonType> dialog = dialog("New Account", "Create a web account");
        TextField username = new TextField();
        username.setPrefColumnCount(20);
        TextField displayName = new TextField();
        ComboBox<Role> role = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        role.setValue(Role.LISTENER);
        GridPane grid = grid();
        grid.addRow(0, new Label("Username"), username);
        grid.addRow(1, new Label("Display name"), displayName);
        grid.addRow(2, new Label("Role"), role);
        Label help = new Label("The account gets a temporary password that must be changed at the first sign-in.");
        help.setWrapText(true);
        grid.add(help, 0, 3, 2, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty()
            .bind(username.textProperty().isEmpty());
        Platform.runLater(username::requestFocus);

        if(dialog.showAndWait().filter(button -> button == ButtonType.OK).isEmpty())
        {
            return;
        }

        act(() -> mAccountService.createUser(Principal.DESKTOP, username.getText(), displayName.getText(),
            role.getValue().name(), ADDRESS), created -> showSecret("New Account", "Temporary password for '" +
            created.user().username() + "'", created.temporaryPassword(), "Give it to the user, who signs in " +
            "with it and then chooses a new password.  It is shown only now."));
    }

    //----------------------------------------------------------------------------------------------------------------
    // Support
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Runs an account change off the JavaFX thread, then shows its result and reloads the accounts
     */
    private <T> void act(Supplier<T> work, Consumer<T> done)
    {
        run(work, result -> {
            done.accept(result);
            refresh();
        }, error -> refresh());
    }

    /**
     * Runs database work off the JavaFX thread.  Errors are shown under the table.
     */
    private <T> void run(Supplier<T> work, Consumer<T> done, Consumer<Throwable> failed)
    {
        CompletableFuture.supplyAsync(work, ThreadPool.CACHED).whenComplete((result, error) -> Platform.runLater(() -> {
            if(error != null)
            {
                Throwable cause = error instanceof CompletionException && error.getCause() != null ?
                    error.getCause() : error;
                showMessage(describe(cause), true);

                if(failed != null)
                {
                    failed.accept(cause);
                }
            }
            else
            {
                done.accept(result);
            }
        }));
    }

    private static String describe(Throwable error)
    {
        if(error instanceof ApiException apiException)
        {
            return apiException.getDetail() != null ? apiException.getDetail() : apiException.getTitle();
        }

        return error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
    }

    private void showMessage(String text, boolean error)
    {
        mMessage.setText(text);
        mMessage.getStyleClass().remove("web-account-error");

        if(error)
        {
            mMessage.getStyleClass().add("web-account-error");
        }
    }

    private Optional<Role> chooseRole(String title, String header, Role initial)
    {
        ChoiceDialog<Role> dialog = new ChoiceDialog<>(initial, Role.values());
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Role");
        dialog.initOwner(getScene().getWindow());
        return dialog.showAndWait();
    }

    private boolean confirm(String title, String header, String content)
    {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content != null ? content : "", ButtonType.OK,
            ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.initOwner(getScene().getWindow());
        return alert.showAndWait().filter(button -> button == ButtonType.OK).isPresent();
    }

    private Dialog<ButtonType> dialog(String title, String header)
    {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.initOwner(getScene().getWindow());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        return dialog;
    }

    private static GridPane grid()
    {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setMaxWidth(460);
        return grid;
    }

    /**
     * Shows a password or token that is shown only once, with a button that copies it
     */
    private void showSecret(String title, String header, String secret, String note)
    {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.initOwner(getScene().getWindow());
        ButtonType copyType = new ButtonType("Copy", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(copyType, ButtonType.CLOSE);

        TextField value = new TextField(secret);
        value.setEditable(false);
        value.setPrefColumnCount(Math.max(24, secret.length()));
        value.setStyle("-fx-font-family: monospace;");
        HBox.setHgrow(value, Priority.ALWAYS);
        Label help = new Label(note);
        help.setWrapText(true);
        help.setMaxWidth(520);
        dialog.getDialogPane().setContent(new VBox(10, value, help));

        Button copy = (Button)dialog.getDialogPane().lookupButton(copyType);
        copy.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(secret);
            Clipboard.getSystemClipboard().setContent(content);
            copy.setText("Copied");
            event.consume();
        });
        Platform.runLater(() -> {
            value.requestFocus();
            value.selectAll();
        });
        dialog.showAndWait();
    }

    private static String format(Long time)
    {
        return time == null ? "" : DATE_TIME.format(Instant.ofEpochMilli(time));
    }

    /**
     * Time since, like the web Admin page: Just now, 4 min ago, 3 h ago, or the date and time
     */
    static String ago(Long time)
    {
        if(time == null)
        {
            return "Never";
        }

        long seconds = Math.max(0, (System.currentTimeMillis() - time) / 1000);

        if(seconds < 60)
        {
            return "Just now";
        }

        if(seconds < 3600)
        {
            return (seconds / 60) + " min ago";
        }

        if(seconds < 86400)
        {
            return (seconds / 3600) + " h ago";
        }

        return format(time);
    }

    /**
     * Account with its activity
     */
    private record AccountRow(UserRecord user, AccountService.Activity activity)
    {
        String status()
        {
            if(user.pendingApproval())
            {
                return "Needs approval";
            }

            if(!user.enabled())
            {
                return "Disabled";
            }

            if(user.lockedUntil() != null && user.lockedUntil() > System.currentTimeMillis())
            {
                return "Locked";
            }

            if(activity.active())
            {
                return "Active now";
            }

            return user.mustChangePassword() ? "Temporary password" : "Enabled";
        }
    }
}
