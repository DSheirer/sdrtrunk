package io.github.dsheirer.gui.playlist.web;

import io.github.dsheirer.web.SdrTrunkWebServer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Playlist editor controls for the embedded web interface. */
public class WebInterfaceEditor extends VBox
{
    private final PasswordField mPassword = new PasswordField();
    private final TextField mBindAddress = new TextField();
    private final Spinner<Integer> mPort = new Spinner<>();
    private final Label mStatus = new Label();
    private final Label mActualBinding = new Label();
    private final ComboBox<SdrTrunkWebServer.WebUser> mUsers = new ComboBox<>();
    private final TextField mUsername = new TextField();
    private final PasswordField mUserPassword = new PasswordField();
    private final ComboBox<String> mUserRole = new ComboBox<>();
    private final CheckBox mUserEnabled = new CheckBox("Enabled");

    public WebInterfaceEditor()
    {
        setPadding(new Insets(20));
        setSpacing(14);
        Label title = new Label("Web Interface");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label help = new Label("Configure the embedded web console, its listening interface, and user accounts. " +
            "Use 0.0.0.0 to listen on every IPv4 network interface. JVM properties or the " +
            "SDRTRUNK_WEB_PASSWORD environment variable continue to override saved settings when supplied.");
        help.setWrapText(true);

        mPassword.setText(SdrTrunkWebServer.getSavedPassword());
        mPassword.setPromptText("Built-in admin password");
        mPassword.setPrefColumnCount(40);
        Button savePassword = new Button("Save Admin Password");
        savePassword.setOnAction(event -> savePassword());
        GridPane access = grid();
        access.add(sectionLabel("Administrator Access"), 0, 0, 2, 1);
        access.add(new Label("Admin password:"), 0, 1);
        access.add(mPassword, 1, 1);
        access.add(savePassword, 1, 2);

        mBindAddress.setText(SdrTrunkWebServer.getSavedBindAddress());
        mBindAddress.setPromptText("127.0.0.1 or 0.0.0.0");
        mBindAddress.setPrefColumnCount(24);
        mPort.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 65535,
            SdrTrunkWebServer.getSavedPort()));
        mPort.setEditable(true);
        Button saveNetwork = new Button("Save Network Settings");
        saveNetwork.setOnAction(event -> saveNetwork());
        Button restart = new Button("Restart Web Interface");
        restart.setOnAction(event -> restart());
        GridPane network = grid();
        network.add(sectionLabel("Listening Interface"), 0, 0, 2, 1);
        network.add(new Label("Bind address:"), 0, 1);
        network.add(mBindAddress, 1, 1);
        network.add(new Label("Port:"), 0, 2);
        network.add(mPort, 1, 2);
        network.add(new Label("Active binding:"), 0, 3);
        network.add(mActualBinding, 1, 3);
        network.add(new HBox(10, saveNetwork, restart), 1, 4);

        configureUserControls();
        GridPane users = grid();
        users.add(sectionLabel("Web Console Users"), 0, 0, 2, 1);
        users.add(new Label("Account:"), 0, 1);
        users.add(mUsers, 1, 1);
        users.add(new Label("Username:"), 0, 2);
        users.add(mUsername, 1, 2);
        users.add(new Label("New password:"), 0, 3);
        users.add(mUserPassword, 1, 3);
        users.add(new Label("Role:"), 0, 4);
        users.add(mUserRole, 1, 4);
        users.add(mUserEnabled, 1, 5);
        Button newUser = new Button("New User");
        newUser.setOnAction(event -> clearUser());
        Button saveUser = new Button("Save User");
        saveUser.setOnAction(event -> saveUser());
        Button deleteUser = new Button("Delete User");
        deleteUser.setOnAction(event -> deleteUser());
        users.add(new HBox(10, newUser, saveUser, deleteUser), 1, 6);
        Label userHelp = new Label("The built-in admin account uses the administrator password above. " +
            "Additional administrators can change settings; operators can use the scanner and playback views.");
        userHelp.setWrapText(true);
        users.add(userHelp, 0, 7, 2, 1);

        refreshUsers();
        refreshStatus();
        getChildren().addAll(title, help, access, new Separator(), network, new Separator(), users, mStatus);
    }

    private static GridPane grid()
    {
        GridPane pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(10);
        return pane;
    }

    private static Label sectionLabel(String text)
    {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        return label;
    }

    private void configureUserControls()
    {
        mUsers.setPrefWidth(360);
        mUsers.setConverter(new StringConverter<>()
        {
            @Override public String toString(SdrTrunkWebServer.WebUser user)
            {
                return user == null ? "Create a new user" : user.username() + " (" + user.role() +
                    (user.enabled() ? ")" : ", disabled)");
            }
            @Override public SdrTrunkWebServer.WebUser fromString(String value) { return null; }
        });
        mUsers.setOnAction(event -> loadSelectedUser());
        mUsername.setPromptText("username");
        mUserPassword.setPromptText("Leave blank to keep current password");
        mUserPassword.setPrefColumnCount(32);
        mUserRole.setItems(FXCollections.observableArrayList("operator", "admin"));
        mUserRole.setValue("operator");
        mUserEnabled.setSelected(true);
    }

    private void savePassword()
    {
        String password = mPassword.getText() == null ? "" : mPassword.getText().trim();
        if(password.isBlank())
        {
            mStatus.setText("Password cannot be empty.");
            return;
        }
        SdrTrunkWebServer.savePassword(password);
        mStatus.setText("Administrator password saved. Existing browser sessions were signed out.");
    }

    private void saveNetwork()
    {
        try
        {
            int port = Integer.parseInt(mPort.getEditor().getText().trim());
            mPort.getValueFactory().setValue(port);
            SdrTrunkWebServer.saveNetworkSettings(mBindAddress.getText(), port);
            mStatus.setText("Network settings saved. Restart the web interface to apply them.");
            refreshStatus();
        }
        catch(Exception e)
        {
            mStatus.setText("Unable to save network settings: " + e.getMessage());
        }
    }

    private void restart()
    {
        try
        {
            SdrTrunkWebServer.restartActive();
            refreshStatus();
            mStatus.setText("Web interface restarted at " + activeAddress() + ".");
        }
        catch(Exception e)
        {
            mStatus.setText("Unable to restart: " + e.getMessage());
        }
    }

    private void refreshStatus()
    {
        mActualBinding.setText(SdrTrunkWebServer.isRunning() ? activeAddress() : "Stopped");
        if(mStatus.getText().isBlank())
        {
            mStatus.setText(SdrTrunkWebServer.isRunning() ? "Status: Running" : "Status: Stopped");
        }
    }

    private String activeAddress()
    {
        String bind = SdrTrunkWebServer.getActualBindAddress();
        int port = SdrTrunkWebServer.getActualPort();
        return formatActiveAddress(bind == null ? SdrTrunkWebServer.getEffectiveBindAddress() : bind,
            port == 0 ? SdrTrunkWebServer.getEffectivePort() : port);
    }

    /**
     * Formats a listener endpoint without accidentally repeating a host that was previously stored as an endpoint.
     */
    static String formatActiveAddress(String value, int port)
    {
        String host = value == null ? "" : value.trim();
        host = host.replaceFirst("^https?://", "").replaceFirst("/+$", "");
        host = host.replaceFirst(":" + port + "$", "");
        host = host.replaceFirst("^([^:]+):\\1$", "$1");
        if(host.contains(":") && !host.startsWith("[") && !host.matches("[^:]+"))
        {
            host = "[" + host + "]";
        }
        return "http://" + host + ":" + port + "/";
    }

    private void refreshUsers()
    {
        mUsers.setItems(FXCollections.observableArrayList(SdrTrunkWebServer.getWebUsers()));
        clearUser();
    }

    private void loadSelectedUser()
    {
        SdrTrunkWebServer.WebUser user = mUsers.getValue();
        if(user == null) { return; }
        mUsername.setText(user.username());
        mUserPassword.clear();
        mUserRole.setValue(user.role());
        mUserEnabled.setSelected(user.enabled());
    }

    private void clearUser()
    {
        mUsers.getSelectionModel().clearSelection();
        mUsers.setValue(null);
        mUsername.clear();
        mUserPassword.clear();
        mUserRole.setValue("operator");
        mUserEnabled.setSelected(true);
    }

    private void saveUser()
    {
        try
        {
            SdrTrunkWebServer.WebUser selected = mUsers.getValue();
            SdrTrunkWebServer.saveWebUser(selected == null ? "" : selected.id(), mUsername.getText(),
                mUserPassword.getText(), mUserRole.getValue(), mUserEnabled.isSelected());
            refreshUsers();
            mStatus.setText("Web user saved.");
        }
        catch(Exception e)
        {
            mStatus.setText("Unable to save user: " + e.getMessage());
        }
    }

    private void deleteUser()
    {
        SdrTrunkWebServer.WebUser selected = mUsers.getValue();
        if(selected == null)
        {
            mStatus.setText("Select a user to delete.");
            return;
        }
        try
        {
            SdrTrunkWebServer.deleteWebUser(selected.id());
            refreshUsers();
            mStatus.setText("Web user deleted.");
        }
        catch(Exception e)
        {
            mStatus.setText("Unable to delete user: " + e.getMessage());
        }
    }
}
