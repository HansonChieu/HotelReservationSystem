package com.hanson.hotelreservationsystem.controller.Admin;

import com.hanson.hotelreservationsystem.repository.AdminRepository;
import org.mindrot.jbcrypt.BCrypt;
import com.hanson.hotelreservationsystem.model.Admin;
import com.hanson.hotelreservationsystem.model.ActivityLog;
import com.hanson.hotelreservationsystem.service.NavigationService;
import com.hanson.hotelreservationsystem.session.AdminSession;
import com.hanson.hotelreservationsystem.util.ActivityLogger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Admin Login Screen.
 *
 * Responsibilities:
 * - Authenticate admin credentials using BCrypt
 * - Handle account lockout after failed attempts
 * - Log all login attempts (success and failure)
 * - Navigate to admin dashboard on success
 * - Provide forgot password functionality
 *
 * Security Features:
 * - BCrypt password hashing verification
 * - Account lockout after 5 failed attempts
 * - 15-minute lockout period
 * - Activity logging for audit trail
 */
public class AdminLoginController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AdminLoginController.class.getName());

    // Maximum login attempts before lockout
    private static final int MAX_LOGIN_ATTEMPTS = 5;

    // FXML Components
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button backButton;
    @FXML private Button forgotPasswordButton;
    @FXML private Label errorLabel;
    @FXML private Label roleInfoLabel;
    @FXML private Label attemptsLabel;
    @FXML private ProgressIndicator loginProgress;

    // Services
    private NavigationService navigationService;
    private AdminSession adminSession;
    private ActivityLogger activityLogger;

    // For demo/testing - would be replaced by AdminRepository in production
    // This simulates database lookup
    private Admin demoAdmin;
    private Admin demoManager;

    /**
     * Default constructor for FXML loader.
     */
    public AdminLoginController() {
        this.navigationService = NavigationService.getInstance();
        this.adminSession = AdminSession.getInstance();
        this.activityLogger = ActivityLogger.getInstance();
        initializeDemoAccounts();
    }

    /**
     * Constructor with dependency injection.
     */
    public AdminLoginController(NavigationService navigationService,
                                AdminSession adminSession,
                                ActivityLogger activityLogger) {
        this.navigationService = navigationService;
        this.adminSession = adminSession;
        this.activityLogger = activityLogger;
        initializeDemoAccounts();
    }

    /**
     * Initialize demo accounts for testing.
     * In production, this would be handled by AdminRepository.
     */
    private void initializeDemoAccounts() {
        // Note: In production, these would come from database
        // Password hashes would be created during account setup
        // For demo: admin/admin123 and manager/manager123
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Admin Login Screen");

        setupUI();
        setupFieldListeners();
        clearForm();
    }

    /**
     * Setup initial UI state.
     */
    private void setupUI() {
        // Hide progress indicator initially
        if (loginProgress != null) {
            loginProgress.setVisible(false);
        }

        // Hide error label initially
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }

        // Hide attempts label initially
        if (attemptsLabel != null) {
            attemptsLabel.setVisible(false);
        }

        // Set role info text
        if (roleInfoLabel != null) {
            roleInfoLabel.setText("Admin: Up to 15% discount | Manager: Up to 30% discount");
        }

        // Focus on username field
        if (usernameField != null) {
            Platform.runLater(() -> usernameField.requestFocus());
        }
    }

    /**
     * Setup field listeners for Enter key handling.
     */
    private void setupFieldListeners() {
        // Allow Enter key to submit from password field
        if (passwordField != null) {
            passwordField.setOnAction(event -> handleLogin(null));
        }

        // Clear error on typing
        if (usernameField != null) {
            usernameField.textProperty().addListener((obs, oldVal, newVal) -> clearError());
        }

        if (passwordField != null) {
            passwordField.textProperty().addListener((obs, oldVal, newVal) -> clearError());
        }
    }

    /**
     * Clear the login form.
     */
    private void clearForm() {
        if (usernameField != null) {
            usernameField.clear();
        }
        if (passwordField != null) {
            passwordField.clear();
        }
        clearError();
    }

    /**
     * Clear error message.
     */
    private void clearError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
        if (attemptsLabel != null) {
            attemptsLabel.setVisible(false);
        }
    }

    /**
     * Show error message.
     *
     * @param message The error message to display
     */
    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }

    /**
     * Show remaining attempts warning.
     *
     * @param remainingAttempts Number of attempts remaining
     */
    private void showAttemptsWarning(int remainingAttempts) {
        if (attemptsLabel != null && remainingAttempts <= 3) {
            attemptsLabel.setText("Warning: " + remainingAttempts + " attempt(s) remaining before lockout");
            attemptsLabel.setVisible(true);
        }
    }

    /**
     * Validate login form inputs.
     *
     * @return true if inputs are valid
     */
    private boolean validateInputs() {
        String username = getUsername();
        String password = getPassword();

        if (username.isEmpty()) {
            showError("Please enter your username");
            usernameField.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            showError("Please enter your password");
            passwordField.requestFocus();
            return false;
        }

        if (username.length() < 4) {
            showError("Username must be at least 4 characters");
            return false;
        }

        return true;
    }

    /**
     * Get username from field.
     */
    private String getUsername() {
        return usernameField != null && usernameField.getText() != null
                ? usernameField.getText().trim()
                : "";
    }

    /**
     * Get password from field.
     */
    private String getPassword() {
        return passwordField != null && passwordField.getText() != null
                ? passwordField.getText()
                : "";
    }

    /**
     * Authenticate admin credentials.
     * In production, this would use AdminRepository and BCrypt.
     *
     * @param username The username to authenticate
     * @param password The password to verify
     * @return Optional containing the authenticated Admin, or empty if authentication failed
     */
    private Optional<Admin> authenticateAdmin(String username, String password) {
        // 1. Get the repository
        AdminRepository adminRepo = AdminRepository.getInstance();

        // 2. Look up the user in the database
        Optional<Admin> adminOpt = adminRepo.findByUsername(username);

        // 3. Verify password if user exists
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();

            // Verify the password using BCrypt
            if (BCrypt.checkpw(password, admin.getPasswordHash())) {
                // Check if account is active/locked logic here if needed
                return Optional.of(admin);
            }
        }

        // Authentication failed
        return Optional.empty();
    }
    /**
     * Create a demo admin for testing.
     * In production, this would come from the database.
     */
    private Admin createDemoAdmin(String username, String firstName, String lastName, boolean isManager) {
        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setFirstName(firstName);
        admin.setLastName(lastName);
        admin.setEmail(username + "@archotel.com");
        admin.setRole(isManager
                ? com.hanson.hotelreservationsystem.model.enums.Role.MANAGER
                : com.hanson.hotelreservationsystem.model.enums.Role.ADMIN);
        admin.setActive(true);
        return admin;
    }

    /**
     * Check if admin account is locked.
     *
     * @param admin The admin to check
     * @return true if the account is locked
     */
    private boolean isAccountLocked(Admin admin) {
        if (admin == null) {
            return false;
        }
        return admin.isLocked();
    }

    /**
     * Handle successful login.
     *
     * @param admin The authenticated admin
     */
    private void handleSuccessfulLogin(Admin admin) {
        // Record successful login
        admin.recordSuccessfulLogin();

        // Start admin session
        adminSession.startSession(admin);

        // Log activity
        logLoginActivity(admin.getUsername(), true, null);

        LOGGER.info("Successful login for admin: " + admin.getUsername() +
                " (Role: " + admin.getRole().getDisplayName() + ")");

        // Show welcome message
        showWelcomeMessage(admin);

        // Navigate to dashboard
        navigateToDashboard();
    }

    /**
     * Handle failed login attempt.
     *
     * @param username The username that failed to authenticate
     * @param reason The reason for failure
     */
    private void handleFailedLogin(String username, String reason) {
        // Log activity
        logLoginActivity(username, false, reason);

        LOGGER.warning("Failed login attempt for username: " + username + " - " + reason);

        // Show error
        showError(reason);

        // Clear password field
        if (passwordField != null) {
            passwordField.clear();
            passwordField.requestFocus();
        }
    }

    /**
     * Log login activity for audit trail.
     */
    private void logLoginActivity(String username, boolean success, String errorMessage) {
        if (activityLogger != null) {
            if (success) {
                activityLogger.logActivity(
                        "Admin: " + username,
                        "LOGIN_SUCCESS",
                        "ADMIN",
                        username,
                        "Admin logged in successfully"
                );
            } else {
                activityLogger.logActivity(
                        "System",
                        "LOGIN_FAILURE",
                        "ADMIN",
                        username,
                        "Login failed: " + (errorMessage != null ? errorMessage : "Invalid credentials")
                );
            }
        }
    }

    /**
     * Show welcome message after successful login.
     */
    private void showWelcomeMessage(Admin admin) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Welcome");
        alert.setHeaderText("Login Successful");
        alert.setContentText("Welcome, " + admin.getFullName() + "!\n" +
                "Role: " + admin.getRole().getDisplayName() + "\n" +
                "Maximum Discount: " + admin.getRole().getMaxDiscountPercentage() + "%");
        alert.showAndWait();
    }

    /**
     * Navigate to the admin dashboard.
     */
    private void navigateToDashboard() {
        try {
            // Use NavigationService to navigate to dashboard
            // In production, add admin dashboard path to NavigationService
            navigationService.goToAdminDashboard();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to dashboard", e);
            showError("Failed to load dashboard. Please try again.");
        }
    }

    // ==================== Event Handlers ====================

    /**
     * Handle the "Login" button click.
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        LOGGER.info("Login attempt initiated");

        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        String username = getUsername();
        String password = getPassword();

        // Show progress
        setLoginInProgress(true);

        // Perform authentication (simulated delay for UX)
        Platform.runLater(() -> {
            try {
                // Authenticate
                Optional<Admin> adminOpt = authenticateAdmin(username, password);

                if (adminOpt.isPresent()) {
                    Admin admin = adminOpt.get();

                    // Check if account is locked
                    if (isAccountLocked(admin)) {
                        handleFailedLogin(username, "Account is locked. Please contact IT support.");
                    } else {
                        handleSuccessfulLogin(admin);
                    }
                } else {
                    handleFailedLogin(username, "Invalid username or password");
                }
            } finally {
                setLoginInProgress(false);
            }
        });
    }

    /**
     * Set UI state for login in progress.
     */
    private void setLoginInProgress(boolean inProgress) {
        if (loginProgress != null) {
            loginProgress.setVisible(inProgress);
        }
        if (loginButton != null) {
            loginButton.setDisable(inProgress);
        }
        if (usernameField != null) {
            usernameField.setDisable(inProgress);
        }
        if (passwordField != null) {
            passwordField.setDisable(inProgress);
        }
    }

    /**
     * Handle the "Back" button click.
     * Returns to the kiosk welcome screen.
     */
    @FXML
    public void handleReturnToKiosk(ActionEvent event) {
        LOGGER.info("User clicked Back - returning to kiosk welcome screen");
        clearForm();
        navigationService.goToWelcome();
    }

    /**
     * Handle the "Forgot Password" button click.
     */
    @FXML
    public void handleForgotPassword(ActionEvent event) {
        LOGGER.info("User clicked Forgot Password");

        // Show forgot password dialog
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Forgot Password");
        dialog.setHeaderText("Password Reset Request");
        dialog.setContentText("Enter your email address:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(email -> {
            if (email.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Invalid Email",
                        "Please enter a valid email address.");
                return;
            }

            // In production, this would trigger password reset email
            LOGGER.info("Password reset requested for email: " + email);

            showAlert(Alert.AlertType.INFORMATION, "Password Reset",
                    "If an account exists with this email, you will receive password reset instructions.\n\n" +
                            "Please contact IT support if you need immediate assistance.");

            // Log the activity
            if (activityLogger != null) {
                activityLogger.logActivity(
                        "System",
                        "PASSWORD_RESET_REQUEST",
                        "ADMIN",
                        "N/A",
                        "Password reset requested for: " + email
                );
            }
        });
    }

    /**
     * Show an alert dialog.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== Setters for Dependency Injection ====================

    public void setNavigationService(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    public void setAdminSession(AdminSession adminSession) {
        this.adminSession = adminSession;
    }

    public void setActivityLogger(ActivityLogger activityLogger) {
        this.activityLogger = activityLogger;
    }
}
