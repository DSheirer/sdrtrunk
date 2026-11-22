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

package io.github.dsheirer.gui.playlist.radioreference;

import com.google.common.annotations.VisibleForTesting;
import io.github.dsheirer.preference.radioreference.RadioReferencePreference.Location;
import io.github.dsheirer.rrapi.RadioReferenceException;
import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.util.ThreadPool;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

/**
 * Dialog to set the user's location, used to rank radio reference results by distance.  The location can be looked
 * up from a United States zip code (premium radio reference account) or entered as decimal degrees.
 */
public class LocationDialog extends Dialog<LocationDialog.Result>
{
    /**
     * Button to clear the location
     */
    public static final ButtonType CLEAR = new ButtonType("Clear", ButtonBar.ButtonData.LEFT);

    private static final String COORDINATE = "([-+]?\\d+(?:[.,]\\d+)?)\\s*°?\\s*([NSEWnsew])?";

    /**
     * A latitude and longitude pair, e.g. "43.0481, -76.1474" as copied from Google Maps, or "43.0481° N, 76.1474° W".
     * A comma separator must be followed by whitespace, so that a decimal comma isn't taken for a separator.
     */
    private static final Pattern COORDINATE_PAIR = Pattern.compile("^\\s*" + COORDINATE + "\\s*(?:,\\s+|;\\s*|\\s+)" +
        COORDINATE + "\\s*$");
    private static final Pattern SINGLE_COORDINATE = Pattern.compile("^\\s*" + COORDINATE + "\\s*$");

    private final RadioReference mRadioReference;
    private final TextField mZipcodeText = new TextField();
    private final Button mLookupButton = new Button("Look Up");
    private final Label mLookupStatus = new Label();
    private final TextField mLatitudeText = new TextField();
    private final TextField mLongitudeText = new TextField();
    private final Label mValidationText = new Label();
    private String mLabel;
    private boolean mUpdatingFields;

    /**
     * Result of the dialog
     * @param location to store, or null to clear the location
     */
    public record Result(Location location)
    {
    }

    /**
     * Constructs an instance
     * @param radioReference service, for zip code lookups
     * @param current location, or null
     * @param owner window
     */
    public LocationDialog(RadioReference radioReference, Location current, Window owner)
    {
        mRadioReference = radioReference;

        setTitle("sdrtrunk - Location");
        setHeaderText("Your location is shown on the map and used to find nearby Radio Reference frequencies");

        if(owner != null)
        {
            initOwner(owner);
        }

        getDialogPane().setContent(createContent());
        getDialogPane().getButtonTypes().addAll(CLEAR, ButtonType.OK, ButtonType.CANCEL);

        if(current != null)
        {
            mUpdatingFields = true;
            mLatitudeText.setText(format(current.latitude()));
            mLongitudeText.setText(format(current.longitude()));
            mUpdatingFields = false;
            mLabel = current.label();
        }
        else
        {
            getDialogPane().lookupButton(CLEAR).setDisable(true);
        }

        BooleanBinding valid = Bindings.createBooleanBinding(() -> getLocation() != null,
            mLatitudeText.textProperty(), mLongitudeText.textProperty());
        getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(valid.not());
        updateValidationText();

        setResultConverter(buttonType -> {
            if(buttonType == ButtonType.OK)
            {
                return new Result(getLocation());
            }
            else if(buttonType == CLEAR)
            {
                return new Result(null);
            }

            return null;
        });
    }

    private Node createContent()
    {
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10, 10, 5, 10));
        gridPane.setHgap(5.0);
        gridPane.setVgap(5.0);

        Label zipcodeLabel = new Label("Zip Code:");
        zipcodeLabel.setLabelFor(mZipcodeText);
        GridPane.setHalignment(zipcodeLabel, HPos.RIGHT);
        gridPane.add(zipcodeLabel, 0, 0);

        mZipcodeText.setPrefColumnCount(6);
        mZipcodeText.setOnAction(event -> lookup());
        gridPane.add(mZipcodeText, 1, 0);

        mLookupButton.setOnAction(event -> lookup());
        gridPane.add(mLookupButton, 2, 0);

        //Zip code lookups need a premium account
        boolean premium = mRadioReference.premiumAccountProperty().get();
        mZipcodeText.setDisable(!premium);
        mLookupButton.setDisable(!premium);
        mLookupStatus.setText(premium ? "United States zip codes only" :
            "Zip code lookup requires a premium Radio Reference login");
        mLookupStatus.setWrapText(true);
        gridPane.add(mLookupStatus, 1, 1, 4, 1);

        Label latitudeLabel = new Label("Latitude:");
        latitudeLabel.setLabelFor(mLatitudeText);
        GridPane.setHalignment(latitudeLabel, HPos.RIGHT);
        gridPane.add(latitudeLabel, 0, 2);

        mLatitudeText.setPrefColumnCount(10);
        mLatitudeText.setPromptText("43.0481");
        GridPane.setHgrow(mLatitudeText, Priority.ALWAYS);
        gridPane.add(mLatitudeText, 1, 2, 2, 1);

        Label longitudeLabel = new Label("Longitude:");
        longitudeLabel.setLabelFor(mLongitudeText);
        GridPane.setHalignment(longitudeLabel, HPos.RIGHT);
        gridPane.add(longitudeLabel, 3, 2);

        mLongitudeText.setPrefColumnCount(10);
        mLongitudeText.setPromptText("-76.1474");
        GridPane.setHgrow(mLongitudeText, Priority.ALWAYS);
        gridPane.add(mLongitudeText, 4, 2);

        Label hint = new Label("Decimal degrees.  You can paste \"latitude, longitude\" from a map into either field.");
        hint.setWrapText(true);
        gridPane.add(hint, 1, 3, 4, 1);

        mValidationText.setWrapText(true);
        //Inline style, because the theme stylesheets override a label's text fill
        mValidationText.setStyle("-fx-text-fill: orangered;");
        gridPane.add(mValidationText, 1, 4, 4, 1);

        mLatitudeText.textProperty().addListener((observable, oldValue, newValue) -> coordinatesEdited(newValue));
        mLongitudeText.textProperty().addListener((observable, oldValue, newValue) -> coordinatesEdited(newValue));

        return gridPane;
    }

    /**
     * Handles an edit of either coordinate field: splits a pasted pair into both fields and forgets the zip code label
     * because the location no longer comes from the zip code.
     */
    private void coordinatesEdited(String text)
    {
        if(mUpdatingFields)
        {
            return;
        }

        mLabel = null;

        double[] pair = parsePair(text);

        if(pair != null)
        {
            //Changing a text field from its own change listener isn't allowed, so split the pair afterwards
            Platform.runLater(() -> setCoordinates(pair[0], pair[1]));
        }

        updateValidationText();
    }

    private void setCoordinates(double latitude, double longitude)
    {
        mUpdatingFields = true;
        mLatitudeText.setText(format(latitude));
        mLongitudeText.setText(format(longitude));
        mUpdatingFields = false;
        updateValidationText();
    }

    private void updateValidationText()
    {
        String latitudeText = mLatitudeText.getText();
        String longitudeText = mLongitudeText.getText();

        if(isBlank(latitudeText) && isBlank(longitudeText))
        {
            mValidationText.setText(null);
        }
        else if(isBlank(latitudeText) || isBlank(longitudeText))
        {
            mValidationText.setText("Enter both a latitude and a longitude");
        }
        else if(Double.isNaN(parseCoordinate(latitudeText, true)))
        {
            mValidationText.setText("Latitude must be a number from -90 to 90");
        }
        else if(Double.isNaN(parseCoordinate(longitudeText, false)))
        {
            mValidationText.setText("Longitude must be a number from -180 to 180");
        }
        else
        {
            mValidationText.setText(null);
        }
    }

    /**
     * Location from the fields
     * @return location, or null if the fields aren't valid
     */
    private Location getLocation()
    {
        double latitude = parseCoordinate(mLatitudeText.getText(), true);
        double longitude = parseCoordinate(mLongitudeText.getText(), false);

        if(Location.isValid(latitude, longitude))
        {
            return new Location(latitude, longitude, mLabel);
        }

        return null;
    }

    /**
     * Looks up the zip code in the background and fills the coordinates
     */
    private void lookup()
    {
        String zipcode = mZipcodeText.getText() != null ? mZipcodeText.getText().trim() : "";

        if(!zipcode.matches("\\d{5}"))
        {
            mLookupStatus.setText("Enter a five digit zip code");
            return;
        }

        mLookupButton.setDisable(true);
        mLookupStatus.setText("Looking up " + zipcode + "...");

        ThreadPool.CACHED.execute(() -> {
            Location location = null;
            String error = null;

            try
            {
                location = mRadioReference.lookupZipcode(Integer.parseInt(zipcode));
            }
            catch(RadioReferenceException rre)
            {
                error = switch(rre.getErrorType())
                {
                    case NETWORK, TIMEOUT -> "Unable to reach Radio Reference";
                    case INVALID_REQUEST -> "Radio Reference has no location for zip code " + zipcode;
                    default -> "Lookup failed: " + rre.getMessage();
                };
            }
            catch(RuntimeException re)
            {
                error = "Lookup failed: " + re.getMessage();
            }

            final Location result = location;
            final String message = error;

            Platform.runLater(() -> {
                mLookupButton.setDisable(false);

                if(result != null)
                {
                    setCoordinates(result.latitude(), result.longitude());
                    mLabel = result.label();
                    mLookupStatus.setText(result.label());
                }
                else
                {
                    mLookupStatus.setText(message);
                }
            });
        });
    }

    /**
     * Parses a latitude and longitude pair
     * @param text to parse
     * @return latitude and longitude, or null if the text isn't a valid pair
     */
    @VisibleForTesting
    static double[] parsePair(String text)
    {
        if(text == null)
        {
            return null;
        }

        Matcher matcher = COORDINATE_PAIR.matcher(text);

        if(matcher.matches())
        {
            double latitude = toDegrees(matcher.group(1), matcher.group(2), true);
            double longitude = toDegrees(matcher.group(3), matcher.group(4), false);

            if(Location.isValid(latitude, longitude))
            {
                return new double[]{latitude, longitude};
            }
        }

        return null;
    }

    /**
     * Parses one coordinate in decimal degrees, with an optional degree sign and hemisphere letter
     * @param text to parse
     * @param latitude true for a latitude, false for a longitude
     * @return degrees, or NaN if the text isn't a valid coordinate
     */
    @VisibleForTesting
    static double parseCoordinate(String text, boolean latitude)
    {
        if(text == null)
        {
            return Double.NaN;
        }

        Matcher matcher = SINGLE_COORDINATE.matcher(text);

        if(matcher.matches())
        {
            double degrees = toDegrees(matcher.group(1), matcher.group(2), latitude);

            if(Math.abs(degrees) <= (latitude ? 90.0 : 180.0))
            {
                return degrees;
            }
        }

        return Double.NaN;
    }

    /**
     * Converts a number and an optional hemisphere letter to signed degrees
     * @return degrees, or NaN if the hemisphere letter doesn't match the coordinate type
     */
    private static double toDegrees(String number, String hemisphere, boolean latitude)
    {
        //Accept a decimal comma
        double degrees = Double.parseDouble(number.replace(',', '.'));

        if(hemisphere != null)
        {
            char letter = Character.toUpperCase(hemisphere.charAt(0));
            boolean latitudeLetter = letter == 'N' || letter == 'S';

            if(latitudeLetter != latitude || degrees < 0)
            {
                return Double.NaN;
            }

            if(letter == 'S' || letter == 'W')
            {
                degrees = -degrees;
            }
        }

        return degrees;
    }

    /**
     * Formats degrees with up to six decimal places (about 0.1 meter)
     */
    @VisibleForTesting
    static String format(double degrees)
    {
        return BigDecimal.valueOf(degrees).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static boolean isBlank(String text)
    {
        return text == null || text.isBlank();
    }
}
