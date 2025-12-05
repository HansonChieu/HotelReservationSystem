package com.hanson.hotelreservationsystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class RulesAndRegulationsController {

    @FXML
    private Button closeButton; // Assuming the close button has fx:id="closeButton"

    /**
     * Closes the Rules and Regulations window.
     */
    @FXML
    private void handleCloseRules() {
        // Get the stage associated with the close button and hide it
        Stage stage = (Stage) ((Button) closeButton).getScene().getWindow();
        stage.close();
    }
}