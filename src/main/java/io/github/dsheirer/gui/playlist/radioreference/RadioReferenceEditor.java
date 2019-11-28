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

package io.github.dsheirer.gui.playlist.radioreference;

import io.github.dsheirer.gui.JavaFxWindowManager;
import io.github.dsheirer.gui.radioreference.LoginDialogViewRequest;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.radioreference.RadioReferencePreference;
import io.github.dsheirer.rrapi.RadioReferenceException;
import io.github.dsheirer.rrapi.type.Agency;
import io.github.dsheirer.rrapi.type.AgencyInfo;
import io.github.dsheirer.rrapi.type.AuthorizationInformation;
import io.github.dsheirer.rrapi.type.Category;
import io.github.dsheirer.rrapi.type.Country;
import io.github.dsheirer.rrapi.type.CountryInfo;
import io.github.dsheirer.rrapi.type.County;
import io.github.dsheirer.rrapi.type.CountyInfo;
import io.github.dsheirer.rrapi.type.Flavor;
import io.github.dsheirer.rrapi.type.Mode;
import io.github.dsheirer.rrapi.type.State;
import io.github.dsheirer.rrapi.type.StateInfo;
import io.github.dsheirer.rrapi.type.System;
import io.github.dsheirer.rrapi.type.Tag;
import io.github.dsheirer.rrapi.type.Type;
import io.github.dsheirer.rrapi.type.UserInfo;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.util.ThreadPool;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.StringConverter;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconFontFX;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RadioReferenceEditor extends BorderPane implements Listener<AuthorizationInformation>
{
    private static final Logger mLog = LoggerFactory.getLogger(RadioReferenceEditor.class);
    public static final String AGENCY_LABEL = "Agency:";
    public static final String COUNTY_LABEL = "County:";

    private UserPreferences mUserPreferences;
    private RadioReference mRadioReference;
    private JavaFxWindowManager mJavaFxWindowManager;
    private HBox mTopBox;
    private HBox mCredentialsBox;
    private TextField mUserNameText;
    private TextField mAccountExpiresText;
    private IconNode mTestPassIcon;
    private IconNode mTestFailIcon;
    private Button mLoginButton;
    private HBox mCountryBox;
    private ComboBox<Country> mCountryComboBox;
    private SplitPane mSplitPane;
    private HBox mCountryEntityBox;
    private ListView<State> mStateListView;
    private ListView<Agency> mCountryAgencyListView;
    private HBox mStateEntityBox;
    private ListView<County> mCountyListView;
    private ListView<Agency> mStateAgencyListView;
    private HBox mCountyEntityBox;
    private ListView<System> mSystemListView;
    private ListView<Agency> mCountyAgencyListView;
    private Map<Integer,Flavor> mFlavorMap = new TreeMap<>();
    private Map<Integer, Mode> mModesMap = new TreeMap<>();
    private Map<Integer,Type> mTypesMap = new TreeMap<>();
    private Map<Integer,Tag> mTagsMap = new TreeMap<>();
    private FrequencyTableView mFrequencyTableView;
    private TrunkedSystemView mTrunkedSystemView;

    public RadioReferenceEditor(UserPreferences userPreferences, RadioReference radioReference, JavaFxWindowManager manager)
    {
        mUserPreferences = userPreferences;
        mRadioReference = radioReference;
        mJavaFxWindowManager = manager;

        IconFontFX.register(jiconfont.icons.font_awesome.FontAwesome.getIconFont());

        setTop(getTopBox());
        setCenter(getSplitPane());
        checkAccount();
        refreshLookupTables();
        refreshCountries();
    }

    private void checkAccount()
    {
        if(mRadioReference.hasCredentials())
        {
            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        UserInfo userInfo = mRadioReference.getService().getUserInfo();
                        getUserNameText().setText(userInfo.getUserName());
                        getAccountExpiresText().setText(userInfo.getExpirationDate());
                        mRadioReference.loggedOnProperty().set(true);
                    }
                    catch(RadioReferenceException rre)
                    {
                        getAccountExpiresText().setText(null);
                        mRadioReference.loggedOnProperty().set(false);
                    }
                }
            });
        }
    }

    private void refreshLookupTables()
    {
        if(mRadioReference.hasCredentials())
        {
            mFlavorMap.clear();

            ThreadPool.SCHEDULED.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Map<Integer,Flavor> flavorMap = mRadioReference.getService().getFlavorsMap();
                        mFlavorMap.putAll(flavorMap);
                        Map<Integer,Mode> modesMap = mRadioReference.getService().getModesMap();
                        mModesMap.putAll(modesMap);
                        Map<Integer,Type> typesMap = mRadioReference.getService().getTypesMap();
                        mTypesMap.putAll(typesMap);
                        Map<Integer,Tag> tagsMap = mRadioReference.getService().getTagsMap();
                        mTagsMap.putAll(tagsMap);
                    }
                    catch(RadioReferenceException rre)
                    {
                        mLog.error("Error refreshing flavors, modes, types and tags lookup maps");
                    }
                }
            });
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

        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    List<Country> countries = mRadioReference.getService().getCountries();
                    getCountryComboBox().getItems().addAll(countries);

                    if(preferredCountryId >= 0)
                    {
                        for(Country country: countries)
                        {
                            if(country.getCountryId() == preferredCountryId)
                            {
                                getCountryComboBox().getSelectionModel().select(country);
                                continue;
                            }
                        }
                    }
                }
                catch(RadioReferenceException rre)
                {
                    mLog.error("Error retrieving country list from radioreference", rre);
                }
            }
        });
    }

    private void setCountry(final Country country)
    {
        getStateListView().getItems().clear();
        getCountryAgencyListView().getItems().clear();
        getFrequencyTableView().update(null, null, Collections.emptyList());
        getTrunkedSystemView().setSystem(null);

        final int preferredStateId = mUserPreferences.getRadioReferencePreference().getPreferredStateId();
        final int preferredAgencyId = mUserPreferences.getRadioReferencePreference().getPreferredAgencyId();

        if(country != null && mRadioReference.hasCredentials())
        {
            ThreadPool.SCHEDULED.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        CountryInfo countryInfo = mRadioReference.getService().getCountryInfo(country);
                        List<State> states = countryInfo.getStates();
                        Collections.sort(states, new Comparator<State>()
                        {
                            @Override
                            public int compare(State o1, State o2)
                            {
                                return o1.getName().compareTo(o2.getName());
                            }
                        });
                        List<Agency> agencies = countryInfo.getAgencies();
                        Collections.sort(agencies, new Comparator<Agency>()
                        {
                            @Override
                            public int compare(Agency o1, Agency o2)
                            {
                                return o1.getName().compareTo(o2.getName());
                            }
                        });

                        if(states != null || agencies != null)
                        {
                            Platform.runLater(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if(states != null && !states.isEmpty())
                                    {
                                        getStateListView().getItems().addAll(states);

                                        if(preferredStateId >= 0)
                                        {
                                            for(State state: states)
                                            {
                                                if(state.getStateId() == preferredStateId)
                                                {
                                                    getStateListView().getSelectionModel().select(state);
                                                    getStateListView().scrollTo(state);
                                                    continue;
                                                }
                                            }
                                        }
                                    }

                                    if(agencies != null && !agencies.isEmpty())
                                    {
                                        getCountryAgencyListView().getItems().addAll(agencies);

                                        if(preferredAgencyId >= 0)
                                        {
                                            for(Agency agency: agencies)
                                            {
                                                if(agency.getAgencyId() == preferredAgencyId)
                                                {
                                                    getCountryAgencyListView().getSelectionModel().select(agency);
                                                    getCountryAgencyListView().scrollTo(agency);
                                                }
                                            }
                                        }
                                    }
                                }
                            });

                        }
                    }
                    catch(RadioReferenceException rre)
                    {
                        mLog.error("Error retrieving country info for " + country.getName());
                    }
                }
            });
        }
    }

    private void setState(State state)
    {
        getCountyListView().getItems().clear();
        getStateAgencyListView().getItems().clear();

        final int preferredCountyId = mUserPreferences.getRadioReferencePreference().getPreferredCountyId();
        final int preferredAgencyId = mUserPreferences.getRadioReferencePreference().getPreferredAgencyId();

        if(state != null && mRadioReference.hasCredentials())
        {
            ThreadPool.SCHEDULED.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        StateInfo stateInfo = mRadioReference.getService().getStateInfo(state.getStateId());

                        List<County> counties = stateInfo.getCounties();

                        //Pre-cache county information instances
                        ThreadPool.SCHEDULED.submit(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    for(County county: counties)
                                    {
                                        mRadioReference.getService().getCountyInfo(county.getCountyId());
                                    }
                                }
                                catch(RadioReferenceException rre)
                                {
                                    //Do nothing, this just an attempt to pre-cache the counties
                                }
                            }
                        });

                        Collections.sort(counties, new Comparator<County>()
                        {
                            @Override
                            public int compare(County o1, County o2)
                            {
                                return o1.getName().compareTo(o2.getName());
                            }
                        });

                        List<Agency> agencies = stateInfo.getAgencies();
                        Collections.sort(agencies, new Comparator<Agency>()
                        {
                            @Override
                            public int compare(Agency o1, Agency o2)
                            {
                                return o1.getName().compareTo(o2.getName());
                            }
                        });



                        if(counties != null || agencies != null)
                        {
                            Platform.runLater(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if(counties != null && !counties.isEmpty())
                                    {
                                        getCountyListView().getItems().addAll(counties);

                                        if(preferredCountyId >= 0)
                                        {
                                            for(County county: counties)
                                            {
                                                if(county.getCountyId() == preferredCountyId)
                                                {
                                                    getCountyListView().getSelectionModel().select(county);
                                                    getCountyListView().scrollTo(county);
                                                    continue;
                                                }
                                            }
                                        }
                                    }

                                    if(agencies != null && !agencies.isEmpty())
                                    {
                                        getStateAgencyListView().getItems().addAll(agencies);

                                        if(preferredAgencyId >= 0)
                                        {
                                            for(Agency agency: agencies)
                                            {
                                                if(agency.getAgencyId() == preferredAgencyId)
                                                {
                                                    getStateAgencyListView().getSelectionModel().select(agency);
                                                    getStateAgencyListView().scrollTo(agency);
                                                }
                                            }
                                        }
                                    }
                                }
                            });

                        }
                    }
                    catch(RadioReferenceException rre)
                    {
                        mLog.error("Error retrieving country info for " + state.getName(), rre);
                    }
                }
            });
        }
    }

    private void setCounty(County county)
    {
        getSystemListView().getItems().clear();
        getCountyAgencyListView().getItems().clear();
        final int preferredSystemId = mUserPreferences.getRadioReferencePreference().getPreferredSystemId();
        final int preferredAgencyId = mUserPreferences.getRadioReferencePreference().getPreferredAgencyId();

        if(county != null && mRadioReference.hasCredentials())
        {
            ThreadPool.SCHEDULED.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        CountyInfo countyInfo = mRadioReference.getService().getCountyInfo(county.getCountyId());
                        setFrequencyViewCategories(COUNTY_LABEL, countyInfo.getName(), countyInfo.getCategories());
                        List<System> systems = countyInfo.getSystems();
                        Collections.sort(systems, new Comparator<System>()
                        {
                            @Override
                            public int compare(System o1, System o2)
                            {
                                return o1.getName().compareTo(o2.getName());
                            }
                        });

                        List<Agency> agencies = countyInfo.getAgencies();
                        Collections.sort(agencies, new Comparator<Agency>()
                        {
                            @Override
                            public int compare(Agency o1, Agency o2)
                            {
                                return o1.getName().compareTo(o2.getName());
                            }
                        });

                        if(systems != null || agencies != null)
                        {
                            Platform.runLater(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if(systems != null && !systems.isEmpty())
                                    {
                                        getSystemListView().getItems().addAll(systems);

                                        if(preferredSystemId >= 0)
                                        {
                                            for(System system: systems)
                                            {
                                                if(system.getSystemId() == preferredSystemId)
                                                {
                                                    getSystemListView().getSelectionModel().select(system);
                                                    getSystemListView().scrollTo(system);
                                                    continue;
                                                }
                                            }
                                        }
                                    }

                                    if(agencies != null && !agencies.isEmpty())
                                    {
                                        getCountyAgencyListView().getItems().addAll(agencies);

                                        if(preferredAgencyId >= 0)
                                        {
                                            for(Agency agency: agencies)
                                            {
                                                if(agency.getAgencyId() == preferredAgencyId)
                                                {
                                                    getCountyAgencyListView().getSelectionModel().select(agency);
                                                    getCountyAgencyListView().scrollTo(agency);
                                                }
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                    catch(RadioReferenceException rre)
                    {
                        mLog.error("Error retrieving country info for " + county.getName(), rre);
                    }
                }
            });
        }
    }

    /**
     * Sets the argument as the results view node in the lower half of the split pane
     * @param node to view
     */
    private void setResultsView(Node node)
    {
        if(getSplitPane().getItems().size() == 2)
        {
            getSplitPane().getItems().remove(1);
            getSplitPane().getItems().add(node);
        }
        else
        {
            mLog.error("Error - expected 2 nodes in split pane but encountered: " + getSplitPane().getItems().size());
        }
    }

    /**
     * Updates the frequency list view with the label and value and updates the categories list
     * @param label to use for the value
     * @param value of the agency name
     * @param categories categories for an agency
     */
    private void setFrequencyViewCategories(String label, String value, List<Category> categories)
    {
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                setResultsView(getFrequencyTableView());
                getFrequencyTableView().clear();
                getFrequencyTableView().update(label, value, categories);
            }
        };

        if(Platform.isFxApplicationThread())
        {
            runnable.run();
        }
        else
        {
            Platform.runLater(runnable);
        }
    }

    private void setSystem(System system)
    {
        if(system != null)
        {
            mUserPreferences.getRadioReferencePreference().setPreferredAgencyId(RadioReferencePreference.INVALID_ID);
            mUserPreferences.getRadioReferencePreference().setPreferredSystemId(system.getSystemId());
            setResultsView(getTrunkedSystemView());
            getTrunkedSystemView().setSystem(system);
        }
    }

    private void setAgency(final Agency agency)
    {
        if(agency != null)
        {
            mUserPreferences.getRadioReferencePreference().setPreferredSystemId(RadioReferencePreference.INVALID_ID);
            mUserPreferences.getRadioReferencePreference().setPreferredAgencyId(agency.getAgencyId());

            ThreadPool.SCHEDULED.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        final AgencyInfo agencyInfo = mRadioReference.getService().getAgencyInfo(agency);
                        setFrequencyViewCategories(AGENCY_LABEL, agencyInfo.getName(), agencyInfo.getCategories());
                    }
                    catch(RadioReferenceException rre)
                    {
                        mLog.error("Error", rre);
                    }
                }
            });
        }
    }

    private HBox getTopBox()
    {
        if(mTopBox == null)
        {
            mTopBox = new HBox();
            HBox.setHgrow(getCredentialsBox(), Priority.ALWAYS);
            mTopBox.getChildren().addAll(getCountryBox(), getCredentialsBox());
        }

        return mTopBox;
    }

    private HBox getCredentialsBox()
    {
        if(mCredentialsBox == null)
        {
            mCredentialsBox = new HBox();
            mCredentialsBox.setAlignment(Pos.CENTER_RIGHT);
            mCredentialsBox.setPadding(new Insets(5, 5, 5, 5));
            mCredentialsBox.setAlignment(Pos.CENTER_RIGHT);
            mCredentialsBox.setSpacing(5.0);
            mCredentialsBox.getChildren().add(new Label("User Name:"));
            mCredentialsBox.getChildren().add(getUserNameText());
            mCredentialsBox.getChildren().add(new Label("Expires:"));
            mCredentialsBox.getChildren().add(getAccountExpiresText());
            mCredentialsBox.getChildren().add(getTestFailIcon());
            mCredentialsBox.getChildren().add(getTestPassIcon());
            mCredentialsBox.getChildren().add(getLoginButton());
        }

        return mCredentialsBox;
    }

    private TextField getUserNameText()
    {
        if(mUserNameText == null)
        {
            mUserNameText = new TextField(mRadioReference.getUserName());
            mUserNameText.setDisable(true);
        }

        return mUserNameText;
    }

    private TextField getAccountExpiresText()
    {
        if(mAccountExpiresText == null)
        {
            mAccountExpiresText = new TextField();
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
            mTestPassIcon.visibleProperty().bind(mRadioReference.loggedOnProperty());
        }

        return mTestPassIcon;
    }

    private IconNode getTestFailIcon()
    {
        if(mTestFailIcon == null)
        {
            mTestFailIcon = new IconNode(FontAwesome.TIMES);
            mTestFailIcon.setFill(Color.RED);
            mTestFailIcon.visibleProperty().bind(mRadioReference.loggedOnProperty().not());
        }

        return mTestFailIcon;
    }

    private Button getLoginButton()
    {
        if(mLoginButton == null)
        {
            mLoginButton = new Button("Login");
            IconNode configureIcon = new IconNode(FontAwesome.COG);
            configureIcon.setFill(Color.GRAY);
            mLoginButton.setGraphic(configureIcon);
            mLoginButton.setOnAction(event -> mJavaFxWindowManager.process(new LoginDialogViewRequest(RadioReferenceEditor.this::receive)));
        }

        return mLoginButton;
    }

    private SplitPane getSplitPane()
    {
        if(mSplitPane == null)
        {
            VBox vbox = new VBox();
            vbox.setPadding(new Insets(5,5,10,5));
            vbox.setSpacing(5.0);
            VBox.setVgrow(getCountryEntityBox(), Priority.SOMETIMES);
            vbox.getChildren().add(getCountryEntityBox());
            VBox.setVgrow(getStateEntityBox(), Priority.SOMETIMES);
            vbox.getChildren().add(getStateEntityBox());
            VBox.setVgrow(getCountyEntityBox(), Priority.SOMETIMES);
            vbox.getChildren().add(getCountyEntityBox());

            mSplitPane = new SplitPane();
            mSplitPane.setOrientation(Orientation.VERTICAL);
            mSplitPane.getItems().addAll(vbox, getFrequencyTableView());
        }

        return mSplitPane;
    }

    /**
     * Frequency table view for displaying frequency search results
     * @return node
     */
    private FrequencyTableView getFrequencyTableView()
    {
        if(mFrequencyTableView == null)
        {
            mFrequencyTableView = new FrequencyTableView(mRadioReference);
        }

        return mFrequencyTableView;
    }

    /**
     * Trunked radio system view for displaying a trunked system
     * @return node
     */
    private TrunkedSystemView getTrunkedSystemView()
    {
        if(mTrunkedSystemView == null)
        {
            mTrunkedSystemView = new TrunkedSystemView(mRadioReference);
        }

        return mTrunkedSystemView;
    }

    private HBox getCountryBox()
    {
        if(mCountryBox == null)
        {
            mCountryBox = new HBox();
            mCountryBox.setAlignment(Pos.CENTER_LEFT);
            mCountryBox.setSpacing(5.0);
            Label label = new Label("Country:");
            mCountryBox.getChildren().add(label);
            mCountryBox.getChildren().add(getCountryComboBox());
        }

        return mCountryBox;
    }

    private ComboBox<Country> getCountryComboBox()
    {
        if(mCountryComboBox == null)
        {
            mCountryComboBox = new ComboBox<>();
            mCountryComboBox.setPrefWidth(200);
            mCountryComboBox.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    Country selected = mCountryComboBox.getValue();
                    setCountry(selected);

                    if(selected != null)
                    {
                        mUserPreferences.getRadioReferencePreference().setPreferredCountryId(selected.getCountryId());
                    }
                }
            });
            mCountryComboBox.setConverter(new StringConverter<Country>()
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
            });
        }

        return mCountryComboBox;
    }

    public HBox getCountryEntityBox()
    {
        if(mCountryEntityBox == null)
        {
            VBox stateBox = new VBox();
            stateBox.getChildren().addAll(new Label("States"), getStateListView());
            VBox agencyBox = new VBox();
            agencyBox.getChildren().addAll(new Label("Country-wide Agencies"), getCountryAgencyListView());
            mCountryEntityBox = new HBox();
            mCountryEntityBox.setSpacing(5.0);
            HBox.setHgrow(stateBox, Priority.ALWAYS);
            HBox.setHgrow(agencyBox, Priority.ALWAYS);
            mCountryEntityBox.getChildren().addAll(stateBox, agencyBox);
        }

        return mCountryEntityBox;
    }

    public ListView<State> getStateListView()
    {
        if(mStateListView == null)
        {
            mStateListView = new ListView<>();
            mStateListView.setCellFactory(new Callback<ListView<State>,ListCell<State>>()
            {
                @Override
                public ListCell<State> call(ListView<State> param)
                {
                    return new StateListCell();
                }
            });
            mStateListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<State>()
            {
                @Override
                public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue)
                {
                    setState(newValue);

                    if(newValue != null)
                    {
                        mUserPreferences.getRadioReferencePreference().setPreferredStateId(newValue.getStateId());
                    }
                }
            });
        }

        return mStateListView;
    }

    public ListView<Agency> getCountryAgencyListView()
    {
        if(mCountryAgencyListView == null)
        {
            mCountryAgencyListView = new ListView<>();
            mCountryAgencyListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Agency>()
            {
                @Override
                public void changed(ObservableValue<? extends Agency> observable, Agency oldValue, Agency newValue)
                {
                    setAgency(newValue);
                }
            });
            mCountryAgencyListView.setCellFactory(new Callback<ListView<Agency>,ListCell<Agency>>()
            {
                @Override
                public ListCell<Agency> call(ListView<Agency> param)
                {
                    return new AgencyListCell();
                }
            });
        }

        return mCountryAgencyListView;
    }

    public HBox getStateEntityBox()
    {
        if(mStateEntityBox == null)
        {
            VBox stateBox = new VBox();
            stateBox.getChildren().addAll(new Label("Counties"), getCountyListView());
            VBox agencyBox = new VBox();
            agencyBox.getChildren().addAll(new Label("State Agencies"), getStateAgencyListView());
            mStateEntityBox = new HBox();
            mStateEntityBox.setSpacing(5.0);
            HBox.setHgrow(stateBox, Priority.ALWAYS);
            HBox.setHgrow(agencyBox, Priority.ALWAYS);
            mStateEntityBox.getChildren().addAll(stateBox, agencyBox);
        }

        return mStateEntityBox;
    }

    public ListView<County> getCountyListView()
    {
        if(mCountyListView == null)
        {
            mCountyListView = new ListView<>();
            mCountyListView.setCellFactory(new Callback<ListView<County>,ListCell<County>>()
            {
                @Override
                public ListCell<County> call(ListView<County> param)
                {
                    return new CountyListCell();
                }
            });
            mCountyListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<County>()
            {
                @Override
                public void changed(ObservableValue<? extends County> observable, County oldValue, County newValue)
                {
                    setCounty(newValue);

                    if(newValue != null)
                    {
                        mUserPreferences.getRadioReferencePreference().setPreferredCountyId(newValue.getCountyId());
                    }
                }
            });
        }

        return mCountyListView;
    }

    public ListView<Agency> getStateAgencyListView()
    {
        if(mStateAgencyListView == null)
        {
            mStateAgencyListView = new ListView<>();
            mStateAgencyListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Agency>()
            {
                @Override
                public void changed(ObservableValue<? extends Agency> observable, Agency oldValue, Agency newValue)
                {
                    setAgency(newValue);
                }
            });
            mStateAgencyListView.setCellFactory(new Callback<ListView<Agency>,ListCell<Agency>>()
            {
                @Override
                public ListCell<Agency> call(ListView<Agency> param)
                {
                    return new AgencyListCell();
                }
            });
        }

        return mStateAgencyListView;
    }

    public HBox getCountyEntityBox()
    {
        if(mCountyEntityBox == null)
        {
            VBox stateBox = new VBox();
            stateBox.getChildren().addAll(new Label("Trunked Radio Systems"), getSystemListView());
            VBox agencyBox = new VBox();
            agencyBox.getChildren().addAll(new Label("County Agencies"), getCountyAgencyListView());
            mCountyEntityBox = new HBox();
            mCountyEntityBox.setSpacing(5.0);
            HBox.setHgrow(stateBox, Priority.ALWAYS);
            HBox.setHgrow(agencyBox, Priority.ALWAYS);
            mCountyEntityBox.getChildren().addAll(stateBox, agencyBox);
        }

        return mCountyEntityBox;
    }

    public ListView<System> getSystemListView()
    {
        if(mSystemListView == null)
        {
            mSystemListView = new ListView<>();
            mSystemListView.setCellFactory(new Callback<ListView<System>,ListCell<System>>()
            {
                @Override
                public ListCell<System> call(ListView<System> param)
                {
                    return new SystemListCell();
                }
            });
            mSystemListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<System>()
            {
                @Override
                public void changed(ObservableValue<? extends System> observable, System oldValue, System newValue)
                {
                    setSystem(newValue);

                    if(newValue != null)
                    {
                        mUserPreferences.getRadioReferencePreference().setPreferredSystemId(newValue.getSystemId());
                    }
                }
            });
        }

        return mSystemListView;
    }

    public ListView<Agency> getCountyAgencyListView()
    {
        if(mCountyAgencyListView == null)
        {
            mCountyAgencyListView = new ListView<>();
            mCountyAgencyListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Agency>()
            {
                @Override
                public void changed(ObservableValue<? extends Agency> observable, Agency oldValue, Agency newValue)
                {
                    setAgency(newValue);
                }
            });
            mCountyAgencyListView.setCellFactory(new Callback<ListView<Agency>,ListCell<Agency>>()
            {
                @Override
                public ListCell<Agency> call(ListView<Agency> param)
                {
                    return new AgencyListCell();
                }
            });
        }

        return mCountyAgencyListView;
    }

    /**
     * Callback method for the LoginEditor to receive updated user credentials
     * @param authorizationInformation with user credentials
     */
    @Override
    public void receive(AuthorizationInformation authorizationInformation)
    {
        mRadioReference.setAuthorizationInformation(authorizationInformation);

        if(mRadioReference.hasCredentials())
        {
            checkAccount();
            refreshLookupTables();
            refreshCountries();
        }
    }

    public class StateListCell extends ListCell<State>
    {
        @Override
        protected void updateItem(State item, boolean empty)
        {
            super.updateItem(item, empty);
            setText((empty || item == null) ? null : item.getName());
        }
    }

    public class CountyListCell extends ListCell<County>
    {
        @Override
        protected void updateItem(County item, boolean empty)
        {
            super.updateItem(item, empty);
            setText((empty || item == null) ? null : item.getName());
        }
    }

    public class SystemListCell extends ListCell<System>
    {
        private HBox mHBox;
        private Label mName;
        private Label mProtocol;

        public SystemListCell()
        {
            mHBox = new HBox();
            mHBox.setMaxWidth(Double.MAX_VALUE);
            mName = new Label();
            mName.setMaxWidth(Double.MAX_VALUE);
            mName.setAlignment(Pos.CENTER_LEFT);
            mProtocol = new Label();
            mProtocol.setMaxWidth(Double.MAX_VALUE);
            mProtocol.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(mName, Priority.ALWAYS);
            HBox.setHgrow(mProtocol, Priority.ALWAYS);
            mHBox.getChildren().addAll(mName, mProtocol);
        }

        @Override
        protected void updateItem(System item, boolean empty)
        {
            super.updateItem(item, empty);

            setText(null);

            if(empty || item == null)
            {
                setGraphic(null);
            }
            else
            {
                mName.setText(item.getName());
                Type type = mTypesMap.get(item.getTypeId());

                if(type != null)
                {
                    mProtocol.setText(type.getName());
                }
                else
                {
                    mProtocol.setText("Unknown");
                }

                setGraphic(mHBox);
            }
        }
    }

    public class AgencyListCell extends ListCell<Agency>
    {
        @Override
        protected void updateItem(Agency item, boolean empty)
        {
            super.updateItem(item, empty);
            setText((empty || item == null) ? null : item.getName());
        }
    }


}
