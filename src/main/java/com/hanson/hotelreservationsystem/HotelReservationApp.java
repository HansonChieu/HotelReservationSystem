package com.hanson.hotelreservationsystem;

import com.hanson.hotelreservationsystem.config.ServiceInitializer;
import com.hanson.hotelreservationsystem.service.NavigationService;
import com.hanson.hotelreservationsystem.session.BookingSession;
import com.hanson.hotelreservationsystem.util.ActivityLogger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application launcher for the ARC Hotel Reservation System.
 */
public class HotelReservationApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(HotelReservationApp.class.getName());

    private static final String APP_TITLE = "ARC Hotel - Self Service Kiosk";
    private static final int WINDOW_WIDTH = 900;
    private static final int WINDOW_HEIGHT = 700;
    private static final String WELCOME_FXML = "/com/hanson/hotelreservationsystem/kiosk/kioskWelcome.fxml";

    @Override
    public void init() throws Exception {
        super.init();
        LOGGER.info("Initializing Hotel Reservation Application...");

        // Initialize the activity logger
        ActivityLogger.getInstance().initialize();

        // ✅ CRITICAL: Initialize all services with their repository dependencies
        // This is the central DI configuration per assignment requirements
        ServiceInitializer.initialize();

        // Pre-initialize session singletons
        BookingSession.getInstance();
        NavigationService.getInstance();

        LOGGER.info("Application initialization complete");
    }

    @Override
    public void start(Stage primaryStage) {
        LOGGER.info("Starting Hotel Reservation Application...");

        try {
            NavigationService.getInstance().setPrimaryStage(primaryStage);
            Parent root = FXMLLoader.load(getClass().getResource(WELCOME_FXML));
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.setResizable(true);
            primaryStage.show();

            LOGGER.info("Application started successfully - Welcome screen displayed");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load welcome screen", e);
            showErrorAndExit("Failed to load the application. Please contact support.", e);
        }
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Shutting down Hotel Reservation Application...");

        BookingSession.getInstance().reset();

        // ✅ Properly shutdown JPA resources
        ServiceInitializer.shutdown();

        LOGGER.info("Application shutdown complete");
        super.stop();
    }

    private void showErrorAndExit(String message, Exception e) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle("Application Error");
        alert.setHeaderText("Failed to Start Application");
        alert.setContentText(message + "\n\nError: " + e.getMessage());
        alert.showAndWait();
        System.exit(1);
    }

    public static void main(String[] args) {
        LOGGER.info("Launching ARC Hotel Reservation System...");
        launch(args);
    }
}