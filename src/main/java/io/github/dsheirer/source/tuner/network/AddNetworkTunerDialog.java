/*
 * *****************************************************************************
 * Copyright (C) 2026 Jared Szechy
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
package io.github.dsheirer.source.tuner.network;

import io.github.dsheirer.gui.control.FrequencyEntry;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationManager;
import io.github.dsheirer.source.tuner.manager.DiscoveredNetworkTuner;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog for adding an rtl_tcp network tuner.
 */
public class AddNetworkTunerDialog extends Dialog<ButtonType> {
  private static final Logger mLog = LoggerFactory.getLogger(AddNetworkTunerDialog.class);
  private static final ButtonType ADD = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
  private final UserPreferences mUserPreferences;
  private final DiscoveredTunerModel mDiscoveredTunerModel;
  private final TunerConfigurationManager mTunerConfigurationManager;
  private final ComboBox<String> mTypeComboBox = new ComboBox<>();
  private final TextField mHostField = new TextField();
  private final TextField mPortField = new TextField("1234");
  private final TextField mFrequencyField = new TextField();
  private final Label mValidationLabel = new Label();

  public AddNetworkTunerDialog(Window owner, UserPreferences userPreferences, DiscoveredTunerModel discoveredTunerModel,
      TunerConfigurationManager tunerConfigurationManager) {
    Validate.notNull(userPreferences, "UserPreferences cannot be null");
    Validate.notNull(discoveredTunerModel, "TunerModel cannot be null");
    Validate.notNull(tunerConfigurationManager, "TunerConfigurationManager cannot be null");
    mDiscoveredTunerModel = discoveredTunerModel;
    mUserPreferences = userPreferences;
    mTunerConfigurationManager = tunerConfigurationManager;

    initOwner(owner);
    setTitle("Add Network Tuner");
    setHeaderText("Add an rtl_tcp network tuner");

    mTypeComboBox.getItems().add("rtl-tcp");
    mTypeComboBox.getSelectionModel().selectFirst();
    mTypeComboBox.setMaxWidth(Double.MAX_VALUE);
    mHostField.setPromptText("hostname or IP address");
    mPortField.setPromptText("e.g. 1234");
    mFrequencyField.setPromptText("e.g. 851.0125");
    mValidationLabel.getStyleClass().add("tuner-validation");
    mValidationLabel.setWrapText(true);
    mValidationLabel.setStyle("-fx-text-fill: #d9443a;");

    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(8);
    grid.setPadding(new Insets(10));
    grid.addRow(0, new Label("Type"), mTypeComboBox);
    grid.addRow(1, new Label("Host"), mHostField);
    grid.addRow(2, new Label("Port"), mPortField);
    grid.addRow(3, new Label("Frequency (MHz)"), mFrequencyField);
    grid.add(mValidationLabel, 0, 4, 2, 1);
    GridPane.setHgrow(mHostField, Priority.ALWAYS);
    grid.setPrefWidth(420);

    getDialogPane().setContent(grid);
    getDialogPane().getButtonTypes().addAll(ADD, ButtonType.CANCEL);

    Node addButton = getDialogPane().lookupButton(ADD);
    addButton.setDisable(true);
    Runnable validate = () -> {
      String error = getValidationError();
      mValidationLabel.setText(error == null ? "" : error);
      addButton.setDisable(error != null);
    };
    mHostField.textProperty().addListener((observable, oldValue, newValue) -> validate.run());
    mPortField.textProperty().addListener((observable, oldValue, newValue) -> validate.run());
    mFrequencyField.textProperty().addListener((observable, oldValue, newValue) -> validate.run());
    validate.run();
    mValidationLabel.setText("");

    setResultConverter(buttonType -> {
      if (buttonType == ADD) {
        addTuner();
      }
      return buttonType;
    });

    Platform.runLater(mHostField::requestFocus);
  }

  /**
   * Validation error for the current entries, or null when they are valid.
   */
  private String getValidationError() {
    if (getHost().isEmpty()) {
      return "Enter the host name or IP address of the rtl_tcp server";
    }

    if (getPort() <= 0) {
      return "Enter a port number from 1 to " + Short.MAX_VALUE;
    }

    Long frequency = FrequencyEntry.parse(mFrequencyField.getText());

    if (frequency == null || frequency <= 0 || frequency > Integer.MAX_VALUE) {
      return "Enter a center frequency up to 2147.483647 MHz";
    }

    return null;
  }

  private void addTuner() {
    long frequency = FrequencyEntry.parse(mFrequencyField.getText());
    mLog.info("Adding network tuner = frequency [" + frequency + "] host [" + getHost() + ":" + getPort() + "]");

    try {
      NetworkTunerConfiguration config = NetworkTunerConfiguration.create();
      config.setFrequency(frequency);
      config.setHost(getHost());
      config.setPort(getPort());
      mTunerConfigurationManager.addTunerConfiguration(config);
      DiscoveredNetworkTuner discoveredNetworkTuner = new DiscoveredNetworkTuner(mUserPreferences, config);
      mDiscoveredTunerModel.addDiscoveredTuner(discoveredNetworkTuner);
    } catch (Exception ex) {
      mLog.error("Error adding network tuner", ex);
    }
  }

  private String getHost() {
    return mHostField.getText() == null ? "" : mHostField.getText().trim();
  }

  private short getPort() {
    try {
      return Short.parseShort(mPortField.getText().trim());
    } catch (Exception e) {
      return 0;
    }
  }
}
