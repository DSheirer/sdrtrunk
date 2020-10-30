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

package io.github.dsheirer.gui.playlist.radioreference;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.rrapi.RadioReferenceException;
import io.github.dsheirer.rrapi.response.Fault;
import io.github.dsheirer.rrapi.type.AuthorizationInformation;
import io.github.dsheirer.service.radioreference.RadioReference;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * Login dialog for radioreference.com with support for testing connection to the service.
 */
public class LoginDialog extends Dialog<AuthorizationInformation>
{
    private final static Logger mLog = LoggerFactory.getLogger(LoginDialog.class);
    private UserPreferences mUserPreferences;
    private TextField mUserNameText;
    private PasswordField mPasswordField;
    private TextField mPasswordText;
    private GridPane mGridPane;
    private CheckBox mShowPasswordCheckBox;
    private CheckBox mStoreLoginCheckBox;
    private Button mTestConnectionButton;
    private IconNode mTestPassIcon;
    private IconNode mTestFailIcon;
    private IconNode mTextExpiredIcon;

    /**
     * Constructs an instance.
     * @param userPreferences for accessing stored user credentials and preferences
     */
    public LoginDialog(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;

        setTitle("sdrtrunk - Radio Reference");
        final DialogPane dialogPane = getDialogPane();
        dialogPane.setContent(getGridPane());
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(dialogButton -> {
            ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();

            if(data == null)
            {
                return null;
            }
            else
            {
                return getAuthorizationInformation();
            }
        });
    }

    private AuthorizationInformation getAuthorizationInformation()
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

        return RadioReference.getAuthorizatonInformation(username, password);
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

            getTestExpiredIcon().setVisible(false);
            GridPane.setConstraints(getTestExpiredIcon(), 2, 4);
            mGridPane.getChildren().add(getTestExpiredIcon());
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
                    getTestExpiredIcon().setVisible(false);

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
                            RadioReference.LoginStatus loginStatus;

                            try
                            {
                                loginStatus = RadioReference.testConnectionWithExp(userName, password);

                                if(loginStatus == RadioReference.LoginStatus.VALID_PREMIUM)
                                {
                                    getTestPassIcon().setVisible(true);
                                    getTestFailIcon().setVisible(false);
                                    getTestExpiredIcon().setVisible(false);
                                }
                                else if (loginStatus == RadioReference.LoginStatus.EXPIRED_PREMIUM)
                                {
                                    getTestPassIcon().setVisible(false);
                                    getTestFailIcon().setVisible(false);
                                    getTestExpiredIcon().setVisible(true);

                                    Alert alert = new Alert(Alert.AlertType.WARNING, "You do not have a valid Radio Reference Premium Subscription (it may have expired).", ButtonType.OK);
                                    alert.setHeaderText("Expired Premium Account");
                                    alert.setTitle("Radio Reference Account issue");
                                    alert.initOwner(((Node)getTestConnectionButton()).getScene().getWindow());
                                    alert.showAndWait();
                                }
                                else if (loginStatus == RadioReference.LoginStatus.INVALID_LOGIN)
                                {
                                    getTestPassIcon().setVisible(false);
                                    getTestFailIcon().setVisible(true);
                                    getTestExpiredIcon().setVisible(false);

                                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please verify username and password", ButtonType.OK);
                                    alert.setHeaderText("Login Failed");
                                    alert.setTitle("Test Failed");
                                    alert.initOwner(((Node)getTestConnectionButton()).getScene().getWindow());
                                    alert.showAndWait();
                                    mLog.error("Login failed. Invalid username or password.  Can't login to radioreference.com");
                                }
                                else
                                {
                                    // Only way to get here is via an exception
                                }
                            }
                            catch(RadioReferenceException rre)
                            {

                                // Set an error state
                                getTestPassIcon().setVisible(false);
                                getTestFailIcon().setVisible(true);
                                getTestExpiredIcon().setVisible(false);

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

                                    Alert alert = new Alert(Alert.AlertType.ERROR, fault.getFaultString(), ButtonType.OK);
                                    alert.setHeaderText(fault.getFaultCode());
                                    alert.setTitle("Test Failed");
                                    alert.initOwner(((Node)getTestConnectionButton()).getScene().getWindow());
                                    alert.showAndWait();
                                    mLog.error("Test failed.  Fault [" + fault.toString() + "] Can't login to radioreference.com");
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


                            getTestConnectionButton().setDisable(false);
                        }
                    });
                }
            });
        }

        return mTestConnectionButton;
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

    private IconNode getTestExpiredIcon()
    {
        if(mTextExpiredIcon == null)
        {
            mTextExpiredIcon = new IconNode(FontAwesome.EXCLAMATION_TRIANGLE);
            mTextExpiredIcon.setIconSize(32);
            mTextExpiredIcon.setFill(Color.ORANGERED);
        }

        return mTextExpiredIcon;
    }
}
