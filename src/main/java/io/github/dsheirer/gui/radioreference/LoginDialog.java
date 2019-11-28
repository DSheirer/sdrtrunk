/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.gui.radioreference;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.rrapi.RadioReferenceException;
import io.github.dsheirer.rrapi.response.Fault;
import io.github.dsheirer.rrapi.type.AuthorizationInformation;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.service.radioreference.RadioReference;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconFontFX;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;

/**
 * Login dialog for radioreference.com with support for testing connection to the service.
 */
public class LoginDialog extends Application
{
    private final static Logger mLog = LoggerFactory.getLogger(LoginDialog.class);
    private UserPreferences mUserPreferences;
    private Stage mStage;
    private VBox mContent;
    private TextField mUserNameText;
    private PasswordField mPasswordField;
    private TextField mPasswordText;
    private GridPane mGridPane;
    private CheckBox mShowPasswordCheckBox;
    private CheckBox mStoreLoginCheckBox;
    private Button mTestConnectionButton;
    private Button mCancelButton;
    private Button mOkButton;
    private HBox mButtonsBox;
    private IconNode mTestPassIcon;
    private IconNode mTestFailIcon;
    private Listener<AuthorizationInformation> mResultsListener;

    /**
     * Constructs an instance.
     * @param userPreferences for accessing stored user credentials and preferences
     * @param listener to receive the authorization information instance created for the login
     */
    public LoginDialog(UserPreferences userPreferences, Listener<AuthorizationInformation> listener)
    {
        mUserPreferences = userPreferences;
        mResultsListener = listener;
        IconFontFX.register(jiconfont.icons.font_awesome.FontAwesome.getIconFont());
    }

    /**
     * Constructs an instance for testing
     */
    public LoginDialog()
    {
        this(new UserPreferences(), new Listener<AuthorizationInformation>()
        {
            @Override
            public void receive(AuthorizationInformation authorizationInformation)
            {
                mLog.debug("Auth Info - User:" + authorizationInformation.getUserName() +
                    " Password:" + authorizationInformation.getPassword());
            }
        });
    }

    /**
     * Shows this dialog if it is either iconified or behind other windows
     */
    public void show()
    {
        if(mStage.isIconified())
        {
            mStage.setIconified(false);
        }
        mStage.show();
        mStage.requestFocus();
        mStage.toFront();
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        mStage = primaryStage;
        mStage.setTitle("Radio Reference Login");
        Scene scene = new Scene(getContent(), 400, 250);
        mStage.setScene(scene);
        mStage.show();
    }

    private Parent getContent()
    {
        if(mContent == null)
        {
            mContent = new VBox();
            mContent.getChildren().add(getGridPane());
            Label filler = new Label();
            filler.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(filler, Priority.ALWAYS);
            mContent.getChildren().addAll(filler, getButtonsBox());
        }

        return mContent;
    }

    private GridPane getGridPane()
    {
        if(mGridPane == null)
        {
            mGridPane = new GridPane();
            mGridPane.setPadding(new Insets(10,10,5,10));
            mGridPane.setHgap(5.0);
            mGridPane.setVgap(5.0);

            Label userNameLabel = new Label("User Name:");
            GridPane.setHalignment(userNameLabel, HPos.RIGHT);
            GridPane.setConstraints(userNameLabel, 0, 0);
            mGridPane.getChildren().add(userNameLabel);

            GridPane.setHgrow(getUserNameText(), Priority.ALWAYS);
            GridPane.setConstraints(getUserNameText(), 1, 0);
            mGridPane.getChildren().add(getUserNameText());

            Label passwordLabel = new Label("Password:");
            GridPane.setHalignment(passwordLabel, HPos.RIGHT);
            GridPane.setConstraints(passwordLabel, 0, 1);
            mGridPane.getChildren().add(passwordLabel);

            boolean show = mUserPreferences.getRadioReferencePreference().getShowPassword();
            getPasswordField().setVisible(!show);
            getPasswordText().setVisible(show);

            GridPane.setHgrow(getPasswordText(), Priority.ALWAYS);
            GridPane.setConstraints(getPasswordText(), 1, 1);
            mGridPane.getChildren().add(getPasswordText());

            GridPane.setHgrow(getPasswordField(), Priority.ALWAYS);
            GridPane.setConstraints(getPasswordField(), 1, 1);
            mGridPane.getChildren().add(getPasswordField());

            GridPane.setConstraints(getShowPasswordCheckBox(), 2, 1);
            mGridPane.getChildren().add(getShowPasswordCheckBox());

            GridPane.setConstraints(getStoreLoginCheckBox(), 1, 2);
            mGridPane.getChildren().add(getStoreLoginCheckBox());

            GridPane.setHalignment(getTestConnectionButton(), HPos.CENTER);
            GridPane.setConstraints(getTestConnectionButton(), 1, 4);
            mGridPane.getChildren().add((getTestConnectionButton()));

            getTestPassIcon().setVisible(false);
            GridPane.setConstraints(getTestPassIcon(), 2, 4);
            mGridPane.getChildren().add(getTestPassIcon());

            getTestFailIcon().setVisible(false);
            GridPane.setConstraints(getTestFailIcon(), 2, 4);
            mGridPane.getChildren().add(getTestFailIcon());
        }

        return mGridPane;
    }

    private TextField getUserNameText()
    {
        if(mUserNameText == null)
        {
            mUserNameText = new TextField();
            mUserNameText.setMaxWidth(Double.MAX_VALUE);

            String userName = mUserPreferences.getRadioReferencePreference().getUserName();
            mUserNameText.setText(userName);
        }

        return mUserNameText;
    }

    private PasswordField getPasswordField()
    {
        if(mPasswordField == null)
        {
            mPasswordField = new PasswordField();
            mPasswordField.setMaxWidth(Double.MAX_VALUE);

            String password = mUserPreferences.getRadioReferencePreference().getPassword();
            mPasswordField.setText(password);
        }

        return mPasswordField;
    }

    private TextField getPasswordText()
    {
        if(mPasswordText == null)
        {
            mPasswordText = new TextField();
            mPasswordText.setMaxWidth(Double.MAX_VALUE);

            String password = mUserPreferences.getRadioReferencePreference().getPassword();
            mPasswordText.setText(password);
        }

        return mPasswordText;
    }

    /**
     * Show (or mask) the password entry field
     */
    private CheckBox getShowPasswordCheckBox()
    {
        if(mShowPasswordCheckBox == null)
        {
            mShowPasswordCheckBox = new CheckBox("Show");
            mShowPasswordCheckBox.setSelected(mUserPreferences.getRadioReferencePreference().getShowPassword());
            mShowPasswordCheckBox.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    boolean show = mShowPasswordCheckBox.isSelected();

                    getPasswordText().setVisible(show);
                    getPasswordField().setVisible(!show);

                    //Transfer the password from the previously visible entry control
                    if(show)
                    {
                        getPasswordText().setText(getPasswordField().getText());
                    }
                    else
                    {
                        getPasswordField().setText(getPasswordText().getText());
                    }

                    mUserPreferences.getRadioReferencePreference().setShowPassword(show);
                }
            });
        }

        return mShowPasswordCheckBox;
    }

    private CheckBox getStoreLoginCheckBox()
    {
        if(mStoreLoginCheckBox == null)
        {
            mStoreLoginCheckBox = new CheckBox("Store Login Credentials");
            mStoreLoginCheckBox.setSelected(mUserPreferences.getRadioReferencePreference().isStoreCredentials());
        }

        return mStoreLoginCheckBox;
    }

    private Button getTestConnectionButton()
    {
        if(mTestConnectionButton == null)
        {
            mTestConnectionButton = new Button("Test Connection");
            mTestConnectionButton.setMaxWidth(Double.MAX_VALUE);
            mTestConnectionButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    getTestConnectionButton().setDisable(true);
                    getTestPassIcon().setVisible(false);
                    getTestFailIcon().setVisible(false);

                    String userName = getUserNameText().getText();
                    String password = getPasswordField().isVisible() ? getPasswordField().getText() : getPasswordText().getText();

                    if(userName == null || userName.isEmpty())
                    {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Please provide a user name", ButtonType.OK);
                        alert.setHeaderText("Invalid User Name");
                        alert.setTitle("Login Credentials Required");
                        alert.initOwner(((Node)getTestConnectionButton()).getScene().getWindow());
                        alert.showAndWait();
                        getTestConnectionButton().setDisable(false);
                        return;
                    }

                    if(password == null || password.isEmpty())
                    {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Please provide a password", ButtonType.OK);
                        alert.setHeaderText("Invalid Password");
                        alert.setTitle("Login Credentials Required");
                        alert.initOwner(((Node)getTestConnectionButton()).getScene().getWindow());
                        alert.showAndWait();
                        getTestConnectionButton().setDisable(false);
                        return;
                    }

                    Platform.runLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            boolean success = false;

                            try
                            {
                                success = RadioReference.testConnection(userName, password);
                            }
                            catch(RadioReferenceException rre)
                            {
                                if(rre.getCause() instanceof ConnectException)
                                {
                                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please check network or radio reference availability", ButtonType.OK);
                                    alert.setHeaderText("No Network Connection");
                                    alert.setTitle("Test Failed");
                                    alert.initOwner(((Node)getTestConnectionButton()).getScene().getWindow());
                                    alert.showAndWait();
                                    mLog.error("No network connection to radioreference.com");
                                }
                                else if(rre.hasFault())
                                {
                                    Fault fault = rre.getFault();

                                    if(fault.getFaultCode() != null && fault.getFaultCode().contentEquals("AUTH"))
                                    {
                                        Alert alert = new Alert(Alert.AlertType.ERROR, "Please verify username and password", ButtonType.OK);
                                        alert.setHeaderText("Login Failed");
                                        alert.setTitle("Test Failed");
                                        alert.initOwner(((Node)getTestConnectionButton()).getScene().getWindow());
                                        alert.showAndWait();
                                        mLog.error("Login failed. Invalid username or password.  Can't login to radioreference.com");
                                    }
                                    else
                                    {
                                        Alert alert = new Alert(Alert.AlertType.ERROR, fault.getFaultString(), ButtonType.OK);
                                        alert.setHeaderText(fault.getFaultCode());
                                        alert.setTitle("Test Failed");
                                        alert.initOwner(((Node)getTestConnectionButton()).getScene().getWindow());
                                        alert.showAndWait();
                                        mLog.error("Test failed.  Fault [" + fault.toString() + "] Can't login to radioreference.com");
                                    }
                                }
                                else
                                {
                                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + rre.getMessage(), ButtonType.OK);
                                    alert.setHeaderText("Unknown Error");
                                    alert.setTitle("Test Failed");
                                    alert.initOwner(((Node)getTestConnectionButton()).getScene().getWindow());
                                    alert.showAndWait();
                                    mLog.error("Error testing connection to radioreference.com", rre);
                                }
                            }

                            getTestPassIcon().setVisible(success);
                            getTestFailIcon().setVisible(!success);
                            getTestConnectionButton().setDisable(false);
                        }
                    });
                }
            });
        }

        return mTestConnectionButton;
    }

    private Button getCancelButton()
    {
        if(mCancelButton == null)
        {
            mCancelButton = new Button("Cancel");
            mCancelButton.setMaxWidth(Double.MAX_VALUE);
            mCancelButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mStage.hide();
                }
            });
        }

        return mCancelButton;
    }

    private Button getOkButton()
    {
        if(mOkButton == null)
        {
            mOkButton = new Button("OK");
            mOkButton.setMaxWidth(Double.MAX_VALUE);
            mOkButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    boolean store = getStoreLoginCheckBox().isSelected();
                    String username = getUserNameText().getText();
                    String password = getPasswordField().isVisible() ? getPasswordField().getText() : getPasswordText().getText();

                    if(store)
                    {
                        mUserPreferences.getRadioReferencePreference().setStoreCredentials(getStoreLoginCheckBox().isSelected());
                        mUserPreferences.getRadioReferencePreference().setUserName(username);
                        mUserPreferences.getRadioReferencePreference().setPassword(password);
                    }
                    else
                    {
                        mUserPreferences.getRadioReferencePreference().removeStoredCredentials();
                    }

                    if(mResultsListener != null)
                    {
                        mResultsListener.receive(RadioReference.getAuthorizatonInformation(username, password));
                    }

                    mStage.hide();
                }
            });
        }

        return mOkButton;
    }

    private HBox getButtonsBox()
    {
        if(mButtonsBox == null)
        {
            mButtonsBox = new HBox();
            mButtonsBox.setPadding(new Insets(5,5,5,5));
            mButtonsBox.setSpacing(5.0);
            HBox.setHgrow(getCancelButton(), Priority.ALWAYS);
            HBox.setHgrow(getOkButton(), Priority.ALWAYS);
            mButtonsBox.getChildren().addAll(getOkButton(), getCancelButton());
        }

        return mButtonsBox;
    }

    private IconNode getTestPassIcon()
    {
        if(mTestPassIcon == null)
        {
            mTestPassIcon = new IconNode(FontAwesome.CHECK);
            mTestPassIcon.setIconSize(32);
            mTestPassIcon.setFill(Color.GREEN);
        }

        return mTestPassIcon;
    }

    private IconNode getTestFailIcon()
    {
        if(mTestFailIcon == null)
        {
            mTestFailIcon = new IconNode(FontAwesome.TIMES);
            mTestFailIcon.setIconSize(32);
            mTestFailIcon.setFill(Color.RED);
        }

        return mTestFailIcon;
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
