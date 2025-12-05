package com.hanson.hotelreservationsystem.service;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for handling navigation between different screens in the application.
 * Implements the Singleton pattern for centralized navigation management.
 * Handles automatic window resizing based on User vs Admin context.
 */
public class NavigationService {

    private static final Logger LOGGER = Logger.getLogger(NavigationService.class.getName());

    private static NavigationService instance;
    private Stage primaryStage;

    // ==================== Window Dimensions ====================
    private static final double KIOSK_WIDTH = 900;
    private static final double KIOSK_HEIGHT = 800;

    // ==================== FXML Paths: Kiosk Screens ====================
    public static final String KIOSK_WELCOME = "/com/hanson/hotelreservationsystem/kiosk/kioskWelcome.fxml";
    public static final String KIOSK_GUEST_COUNT = "/com/hanson/hotelreservationsystem/kiosk/kioskGuestCount.fxml";
    public static final String KIOSK_DATE_SELECTION = "/com/hanson/hotelreservationsystem/kiosk/kioskDateSelection.fxml";
    public static final String KIOSK_ROOM_SELECTION = "/com/hanson/hotelreservationsystem/kiosk/kioskRoomSelection.fxml";
    public static final String KIOSK_GUEST_DETAILS = "/com/hanson/hotelreservationsystem/kiosk/kioskGuestDetails.fxml";
    public static final String KIOSK_ADDON_SERVICES = "/com/hanson/hotelreservationsystem/kiosk/kioskAddOnServices.fxml";
    public static final String KIOSK_BOOKING_SUMMARY = "/com/hanson/hotelreservationsystem/kiosk/kioskBookingSummary.fxml";
    public static final String KIOSK_CONFIRMATION = "/com/hanson/hotelreservationsystem/kiosk/kioskConfirmation.fxml";

    // ==================== FXML Paths: Admin Screens ====================
    public static final String ADMIN_LOGIN = "/com/hanson/hotelreservationsystem/admin/adminLogin.fxml";
    public static final String ADMIN_DASHBOARD = "/com/hanson/hotelreservationsystem/admin/adminDashboard.fxml";
    public static final String ADMIN_RESERVATION_DETAILS = "/com/hanson/hotelreservationsystem/admin/adminReservationDetails.fxml";
    public static final String ADMIN_FORM = "/com/hanson/hotelreservationsystem/admin/adminReservationForm.fxml";
    public static final String ADMIN_PAYMENT = "/com/hanson/hotelreservationsystem/admin/adminPayment.fxml";
    public static final String ADMIN_CHECKOUT = "/com/hanson/hotelreservationsystem/admin/adminCheckout.fxml";
    public static final String ADMIN_LOYALTY_DASHBOARD = "/com/hanson/hotelreservationsystem/admin/adminLoyaltyDashboard.fxml";
    public static final String ADMIN_FEEDBACK_VIEWER = "/com/hanson/hotelreservationsystem/admin/adminFeedbackViewer.fxml";
    public static final String ADMIN_REPORTS = "/com/hanson/hotelreservationsystem/admin/adminReports.fxml";
    public static final String ADMIN_WAITLIST = "/com/hanson/hotelreservationsystem/admin/adminWaitlist.fxml";

    // ==================== FXML Paths: Shared/Other Screens ====================
    public static final String GUEST_FEEDBACK = "/com/hanson/hotelreservationsystem/guestFeedback.fxml";
    public static final String RULES_REGULATIONS = "/com/hanson/hotelreservationsystem/rulesRegulations.fxml";

    private NavigationService() {
        // Private constructor for Singleton
    }

    /**
     * Get the singleton instance of NavigationService.
     */
    public static synchronized NavigationService getInstance() {
        if (instance == null) {
            instance = new NavigationService();
        }
        return instance;
    }

    /**
     * Set the primary stage for navigation.
     * Should be called during application initialization.
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    /**
     * Get the primary stage.
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Adjusts the window size and state based on the screen type.
     * - Admin Dashboard/Tools: Maximized
     * - Admin Login & Kiosk: Normal fixed size
     */
    private void configureWindowSize(String fxmlPath) {
        if (primaryStage == null) return;

        boolean isAdminSection = fxmlPath.contains("/admin/");
        // We typically want the Login screen to be small, even though it is an admin screen
        boolean isLoginScreen = fxmlPath.equals(ADMIN_LOGIN);

        if (isAdminSection && !isLoginScreen) {
            // MAXIMIZED MODE
            if (!primaryStage.isMaximized()) {
                primaryStage.setMaximized(true);
            }
        } else {
            // NORMAL/KIOSK MODE
            if (primaryStage.isMaximized()) {
                primaryStage.setMaximized(false);
            }
            primaryStage.setWidth(KIOSK_WIDTH);
            primaryStage.setHeight(KIOSK_HEIGHT);
            primaryStage.centerOnScreen();
        }
    }

    /**
     * Navigate to a new screen by FXML path.
     *
     * @param fxmlPath the path to the FXML file
     */
    public void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Scene scene = primaryStage.getScene();

            if (scene == null) {
                // First time loading (Application start)
                scene = new Scene(root);
                primaryStage.setScene(scene);
            } else {
                // SWAP: Keep the window size/state, just change the content
                scene.setRoot(root);
            }

            // Apply CSS if exists
            String cssPath = fxmlPath.replace(".fxml", ".css");
            if (getClass().getResource(cssPath) != null) {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            }

            // --- ADJUST WINDOW SIZE LOGIC ---
            configureWindowSize(fxmlPath);
            // --------------------------------

            primaryStage.show();

            LOGGER.info("Navigated to: " + fxmlPath);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to: " + fxmlPath, e);
            throw new RuntimeException("Navigation failed: " + fxmlPath, e);
        }
    }

    /**
     * Navigate to a new screen and get the controller instance.
     *
     * @param fxmlPath the path to the FXML file
     * @param <T> the controller type
     * @return the controller instance
     */
    public <T> T navigateToAndGetController(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = primaryStage.getScene();

            if (scene == null) {
                // First time loading
                scene = new Scene(root);
                primaryStage.setScene(scene);
            } else {
                // SWAP: Keep the window size/state, just change the content
                scene.setRoot(root);
            }

            // Apply CSS if exists
            String cssPath = fxmlPath.replace(".fxml", ".css");
            if (getClass().getResource(cssPath) != null) {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            }

            // --- ADJUST WINDOW SIZE LOGIC ---
            configureWindowSize(fxmlPath);
            // --------------------------------

            primaryStage.show();

            LOGGER.info("Navigated to: " + fxmlPath);
            return loader.getController();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to: " + fxmlPath, e);
            throw new RuntimeException("Navigation failed: " + fxmlPath, e);
        }
    }

    // ==================== Kiosk Navigation Methods ====================

    public void goToWelcome() {
        navigateTo(KIOSK_WELCOME);
    }

    public void goToGuestCount() {
        navigateTo(KIOSK_GUEST_COUNT);
    }

    public void goToDateSelection() {
        navigateTo(KIOSK_DATE_SELECTION);
    }

    public void goToRoomSelection() {
        navigateTo(KIOSK_ROOM_SELECTION);
    }

    public void goToGuestDetails() {
        navigateTo(KIOSK_GUEST_DETAILS);
    }

    public void goToAddOnServices() {
        navigateTo(KIOSK_ADDON_SERVICES);
    }

    public void goToBookingSummary() {
        navigateTo(KIOSK_BOOKING_SUMMARY);
    }

    public void goToConfirmation() {
        navigateTo(KIOSK_CONFIRMATION);
    }

    public void goToFeedback() {
        navigateTo(GUEST_FEEDBACK);
    }

    // ==================== Admin Navigation Methods ====================

    public void goToAdminLogin() {
        navigateTo(ADMIN_LOGIN);
    }

    public void goToAdminDashboard() {
        navigateTo(ADMIN_DASHBOARD);
    }

    public void goToAdminPayment() {
        navigateTo(ADMIN_PAYMENT);
    }

    public void goToAdminReservationDetails() {
        navigateTo(ADMIN_RESERVATION_DETAILS);
    }

    public void goToAdminCheckout() {
        navigateTo(ADMIN_CHECKOUT);
    }

    public void goToAdminReservationForm() {
        navigateTo(ADMIN_FORM);
    }

    public void goToAdminWaitlist(){ navigateTo(ADMIN_WAITLIST);}

    public void goToAdminLoyaltyDashboard() {
        navigateTo(ADMIN_LOYALTY_DASHBOARD);
    }

    public void goToAdminFeedbackViewer() {
        navigateTo(ADMIN_FEEDBACK_VIEWER);
    }

    public void goToAdminReports() {
        navigateTo(ADMIN_REPORTS);
    }

    // ==================== Utility Methods ====================

    /**
     * Show rules and regulations in a dialog or new window.
     */
    public void showRulesAndRegulations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(RULES_REGULATIONS));
            Parent root = loader.load();

            Stage rulesStage = new Stage();
            rulesStage.setTitle("Rules and Regulations - ARC Hotel");
            rulesStage.setScene(new Scene(root));
            rulesStage.initOwner(primaryStage);
            rulesStage.show();

            LOGGER.info("Opened Rules and Regulations dialog");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Rules and Regulations FXML not found, showing alert instead", e);
        }
    }
}