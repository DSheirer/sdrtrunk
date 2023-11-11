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

package io.github.dsheirer.gui.playlist.radioreference;

import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.rrapi.RadioReferenceException;
import io.github.dsheirer.rrapi.type.Agency;
import io.github.dsheirer.rrapi.type.AuthorizationInformation;
import io.github.dsheirer.rrapi.type.Country;
import io.github.dsheirer.rrapi.type.CountryInfo;
import io.github.dsheirer.rrapi.type.County;
import io.github.dsheirer.rrapi.type.CountyInfo;
import io.github.dsheirer.rrapi.type.State;
import io.github.dsheirer.rrapi.type.StateInfo;
import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.util.ThreadPool;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RadioReferenceEditor extends BorderPane implements Consumer<AuthorizationInformation>
{
    private static final Logger mLog = LoggerFactory.getLogger(RadioReferenceEditor.class);
    private UserPreferences mUserPreferences;
    private RadioReference mRadioReference;
    private PlaylistManager mPlaylistManager;
    private VBox mTopBox;
    private HBox mCredentialsBox;
    private Label mRRStatusText;
    private TextField mUserNameText;
    private TextField mAccountExpiresText;
    private IconNode mTestPassIcon;
    private IconNode mTestFailIcon;
    private IconNode mTestExpiredIcon;
    private Button mLoginButton;
    private GridPane mComboBoxPane;
    private ComboBox<Country> mCountryComboBox;
    private ComboBox<State> mStateComboBox;
    private ComboBox<County> mCountyComboBox;
    private AgencyEditor mNationalAgencyEditor;
    private AgencyEditor mStateAgencyEditor;
    private AgencyEditor mCountyAgencyEditor;
    private SystemEditor mStateSystemEditor;
    private SystemEditor mCountySystemEditor;
    private TabPane mTabPane;
    private Tab mNationalAgencyTab;
    private Tab mStateAgencyTab;
    private Tab mCountyAgencyTab;
    private Tab mStateSystemTab;
    private Tab mCountySystemTab;

    public RadioReferenceEditor(UserPreferences userPreferences, PlaylistManager playlistManager)
    {
        mUserPreferences = userPreferences;
        mRadioReference = playlistManager.getRadioReference();
        mPlaylistManager = playlistManager;

        setTop(getTopBox());
        setCenter(getTabPane());

        login();

        if(mRadioReference.premiumAccountProperty().get())
        {
            refreshCountries();
        }

        //Refresh the countries combo box once we're logged on, if not already
        mRadioReference.premiumAccountProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue)
            {
                refreshCountries();
            }
        });
    }

    private void login()
    {
        if(mUserPreferences.getRadioReferencePreference().hasStoredCredentials())
        {
            AuthorizationInformation credentials = mUserPreferences.getRadioReferencePreference().getAuthorizationInformation();

            if(credentials != null)
            {
                ThreadPool.CACHED.execute(() -> accept(credentials));
            }
        }
    }

    /**
     * Retrieves the list of countries supported by the service and populates the combobox, auto-selecting the
     * country that the user last selected.
     */
    private void refreshCountries()
    {
        getCountryComboBox().getItems().clear();

        final int preferredCountryId = mUserPreferences.getRadioReferencePreference().getPreferredCountryId();

        ThreadPool.CACHED.execute(() -> {
            try
            {
                List<Country> countries = mRadioReference.getService().getCountries();

                Platform.runLater(() -> {
                    getCountryComboBox().getItems().addAll(countries);

                    if(preferredCountryId >= 0)
                    {
                        for(Country country: countries)
                        {
                            if(country.getCountryId() == preferredCountryId)
                            {
                                getCountryComboBox().getSelectionModel().select(country);
                                return;
                            }
                        }
                    }
                });
            }
            catch(Throwable t)
            {
                mLog.error("Error retrieving country list from radio reference", t);
                Platform.runLater(() -> new RadioReferenceUnavailableAlert(getCountryComboBox()).showAndWait());
            }
        });
    }

    private TabPane getTabPane()
    {
        if(mTabPane == null)
        {
            mTabPane = new TabPane();
            mTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            mTabPane.getTabs().addAll(getCountySystemTab(), getStateSystemTab(), getCountyAgencyTab(),
                getStateAgencyTab(), getNationalAgencyTab());
            mTabPane.disableProperty().bind(mRadioReference.premiumAccountProperty().not());
        }

        return mTabPane;
    }

    private Tab getNationalAgencyTab()
    {
        if(mNationalAgencyTab == null)
        {
            mNationalAgencyTab = new Tab("National Agencies");
            mNationalAgencyTab.setContent(getNationalAgencyEditor());
            getNationalAgencyEditor().agencyCountProperty().addListener((observable, oldValue, newValue) -> {
                int count = (newValue != null ? newValue.intValue() : 0);

                if(count > 0)
                {
                    getNationalAgencyTab().setText("National Agencies (" + count + ")");
                }
                else
                {
                    getNationalAgencyTab().setText("National Agencies");
                }
            });
        }

        return mNationalAgencyTab;
    }

    private Tab getStateAgencyTab()
    {
        if(mStateAgencyTab == null)
        {
            mStateAgencyTab = new Tab("State Agencies");
            mStateAgencyTab.setContent(getStateAgencyEditor());
            getStateAgencyEditor().agencyCountProperty().addListener((observable, oldValue, newValue) -> {
                int count = (newValue != null ? newValue.intValue() : 0);

                if(count > 0)
                {
                    getStateAgencyTab().setText("State Agencies (" + count + ")");
                }
                else
                {
                    getStateAgencyTab().setText("State Agencies");
                }
            });
        }

        return mStateAgencyTab;
    }

    private Tab getCountyAgencyTab()
    {
        if(mCountyAgencyTab == null)
        {
            mCountyAgencyTab = new Tab("County Agencies");
            mCountyAgencyTab.setContent(getCountyAgencyEditor());
            getCountyAgencyEditor().agencyCountProperty().addListener((observable, oldValue, newValue) -> {
                int count = (newValue != null ? newValue.intValue() : 0);

                if(count > 0)
                {
                    getCountyAgencyTab().setText("County Agencies (" + count + ")");
                }
                else
                {
                    getCountyAgencyTab().setText("County Agencies");
                }
            });
        }

        return mCountyAgencyTab;
    }

    private Tab getStateSystemTab()
    {
        if(mStateSystemTab == null)
        {
            mStateSystemTab = new Tab("State Trunked Systems");
            mStateSystemTab.setContent(getStateSystemEditor());
            getStateSystemEditor().systemCountProperty().addListener((observable, oldValue, newValue) -> {
                int count = (newValue != null ? newValue.intValue() : 0);

                if(count > 0)
                {
                    getStateSystemTab().setText("State Trunked Systems (" + count + ")");
                }
                else
                {
                    getStateSystemTab().setText("State Trunked Systems");
                }
            });
        }

        return mStateSystemTab;
    }

    private Tab getCountySystemTab()
    {
        if(mCountySystemTab == null)
        {
            mCountySystemTab = new Tab("County Trunked Systems");
            mCountySystemTab.setContent(getCountySystemEditor());
            getCountySystemEditor().systemCountProperty().addListener((observable, oldValue, newValue) -> {
                int count = (newValue != null ? newValue.intValue() : 0);

                if(count > 0)
                {
                    getCountySystemTab().setText("County Trunked Systems (" + count + ")");
                }
                else
                {
                    getCountySystemTab().setText("County Trunked Systems");
                }
            });
        }

        return mCountySystemTab;
    }

    private AgencyEditor getNationalAgencyEditor()
    {
        if(mNationalAgencyEditor == null)
        {
            mNationalAgencyEditor = new AgencyEditor(mUserPreferences, mRadioReference, mPlaylistManager, Level.NATIONAL);
        }

        return mNationalAgencyEditor;
    }

    private AgencyEditor getStateAgencyEditor()
    {
        if(mStateAgencyEditor == null)
        {
            mStateAgencyEditor = new AgencyEditor(mUserPreferences, mRadioReference, mPlaylistManager, Level.STATE);
        }

        return mStateAgencyEditor;
    }

    private AgencyEditor getCountyAgencyEditor()
    {
        if(mCountyAgencyEditor == null)
        {
            mCountyAgencyEditor = new AgencyEditor(mUserPreferences, mRadioReference, mPlaylistManager, Level.COUNTY);
        }

        return mCountyAgencyEditor;
    }

    private SystemEditor getStateSystemEditor()
    {
        if(mStateSystemEditor == null)
        {
            mStateSystemEditor = new SystemEditor(mUserPreferences, mRadioReference, mPlaylistManager, Level.STATE);
        }

        return mStateSystemEditor;
    }

    private SystemEditor getCountySystemEditor()
    {
        if(mCountySystemEditor == null)
        {
            mCountySystemEditor = new SystemEditor(mUserPreferences, mRadioReference, mPlaylistManager, Level.COUNTY);
        }

        return mCountySystemEditor;
    }

    private VBox getTopBox()
    {
        if(mTopBox == null)
        {
            mTopBox = new VBox();
            mTopBox.setSpacing(5);
            mTopBox.setPadding(new Insets(10,10,10,10));
            mTopBox.getChildren().addAll(getCredentialsBox(), getComboBoxPane());
        }

        return mTopBox;
    }

    private GridPane getComboBoxPane()
    {
        if(mComboBoxPane == null)
        {
            mComboBoxPane = new GridPane();
            mComboBoxPane.setHgap(5);
            mComboBoxPane.setVgap(2);

            ColumnConstraints column1 = new ColumnConstraints();
            column1.setPercentWidth(33.33);
            ColumnConstraints column2 = new ColumnConstraints();
            column2.setPercentWidth(33.33);
            ColumnConstraints column3 = new ColumnConstraints();
            column3.setPercentWidth(33.33);
            mComboBoxPane.getColumnConstraints().addAll(column1, column2, column3);

            Label countryLabel = new Label("Country");
            GridPane.setConstraints(countryLabel, 0, 0);
            mComboBoxPane.getChildren().add(countryLabel);

            GridPane.setConstraints(getCountryComboBox(), 0, 1);
            GridPane.setHgrow(getCountryComboBox(), Priority.ALWAYS);
            mComboBoxPane.getChildren().add(getCountryComboBox());

            Label stateLabel = new Label("State");
            GridPane.setConstraints(stateLabel, 1, 0);
            mComboBoxPane.getChildren().add(stateLabel);

            GridPane.setConstraints(getStateComboBox(), 1, 1);
            GridPane.setHgrow(getStateComboBox(), Priority.ALWAYS);
            mComboBoxPane.getChildren().add(getStateComboBox());

            Label countyLabel = new Label("County");
            GridPane.setConstraints(countyLabel, 2, 0);
            mComboBoxPane.getChildren().add(countyLabel);

            GridPane.setConstraints(getCountyComboBox(), 2, 1);
            GridPane.setHgrow(getCountyComboBox(), Priority.ALWAYS);
            mComboBoxPane.getChildren().add(getCountyComboBox());

            mComboBoxPane.disableProperty().bind(mRadioReference.premiumAccountProperty().not());
        }

        return mComboBoxPane;
    }

    private HBox getCredentialsBox()
    {
        if(mCredentialsBox == null)
        {
            mCredentialsBox = new HBox();
            mCredentialsBox.setAlignment(Pos.CENTER_RIGHT);
            mCredentialsBox.setSpacing(5.0);
            Region leftFiller = new Region();
            HBox.setHgrow(leftFiller, Priority.ALWAYS);
            mCredentialsBox.getChildren().add(leftFiller);
            mCredentialsBox.getChildren().add(getRRPremiumReqText());
            Region rightFiller = new Region();
            HBox.setHgrow(rightFiller, Priority.ALWAYS);
            mCredentialsBox.getChildren().add(rightFiller);
            mCredentialsBox.getChildren().add(new Label("User Name:"));
            mCredentialsBox.getChildren().add(getUserNameText());
            mCredentialsBox.getChildren().add(new Label("Expires:"));
            mCredentialsBox.getChildren().add(getAccountExpiresText());
            mCredentialsBox.getChildren().add(getTestFailIcon());
            mCredentialsBox.getChildren().add(getTestPassIcon());
            mCredentialsBox.getChildren().add(getTestExpiredIcon());
            mCredentialsBox.getChildren().add(getLoginButton());
        }

        return mCredentialsBox;
    }

    private Label getRRPremiumReqText()
    {
        if(mRRStatusText == null)
        {
            mRRStatusText = new Label();
            mRRStatusText.setText("A premium Radio Reference subscription is required to import systems.");
            mRRStatusText.visibleProperty().bind(
                    Bindings.and(
                            mRadioReference.availableProperty(),
                            mRadioReference.premiumAccountProperty().not()));
            mRRStatusText.setTextFill(Color.ORANGERED);
            mRRStatusText.setStyle("-fx-font-weight: bold;"); // I guess
        }

        return mRRStatusText;
    }

    private TextField getUserNameText()
    {
        if(mUserNameText == null)
        {
            mUserNameText = new TextField();
            mUserNameText.textProperty().bind(mRadioReference.userNameProperty());
            mUserNameText.setDisable(true);
        }

        return mUserNameText;
    }

    private TextField getAccountExpiresText()
    {
        if(mAccountExpiresText == null)
        {
            mAccountExpiresText = new TextField();
            mAccountExpiresText.textProperty().bind(mRadioReference.accountExpiresProperty());
            mAccountExpiresText.setDisable(true);
        }

        return mAccountExpiresText;
    }

    private IconNode getTestPassIcon()
    {
        if(mTestPassIcon == null)
        {
            mTestPassIcon = new IconNode(FontAwesome.CHECK);
            mTestPassIcon.setFill(Color.GREEN);
            mTestPassIcon.setVisible(false);
            // Need to have a valid account, _and_ have premium
            mTestPassIcon.visibleProperty().bind(
                    Bindings.and(
                            mRadioReference.availableProperty(),
                            mRadioReference.premiumAccountProperty()));
        }

        return mTestPassIcon;
    }

    private IconNode getTestFailIcon()
    {
        if(mTestFailIcon == null)
        {
            mTestFailIcon = new IconNode(FontAwesome.TIMES);
            mTestFailIcon.setFill(Color.RED);
            mTestFailIcon.setVisible(false);
            mTestFailIcon.visibleProperty().bind(mRadioReference.availableProperty().not());
        }

        return mTestFailIcon;
    }

    private IconNode getTestExpiredIcon()
    {
        if(mTestExpiredIcon == null)
        {
            mTestExpiredIcon = new IconNode(FontAwesome.EXCLAMATION_TRIANGLE);
            mTestExpiredIcon.setFill(Color.ORANGERED);
            // Need to be signed in, but _not_ have a valid premium subscription
            mTestExpiredIcon.visibleProperty().bind(
                    Bindings.and(
                            mRadioReference.availableProperty(),
                            mRadioReference.premiumAccountProperty().not()));
        }

        return mTestExpiredIcon;
    }

    private Button getLoginButton()
    {
        if(mLoginButton == null)
        {
            mLoginButton = new Button("Login");
            IconNode configureIcon = new IconNode(FontAwesome.COG);
            configureIcon.setFill(Color.GRAY);
            mLoginButton.setGraphic(configureIcon);
            mLoginButton.setOnAction(event -> {
                new LoginDialog(mUserPreferences).showAndWait().ifPresent(this);
            });
        }

        return mLoginButton;
    }

    private void clearEditors(Level level)
    {
        switch(level)
        {
            case NATIONAL:
                getNationalAgencyEditor().clear();
                getStateAgencyEditor().clear();
                getCountyAgencyEditor().clear();
                getStateSystemEditor().clear();
                getCountySystemEditor().clear();
                break;
            case STATE:
                getStateAgencyEditor().clear();
                getCountyAgencyEditor().clear();
                getStateSystemEditor().clear();
                getCountySystemEditor().clear();
                break;
            case COUNTY:
                getCountyAgencyEditor().clear();
                getCountySystemEditor().clear();
                break;
        }
    }

    private void setCountry(Country country)
    {
        clearEditors(Level.NATIONAL);
        getStateComboBox().getItems().clear();
        getCountyComboBox().getItems().clear();

        if(country != null)
        {
            int preferredStateId = mUserPreferences.getRadioReferencePreference().getPreferredStateId();

            ThreadPool.CACHED.execute(() -> {
                try
                {
                    final CountryInfo countryInfo = mRadioReference.getService().getCountryInfo(country.getCountryId());

                    Platform.runLater(() -> {
                        getNationalAgencyEditor().setAgencies(countryInfo.getAgencies());
                        getStateComboBox().getItems().addAll(countryInfo.getStates());

                        for(State state: mStateComboBox.getItems())
                        {
                            if(state.getStateId() == preferredStateId)
                            {
                                getStateComboBox().getSelectionModel().select(state);
                                return;
                            }
                        }
                    });
                }
                catch(RadioReferenceException rre)
                {
                    mLog.error("Error retrieving country information from radio reference - " + rre.getMessage());
                    Platform.runLater(() -> new RadioReferenceUnavailableAlert(getCountryComboBox()).showAndWait());
                }
            });
        }
    }

    private ComboBox<Country> getCountryComboBox()
    {
        if(mCountryComboBox == null)
        {
            mCountryComboBox = new ComboBox<>();
            mCountryComboBox.setConverter(new CountryStringConverter());
            mCountryComboBox.setMaxWidth(Double.MAX_VALUE);
            mCountryComboBox.setOnAction(event -> {
                Country selected = mCountryComboBox.getValue();
                setCountry(selected);

                if(selected != null)
                {
                    mUserPreferences.getRadioReferencePreference().setPreferredCountryId(selected.getCountryId());
                }
            });
        }

        return mCountryComboBox;
    }

    private void setState(State state)
    {
        clearEditors(Level.STATE);
        getCountyComboBox().getItems().clear();

        int preferredCountyId = mUserPreferences.getRadioReferencePreference().getPreferredCountyId();

        if(state != null)
        {
            ThreadPool.CACHED.execute(() -> {
                try
                {
                    final StateInfo stateInfo = mRadioReference.getService().getStateInfo(state.getStateId());

                    Platform.runLater(() -> {
                        getStateAgencyEditor().setAgencies(stateInfo.getAgencies());
                        getStateSystemEditor().setSystems(stateInfo.getSystems());
                        getCountyComboBox().getItems().addAll(stateInfo.getCounties());

                        for(County county: mCountyComboBox.getItems())
                        {
                            if(county.getCountyId() == preferredCountyId)
                            {
                                getCountyComboBox().getSelectionModel().select(county);
                                return;
                            }
                        }
                    });
                }
                catch(RadioReferenceException rre)
                {
                    mLog.error("Error retrieving state information from radio reference - " + rre.getMessage());
                    Platform.runLater(() -> new RadioReferenceUnavailableAlert(getStateComboBox()).showAndWait());
                }
            });
        }
    }

    private ComboBox<State> getStateComboBox()
    {
        if(mStateComboBox == null)
        {
            mStateComboBox = new ComboBox<>();
            mStateComboBox.setConverter(new StateStringConverter());
            mStateComboBox.setMaxWidth(Double.MAX_VALUE);
            mStateComboBox.setOnAction(event -> {
                State selected = mStateComboBox.getValue();
                setState(selected);

                if(selected != null)
                {
                    mUserPreferences.getRadioReferencePreference().setPreferredStateId(selected.getStateId());
                }
            });
        }

        return mStateComboBox;
    }

    private void setCounty(County county)
    {
        clearEditors(Level.COUNTY);

        if(county != null)
        {
            ThreadPool.CACHED.execute(() -> {
                try
                {
                    final CountyInfo countyInfo = mRadioReference.getService().getCountyInfo(county.getCountyId());

                    Platform.runLater(() -> {
                        getCountySystemEditor().setSystems(countyInfo.getSystems());

                        // Make a new list so we don't add our CountyAgency to the original list of agencies
                        List<Agency> countyAgencies = new ArrayList<Agency>();
                        countyAgencies.add(new CountyAgency(countyInfo));
                        countyAgencies.addAll(countyInfo.getAgencies());
                        getCountyAgencyEditor().setAgencies(countyAgencies);
                    });
                }
                catch(RadioReferenceException rre)
                {
                    mLog.error("Error retrieving county information from radio reference - " + rre.getMessage());
                    Platform.runLater(() -> new RadioReferenceUnavailableAlert(getCountyComboBox()).showAndWait());
                }
            });
        }
    }

    private ComboBox<County> getCountyComboBox()
    {
        if(mCountyComboBox == null)
        {
            mCountyComboBox = new ComboBox<>();
            mCountyComboBox.setConverter(new CountyStringConverter());
            mCountyComboBox.setMaxWidth(Double.MAX_VALUE);
            mCountyComboBox.setOnAction(event -> {
                County selected = mCountyComboBox.getValue();
                setCounty(selected);

                if(selected != null)
                {
                    mUserPreferences.getRadioReferencePreference().setPreferredCountyId(selected.getCountyId());
                }
            });
        }

        return mCountyComboBox;
    }


    /**
     * Consumer interface method for receiving login credentials
     */
    @Override
    public void accept(AuthorizationInformation authorizationInformation)
    {
        if(authorizationInformation != null)
        {
            mRadioReference.setAuthorizationInformation(authorizationInformation);
        }
    }

    public class CountryStringConverter extends StringConverter<Country>
    {
        @Override
        public String toString(Country country)
        {
            if(country != null)
            {
                return country.getName();
            }

            return null;
        }

        @Override
        public Country fromString(String string)
        {
            if(string != null && !string.isEmpty())
            {
                for(Country country: mCountryComboBox.getItems())
                {
                    if(country.getName().toLowerCase().contentEquals(string.toLowerCase()))
                    {
                        return country;
                    }
                }
            }

            return null;
        }
    }

    public class StateStringConverter extends StringConverter<State>
    {
        @Override
        public String toString(State state)
        {
            if(state != null)
            {
                return state.getName();
            }

            return null;
        }

        @Override
        public State fromString(String string)
        {
            if(string != null && !string.isEmpty())
            {
                for(State state: mStateComboBox.getItems())
                {
                    if(state.getName().toLowerCase().contentEquals(string.toLowerCase()))
                    {
                        return state;
                    }
                }
            }

            return null;
        }
    }

    public class CountyStringConverter extends StringConverter<County>
    {
        @Override
        public String toString(County county)
        {
            if(county != null)
            {
                return county.getName();
            }

            return null;
        }

        @Override
        public County fromString(String string)
        {
            if(string != null && !string.isEmpty())
            {
                for(County county: mCountyComboBox.getItems())
                {
                    if(county.getName().toLowerCase().contentEquals(string.toLowerCase()))
                    {
                        return county;
                    }
                }
            }

            return null;
        }
    }
}
