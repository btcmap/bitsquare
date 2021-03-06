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

package io.bitsquare.gui.popups;

import io.bitsquare.common.handlers.ResultHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static io.bitsquare.gui.util.FormBuilder.addMultilineLabel;

public class OpenEmergencyTicketPopup extends Popup {
    private static final Logger log = LoggerFactory.getLogger(OpenEmergencyTicketPopup.class);
    private Button openTicketButton;
    private ResultHandler openTicketHandler;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public OpenEmergencyTicketPopup() {
    }

    public OpenEmergencyTicketPopup show() {
        if (headLine == null)
            headLine = "Open support ticket";

        width = 700;
        createGridPane();
        addHeadLine();
        addContent();
        createPopup();
        return this;
    }

    public OpenEmergencyTicketPopup onOpenTicket(ResultHandler openTicketHandler) {
        this.openTicketHandler = openTicketHandler;
        return this;
    }

    public OpenEmergencyTicketPopup onClose(Runnable closeHandler) {
        this.closeHandlerOptional = Optional.of(closeHandler);
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void addContent() {
        addMultilineLabel(gridPane, ++rowIndex,
                "Please use that only in emergency case if you don't get displayed a support or dispute screen in the UI.\n" +
                        "When you open a ticket the trade will be interrupted and handled by the arbitrator.",
                10);


        openTicketButton = new Button("Open support ticket");
        openTicketButton.setOnAction(e -> {
            openTicketHandler.handleResult();
            hide();
        });

        closeButton = new Button("Cancel");
        closeButton.setOnAction(e -> {
            hide();
            closeHandlerOptional.ifPresent(closeHandler -> closeHandler.run());
        });

        HBox hBox = new HBox();
        hBox.setSpacing(10);
        GridPane.setRowIndex(hBox, ++rowIndex);
        GridPane.setColumnIndex(hBox, 1);
        hBox.getChildren().addAll(openTicketButton, closeButton);
        gridPane.getChildren().add(hBox);
        GridPane.setMargin(hBox, new Insets(10, 0, 0, 0));
    }


}
