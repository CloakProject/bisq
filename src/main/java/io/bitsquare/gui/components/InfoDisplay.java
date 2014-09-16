/*
 * This file is part of Bitsquare.
 *
 * Bitsquare is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bitsquare is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bitsquare. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bitsquare.gui.components;

import io.bitsquare.locale.BSResources;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience Component for info icon, info text and link display in a GridPane.
 * Only the properties needed are supported.
 * We need to extend from Parent so we can use it in FXML, but the InfoDisplay is not used as node,
 * but add the children nodes to the gridPane.
 */
public class InfoDisplay extends Parent {
    private static final Logger log = LoggerFactory.getLogger(InfoDisplay.class);

    private final StringProperty text = new SimpleStringProperty();
    private final IntegerProperty rowIndex = new SimpleIntegerProperty(0);
    private final IntegerProperty columnIndex = new SimpleIntegerProperty(0);
    private final DoubleProperty prefWidth = new SimpleDoubleProperty(740);
    private final ObjectProperty<EventHandler<ActionEvent>> onAction = new SimpleObjectProperty<>();
    private final ObjectProperty<GridPane> gridPane = new SimpleObjectProperty<>();

    private boolean useReadMore;

    private final ImageView icon;
    private final TextFlow textFlow;
    private final Label label;
    private final Hyperlink link;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    public InfoDisplay() {

        icon = new ImageView();
        icon.setId("image-info");
        icon.setPickOnBounds(true);
        icon.setPreserveRatio(true);
        icon.visibleProperty().bind(visibleProperty());

        GridPane.setValignment(icon, VPos.TOP);
        GridPane.setMargin(icon, new Insets(4, 2, 0, 0));
        GridPane.setRowSpan(icon, 2);

        label = new Label();
        label.textProperty().bind(text);
        label.prefWidthProperty().bind(prefWidth);
        label.setTextOverrun(OverrunStyle.WORD_ELLIPSIS);

        link = new Hyperlink(BSResources.get("shared.readMore"));
        link.setPadding(new Insets(0, 0, 0, -2));

        // We need that to know if we have a wrapping or not. 
        // Did not find a way to get that from the API.
        Label testLabel = new Label();
        testLabel.textProperty().bind(text);

        textFlow = new TextFlow();
        textFlow.visibleProperty().bind(visibleProperty());
        textFlow.getChildren().addAll(testLabel);

        testLabel.widthProperty().addListener((ov, o, n) -> {
            if ((double) n > textFlow.getWidth()) {
                link.setText(BSResources.get("shared.readMore"));
                useReadMore = true;
            }
            else {
                link.setText(BSResources.get("shared.openHelp"));
            }
            Platform.runLater(() -> textFlow.getChildren().setAll(label, link));
        });

        // when clicking "Read more..." we expand and change the link to the Help 
        link.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (useReadMore) {

                    label.setWrapText(true);
                    link.setText(BSResources.get("shared.openHelp"));
                    label.prefWidthProperty().bind(textFlow.widthProperty());
                    link.setVisited(false);
                    // focus border is a bit confusing here so we remove it
                    link.setStyle("-fx-focus-color: transparent;");
                    link.setOnAction(onAction.get());
                }
                else {
                    onAction.get().handle(actionEvent);
                }
            }
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Setters
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void setText(String text) {
        this.text.set(text);
    }

    public void setPrefWidth(double prefWidth) {
        this.prefWidth.set(prefWidth);
        //  label.setPrefWidth(getPrefWidth());
    }

    public void setGridPane(GridPane gridPane) {
        this.gridPane.set(gridPane);

        gridPane.getChildren().addAll(icon, textFlow);

        GridPane.setColumnIndex(icon, columnIndex.get());
        GridPane.setColumnIndex(textFlow, columnIndex.get() + 1);

        GridPane.setRowIndex(icon, rowIndex.get());
        GridPane.setRowIndex(textFlow, rowIndex.get());
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex.set(rowIndex);

        GridPane.setRowIndex(icon, rowIndex);
        GridPane.setRowIndex(textFlow, rowIndex);
    }

    public void setColumnIndex(int columnIndex) {
        this.columnIndex.set(columnIndex);

        GridPane.setColumnIndex(icon, columnIndex);
        GridPane.setColumnIndex(textFlow, columnIndex + 1);

    }

    public final void setOnAction(EventHandler<ActionEvent> eventHandler) {
        onAction.set(eventHandler);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Getters
    ///////////////////////////////////////////////////////////////////////////////////////////

    public String getText() {
        return text.get();
    }

    public StringProperty textProperty() {
        return text;
    }

    public double getPrefWidth() {
        return prefWidth.get();
    }

    public DoubleProperty prefWidthProperty() {
        return prefWidth;
    }

    public int getColumnIndex() {
        return columnIndex.get();
    }

    public IntegerProperty columnIndexProperty() {
        return columnIndex;
    }

    public int getRowIndex() {
        return rowIndex.get();
    }

    public IntegerProperty rowIndexProperty() {
        return rowIndex;
    }

    public EventHandler<ActionEvent> getOnAction() {
        return onAction.get();
    }

    public ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        return onAction;
    }

    public GridPane getGridPane() {
        return gridPane.get();
    }

    public ObjectProperty<GridPane> gridPaneProperty() {
        return gridPane;
    }

}
