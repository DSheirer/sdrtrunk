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
import io.github.dsheirer.rrapi.type.Category;
import io.github.dsheirer.rrapi.type.Frequency;
import io.github.dsheirer.rrapi.type.Mode;
import io.github.dsheirer.rrapi.type.SubCategory;
import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.util.ThreadPool;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Grid pane with components for visualizing and selecting categories of frequencies for an agency
 */
public class AgencyFrequencyEditor extends GridPane
{
    private static final Logger mLog = LoggerFactory.getLogger(AgencyFrequencyEditor.class);
    private static final Category ALL_CATEGORIES = new AllCategories();
    private static final SubCategory ALL_SUB_CATEGORIES = new AllSubCategories();

    private UserPreferences mUserPreferences;
    private RadioReference mRadioReference;
    private PlaylistManager mPlaylistManager;
    private Level mLevel;
    private ComboBox<Category> mCategoryComboBox;
    private ComboBox<SubCategory> mSubCategoryComboBox;
    private TableView<Frequency> mFrequencyTableView;
    private Label mPlaceholderLabel;
    private ProgressIndicator mProgressIndicator;
    private FrequencyEditor mFrequencyEditor;

    public AgencyFrequencyEditor(UserPreferences userPreferences, RadioReference radioReference,
                                 PlaylistManager playlistManager, Level level)
    {
        mUserPreferences = userPreferences;
        mRadioReference = radioReference;
        mPlaylistManager = playlistManager;
        mLevel = level;

        setPadding(new Insets(5, 5, 5,5));
        setVgap(10);
        setHgap(10);
        setMaxHeight(Double.MAX_VALUE);

        int row = 0;

        ColumnConstraints column1 = new ColumnConstraints();
        ColumnConstraints column2 = new ColumnConstraints();
        ColumnConstraints column3 = new ColumnConstraints();
        column3.setPercentWidth(40);
        getColumnConstraints().addAll(column1, column2, column3);

        Label categoryLabel = new Label("Category");
        GridPane.setConstraints(categoryLabel, 0, row);
        GridPane.setHalignment(categoryLabel, HPos.RIGHT);
        getChildren().add(categoryLabel);

        GridPane.setConstraints(getCategoryComboBox(), 1, row);
        GridPane.setHgrow(getCategoryComboBox(), Priority.ALWAYS);
        getChildren().add(getCategoryComboBox());

        Label subCategoryLabel = new Label("Sub-Category");
        GridPane.setConstraints(subCategoryLabel, 0, ++row);
        GridPane.setHalignment(subCategoryLabel, HPos.RIGHT);
        getChildren().add(subCategoryLabel);

        GridPane.setConstraints(getSubCategoryComboBox(), 1, row);
        GridPane.setHgrow(getSubCategoryComboBox(), Priority.ALWAYS);
        getChildren().add(getSubCategoryComboBox());

        GridPane.setConstraints(getFrequencyTableView(), 0, ++row, 2, 1);
        GridPane.setHgrow(getFrequencyTableView(), Priority.ALWAYS);
        GridPane.setVgrow(getFrequencyTableView(), Priority.ALWAYS);
        getChildren().add(getFrequencyTableView());

        GridPane.setConstraints(getFrequencyEditor(), 2, 0, 1, 3);
        GridPane.setHgrow(getFrequencyEditor(), Priority.ALWAYS);
        getChildren().add(getFrequencyEditor());
    }

    public void setLoading(boolean loading)
    {
        if(Platform.isFxApplicationThread())
        {
            getFrequencyTableView().setPlaceholder(loading ? getProgressIndicator() : getPlaceholderLabel());
        }
        else
        {
            Platform.runLater(() -> getFrequencyTableView()
                .setPlaceholder(loading ? getProgressIndicator() : getPlaceholderLabel()));
        }
    }

    public void clear()
    {
        getCategoryComboBox().getItems().clear();
        getSubCategoryComboBox().getItems().clear();
        getFrequencyTableView().getItems().clear();
    }

    public void clearAndSetLoading()
    {
        clear();
        setLoading(true);
    }

    public void setCategories(List<Category> categories)
    {
        clear();

        if(categories != null)
        {
            Collections.sort(categories, new CategoryComparator());
        }

        if(categories != null && !categories.isEmpty())
        {
            // Only allow the combined list of all categories if there aren't that many
            if (categories.size() < 10) {
                categories.add(0, ALL_CATEGORIES);
            }
            getCategoryComboBox().getItems().addAll(categories);
            getCategoryComboBox().getSelectionModel().select(categories.get(0));
        }
        else
        {
            setLoading(false);
        }
    }

    private FrequencyEditor getFrequencyEditor()
    {
        if(mFrequencyEditor == null)
        {
            mFrequencyEditor = new FrequencyEditor(mUserPreferences, mRadioReference, mPlaylistManager, mLevel);
        }

        return mFrequencyEditor;
    }

    private ProgressIndicator getProgressIndicator()
    {
        if(mProgressIndicator == null)
        {
            mProgressIndicator = new ProgressIndicator();
            mProgressIndicator.setProgress(-1);
        }

        return mProgressIndicator;
    }

    private Label getPlaceholderLabel()
    {
        if(mPlaceholderLabel == null)
        {
            mPlaceholderLabel = new Label("Select an Agency to View Frequency Records");
        }

        return mPlaceholderLabel;
    }

    private TableView<Frequency> getFrequencyTableView()
    {
        if(mFrequencyTableView == null)
        {
            mFrequencyTableView = new TableView<>();
            mFrequencyTableView.setMaxHeight(Double.MAX_VALUE);
            mFrequencyTableView.setPlaceholder(getPlaceholderLabel());

            TableColumn descriptionColumn = new TableColumn();
            descriptionColumn.setText("Description");
            descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
            descriptionColumn.setPrefWidth(300);

            TableColumn frequencyColumn = new TableColumn();
            frequencyColumn.setText("Frequency");
            frequencyColumn.setCellValueFactory(new FrequencyCellValueFactory());
            frequencyColumn.setPrefWidth(100);

            TableColumn modeColumn = new TableColumn();
            modeColumn.setText("Mode");
            modeColumn.setCellValueFactory(new ModeCellValueFactory());
            modeColumn.setPrefWidth(100);

            mFrequencyTableView.getColumns().addAll(descriptionColumn, frequencyColumn, modeColumn);
            mFrequencyTableView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, selected) -> {
                    if(selected != null)
                    {
                        SubCategory subCategory = getSubCategory(selected.getSubCategoryId());
                        Category category = getCategory(subCategory);
                        getFrequencyEditor().setItem(selected, category, subCategory);
                    }
                    else
                    {
                        getFrequencyEditor().setItem(null, null, null);
                    }
                });
        }

        return mFrequencyTableView;
    }

    /**
     * Returns the category parent for the specified sub-category from the current list of categories.
     * @return category or null
     */
    private Category getCategory(SubCategory subCategory)
    {
        if(subCategory != null)
        {
            for(Category category: getCategoryComboBox().getItems())
            {
                for(SubCategory sub: category.getSubCategories())
                {
                    if(sub == subCategory)
                    {
                        return category;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Returns the sub-category that matches the id from the current list of sub-categories
     * @return sub-category or null
     */
    private SubCategory getSubCategory(int id)
    {
        for(SubCategory subCategory: getSubCategoryComboBox().getItems())
        {
            if(subCategory.getSubCategoryId() == id)
            {
                return subCategory;
            }
        }

        return null;
    }

    private ComboBox<Category> getCategoryComboBox()
    {
        if(mCategoryComboBox == null)
        {
            mCategoryComboBox = new ComboBox<>();
            mCategoryComboBox.setMaxWidth(Double.MAX_VALUE);
            mCategoryComboBox.setOnAction(event -> {
                getFrequencyTableView().getItems().clear();
                getSubCategoryComboBox().getItems().clear();
                Category selected = mCategoryComboBox.getSelectionModel().getSelectedItem();

                if(selected != null)
                {
                    List<SubCategory> subCategories = new ArrayList<>();

                    if(selected.equals(ALL_CATEGORIES))
                    {
                        for(Category category: getCategoryComboBox().getItems())
                        {
                            if(category.getSubCategories() != null)
                            {
                                subCategories.addAll(category.getSubCategories());
                            }
                        }
                    }
                    else if(selected.getSubCategories() != null)
                    {
                        subCategories.addAll(selected.getSubCategories());
                    }

                    Collections.sort(subCategories, new SubCategoryComparator());
                    getSubCategoryComboBox().getItems().add(ALL_SUB_CATEGORIES);
                    getSubCategoryComboBox().getItems().addAll(subCategories);
                    getSubCategoryComboBox().getSelectionModel().select(ALL_SUB_CATEGORIES);
                }
            });
            mCategoryComboBox.setConverter(new StringConverter<Category>()
            {
                @Override
                public String toString(Category category)
                {
                    if(category != null)
                    {
                        return category.getName();
                    }

                    return null;
                }

                @Override
                public Category fromString(String string)
                {
                    if(string != null)
                    {
                        for(Category category: mCategoryComboBox.getItems())
                        {
                            if(category.getName().contentEquals(string))
                            {
                                return category;
                            }
                        }
                    }

                    return null;
                }
            });
        }

        return mCategoryComboBox;
    }

    private ComboBox<SubCategory> getSubCategoryComboBox()
    {
        if(mSubCategoryComboBox == null)
        {
            mSubCategoryComboBox = new ComboBox<>();
            mSubCategoryComboBox.setMaxWidth(Double.MAX_VALUE);
            mSubCategoryComboBox.setConverter(new StringConverter<SubCategory>()
            {
                @Override
                public String toString(SubCategory subCategory)
                {
                    if(subCategory != null)
                    {
                        return subCategory.getName();
                    }

                    return null;
                }

                @Override
                public SubCategory fromString(String string)
                {
                    if(string != null)
                    {
                        for(SubCategory subCategory: mSubCategoryComboBox.getItems())
                        {
                            if(subCategory.getName().contentEquals(string))
                            {
                                return subCategory;
                            }
                        }
                    }

                    return null;
                }
            });

            mSubCategoryComboBox.setOnAction(event -> {
                setLoading(true);
                getFrequencyTableView().getItems().clear();

                SubCategory subCategory = mSubCategoryComboBox.getValue();

                if(subCategory != null)
                {
                    ThreadPool.CACHED.submit(() -> {
                        try
                        {
                            List<Frequency> frequencies = new ArrayList<>();

                            if(subCategory.equals(ALL_SUB_CATEGORIES))
                            {
                                for(SubCategory subCategory1: getSubCategoryComboBox().getItems())
                                {
                                    if(subCategory1.equals(ALL_SUB_CATEGORIES))
                                    {
                                        //skip
                                    }
                                    else
                                    {
                                        final List<Frequency> subCatFrequencies = mRadioReference.getService()
                                                .getSubCategoryFrequencies(subCategory1.getSubCategoryId());

                                        if(subCatFrequencies != null)
                                        {
                                            frequencies.addAll(subCatFrequencies);
                                        }
                                    }
                                }
                            }
                            else
                            {
                                final List<Frequency> subCatFrequencies = mRadioReference.getService()
                                        .getSubCategoryFrequencies(subCategory.getSubCategoryId());

                                if(subCatFrequencies != null)
                                {
                                    frequencies.addAll(subCatFrequencies);
                                }
                            }

                            Collections.sort(frequencies, new FrequencyComparator());

                            Platform.runLater(() -> {
                                getFrequencyTableView().getItems().addAll(frequencies);
                                setLoading(false);
                            });
                        }
                        catch(Throwable t)
                        {
                            mLog.error("Error retrieving frequencies for subcategory [" + subCategory.getName() + "]", t);
                            Platform.runLater(() -> new RadioReferenceUnavailableAlert(getSubCategoryComboBox()).showAndWait());
                        }
                    });
                }
            });
        }

        return mSubCategoryComboBox;
    }

    public static class AllCategories extends Category
    {
        public AllCategories()
        {
            this.setCategoryId(-1);
            this.setName("(All)");
        }
    }

    public static class AllSubCategories extends SubCategory
    {
        public AllSubCategories()
        {
            this.setSubCategoryId(-1);
            this.setName("(All)");
        }
    }

    public class FrequencyCellValueFactory implements Callback<TableColumn.CellDataFeatures<Frequency, String>,
            ObservableValue<String>>
    {
        private final DecimalFormat FREQUENCY_FORMATTER = new DecimalFormat("0.00000");
        private SimpleStringProperty mFrequencyFormatted = new SimpleStringProperty();

        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<Frequency, String> param)
        {
            mFrequencyFormatted.set(FREQUENCY_FORMATTER.format(param.getValue().getDownlink()));
            return mFrequencyFormatted;
        }
    }

    public class ModeCellValueFactory implements Callback<TableColumn.CellDataFeatures<Frequency, String>,
            ObservableValue<String>>
    {
        private SimpleStringProperty mMode = new SimpleStringProperty();

        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<Frequency, String> param)
        {
            String mode = param.getValue().getMode();

            try
            {
                Integer modeInt = Integer.valueOf(mode);
                Mode modeObj = mRadioReference.getService().getModesMap().get(modeInt);

                if(modeObj != null)
                {
                    mMode.set(modeObj != null ? modeObj.getName() : null);
                }
            }
            catch(Exception pe)
            {
                mMode.set("Error");
            }

            return mMode;
        }
    }

    public class CategoryComparator implements Comparator<Category>
    {
        @Override
        public int compare(Category o1, Category o2)
        {
            if(o1.getName() == null && o2.getName() == null)
            {
                return 0;
            }
            else if(o1.getName() == null)
            {
                return 1;
            }
            else if(o2.getName() == null)
            {
                return -1;
            }
            else
            {
                return o1.getName().compareTo(o2.getName());
            }
        }
    }

    public class FrequencyComparator implements Comparator<Frequency>
    {
        @Override
        public int compare(Frequency o1, Frequency o2)
        {
            if(o1.getAlphaTag() == null && o2.getAlphaTag() == null)
            {
                return 0;
            }
            else if(o1.getAlphaTag() == null)
            {
                return 1;
            }
            else if(o2.getAlphaTag() == null)
            {
                return -1;
            }
            else
            {
                return o1.getAlphaTag().compareTo(o2.getAlphaTag());
            }
        }
    }

    public class SubCategoryComparator implements Comparator<SubCategory>
    {
        @Override
        public int compare(SubCategory o1, SubCategory o2)
        {
            if(o1.getName() == null && o2.getName() == null)
            {
                return 0;
            }
            else if(o1.getName() == null)
            {
                return 1;
            }
            else if(o2.getName() == null)
            {
                return -1;
            }
            else
            {
                return o1.getName() .compareTo(o2.getName());
            }
        }
    }
}
