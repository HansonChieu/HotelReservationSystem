package com.hanson.hotelreservationsystem.controller.Kiosk;

import com.hanson.hotelreservationsystem.service.NavigationService;
import com.hanson.hotelreservationsystem.service.LoyaltyService;
import com.hanson.hotelreservationsystem.service.ValidationService;
import com.hanson.hotelreservationsystem.session.BookingSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Controller for the Kiosk Guest Details Screen (Step 4 of 5).
 *
 * Responsibilities:
 * - Collect guest personal information
 * - Validate all required fields with inline validation
 * - Check for existing loyalty membership
 * - Offer loyalty program registration
 *
 * Validation Rules:
 * - First Name: Required, letters only
 * - Last Name: Required, letters only
 * - Email: Required, valid email format
 * - Phone: Required, valid phone format
 * - Country: Required
 * - Address, City, State, Postal Code: Required
 * - ID/Passport: Optional
 * - Special Requests: Optional
 */
public class KioskGuestDetailsController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(KioskGuestDetailsController.class.getName());

    // Validation patterns
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s'-]{2,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[\\d\\s()+-]{10,20}$");
    private static final Pattern POSTAL_PATTERN = Pattern.compile("^[A-Za-z0-9\\s-]{3,10}$");

    // Form fields
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField countryField;
    @FXML private TextField idNumberField;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private TextField stateField;
    @FXML private TextField postalCodeField;
    @FXML private TextField specialRequestsField;

    // Error labels
    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label countryError;

    // Loyalty program components
    @FXML private VBox loyaltyStatusBox;
    @FXML private VBox loyaltyInfoBox;
    @FXML private VBox registrationPromptBox;
    @FXML private Label loyaltyNumberLabel;
    @FXML private Label pointsEarnedLabel;
    @FXML private Label memberSinceLabel;
    @FXML private Label bonusPointsLabel;

    // Navigation buttons
    @FXML private Button backButton;
    @FXML private Button nextButton;

    // Services
    private NavigationService navigationService;
    private BookingSession bookingSession;
    private LoyaltyService loyaltyService;
    private ValidationService validationService;

    // State
    private boolean loyaltyChecked = false;
    private boolean isExistingMember = false;

    /**
     * Default constructor for FXML loader.
     */
    public KioskGuestDetailsController() {
        this.navigationService = NavigationService.getInstance();
        this.bookingSession = BookingSession.getInstance();
    }

    /**
     * Constructor with dependency injection.
     */
    public KioskGuestDetailsController(NavigationService navigationService,
                                       BookingSession bookingSession,
                                       LoyaltyService loyaltyService,
                                       ValidationService validationService) {
        this.navigationService = navigationService;
        this.bookingSession = bookingSession;
        this.loyaltyService = loyaltyService;
        this.validationService = validationService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Kiosk Guest Details Screen (Step 4)");

        setupFieldListeners();
        loadExistingValues();
        hideLoyaltySection();
    }

    /**
     * Setup listeners for real-time validation.
     */
    private void setupFieldListeners() {
        // Add focus lost listeners for validation
        if (firstNameField != null) {
            firstNameField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) validateFirstName();
            });
        }

        if (lastNameField != null) {
            lastNameField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) validateLastName();
            });
        }

        if (emailField != null) {
            emailField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    validateEmail();
                    checkLoyaltyMembership();
                }
            });
        }

        if (phoneField != null) {
            phoneField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    validatePhone();
                    if (!loyaltyChecked) checkLoyaltyMembership();
                }
            });
        }
    }

    /**
     * Load any existing values from the booking session.
     */
    private void loadExistingValues() {
        if (bookingSession.getFirstName() != null && firstNameField != null) {
            firstNameField.setText(bookingSession.getFirstName());
        }
        if (bookingSession.getLastName() != null && lastNameField != null) {
            lastNameField.setText(bookingSession.getLastName());
        }
        if (bookingSession.getEmail() != null && emailField != null) {
            emailField.setText(bookingSession.getEmail());
        }
        if (bookingSession.getPhone() != null && phoneField != null) {
            phoneField.setText(bookingSession.getPhone());
        }
        if (bookingSession.getCountry() != null && countryField != null) {
            countryField.setText(bookingSession.getCountry());
        }
        if (bookingSession.getIdNumber() != null && idNumberField != null) {
            idNumberField.setText(bookingSession.getIdNumber());
        }
        if (bookingSession.getAddress() != null && addressField != null) {
            addressField.setText(bookingSession.getAddress());
        }
        if (bookingSession.getCity() != null && cityField != null) {
            cityField.setText(bookingSession.getCity());
        }
        if (bookingSession.getState() != null && stateField != null) {
            stateField.setText(bookingSession.getState());
        }
        if (bookingSession.getPostalCode() != null && postalCodeField != null) {
            postalCodeField.setText(bookingSession.getPostalCode());
        }
        if (bookingSession.getSpecialRequests() != null && specialRequestsField != null) {
            specialRequestsField.setText(bookingSession.getSpecialRequests());
        }

        // If already a loyalty member, show info
        if (bookingSession.isLoyaltyMember()) {
            showLoyaltyMemberInfo();
        }
    }

    // ==================== Field Change Handlers ====================

    @FXML
    public void handleFirstNameChange(KeyEvent event) {
        clearError(firstNameError);
    }

    @FXML
    public void handleLastNameChange(KeyEvent event) {
        clearError(lastNameError);
    }

    @FXML
    public void handleEmailChange(KeyEvent event) {
        clearError(emailError);
        loyaltyChecked = false;
    }

    @FXML
    public void handlePhoneChange(KeyEvent event) {
        clearError(phoneError);
        loyaltyChecked = false;
    }

    // ==================== Validation Methods ====================

    private boolean validateFirstName() {
        String value = getFieldValue(firstNameField);

        if (value.isEmpty()) {
            showError(firstNameError, "First name is required");
            return false;
        }

        if (!NAME_PATTERN.matcher(value).matches()) {
            showError(firstNameError, "Please enter a valid first name");
            return false;
        }

        clearError(firstNameError);
        return true;
    }

    private boolean validateLastName() {
        String value = getFieldValue(lastNameField);

        if (value.isEmpty()) {
            showError(lastNameError, "Last name is required");
            return false;
        }

        if (!NAME_PATTERN.matcher(value).matches()) {
            showError(lastNameError, "Please enter a valid last name");
            return false;
        }

        clearError(lastNameError);
        return true;
    }

    private boolean validateEmail() {
        String value = getFieldValue(emailField);

        if (value.isEmpty()) {
            showError(emailError, "Email address is required");
            return false;
        }

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            showError(emailError, "Please enter a valid email address");
            return false;
        }

        clearError(emailError);
        return true;
    }

    private boolean validatePhone() {
        String value = getFieldValue(phoneField);

        if (value.isEmpty()) {
            showError(phoneError, "Phone number is required");
            return false;
        }

        if (!PHONE_PATTERN.matcher(value).matches()) {
            showError(phoneError, "Please enter a valid phone number");
            return false;
        }

        clearError(phoneError);
        return true;
    }

    private boolean validateCountry() {
        String value = getFieldValue(countryField);

        if (value.isEmpty()) {
            showError(countryError, "Country is required");
            return false;
        }

        clearError(countryError);
        return true;
    }

    private boolean validateAddress() {
        return !getFieldValue(addressField).isEmpty();
    }

    private boolean validateCity() {
        return !getFieldValue(cityField).isEmpty();
    }

    private boolean validateState() {
        return !getFieldValue(stateField).isEmpty();
    }

    private boolean validatePostalCode() {
        String value = getFieldValue(postalCodeField);
        return !value.isEmpty() && POSTAL_PATTERN.matcher(value).matches();
    }

    /**
     * Validate all required fields.
     */
    private boolean validateAllFields() {
        boolean valid = true;

        valid &= validateFirstName();
        valid &= validateLastName();
        valid &= validateEmail();
        valid &= validatePhone();
        valid &= validateCountry();

        // Check address fields
        if (!validateAddress() || !validateCity() || !validateState() || !validatePostalCode()) {
            showAlert("Missing Information", "Please fill in all address fields.");
            valid = false;
        }

        return valid;
    }

    // ==================== Loyalty Program Methods ====================

    /**
     * Check if the guest is an existing loyalty member.
     */
    private void checkLoyaltyMembership() {
        String email = getFieldValue(emailField);
        String phone = getFieldValue(phoneField);

        if (email.isEmpty() && phone.isEmpty()) {
            return;
        }

        loyaltyChecked = true;

        // In production, this would call loyaltyService.findMember(email, phone)
        if (loyaltyService != null) {
            // LoyaltyMember member = loyaltyService.findMemberByEmailOrPhone(email, phone);
            // if (member != null) {
            //     isExistingMember = true;
            //     showLoyaltyMemberInfo(member);
            // } else {
            //     showLoyaltyRegistrationPrompt();
            // }
        } else {
            // For demo, simulate no existing member
            showLoyaltyRegistrationPrompt();
        }
    }

    /**
     * Hide the loyalty section initially.
     */
    private void hideLoyaltySection() {
        if (loyaltyStatusBox != null) {
            loyaltyStatusBox.setVisible(false);
            loyaltyStatusBox.setManaged(false);
        }
    }

    /**
     * Show loyalty member information for existing members.
     */
    private void showLoyaltyMemberInfo() {
        if (loyaltyStatusBox != null) {
            loyaltyStatusBox.setVisible(true);
            loyaltyStatusBox.setManaged(true);
        }

        if (loyaltyInfoBox != null) {
            loyaltyInfoBox.setVisible(true);
            loyaltyInfoBox.setManaged(true);
        }

        if (registrationPromptBox != null) {
            registrationPromptBox.setVisible(false);
            registrationPromptBox.setManaged(false);
        }

        // Update labels with member info
        if (loyaltyNumberLabel != null) {
            loyaltyNumberLabel.setText(bookingSession.getLoyaltyNumber());
        }
        if (pointsEarnedLabel != null) {
            pointsEarnedLabel.setText(bookingSession.getLoyaltyPoints() + " pts");
        }

        isExistingMember = true;
    }

    /**
     * Show loyalty registration prompt for new guests.
     */
    private void showLoyaltyRegistrationPrompt() {
        if (loyaltyStatusBox != null) {
            loyaltyStatusBox.setVisible(true);
            loyaltyStatusBox.setManaged(true);
        }

        if (loyaltyInfoBox != null) {
            loyaltyInfoBox.setVisible(false);
            loyaltyInfoBox.setManaged(false);
        }

        if (registrationPromptBox != null) {
            registrationPromptBox.setVisible(true);
            registrationPromptBox.setManaged(true);
        }

        isExistingMember = false;
    }

    /**
     * Handle loyalty registration acceptance.
     */
    @FXML
    public void handleLoyaltyRegistration(ActionEvent event) {
        LOGGER.info("Guest opted to join loyalty program");
        bookingSession.setWantsToJoinLoyalty(true);

        // Show confirmation
        showAlert("Welcome!", "You will be enrolled in ARC Rewards after your reservation is confirmed.");

        // Update UI to show pending status
        if (registrationPromptBox != null) {
            registrationPromptBox.setVisible(false);
            registrationPromptBox.setManaged(false);
        }
    }

    /**
     * Handle loyalty registration decline.
     */
    @FXML
    public void handleLoyaltyDecline(ActionEvent event) {
        LOGGER.info("Guest declined loyalty program");
        bookingSession.setWantsToJoinLoyalty(false);

        // Hide the loyalty section
        hideLoyaltySection();
    }

    // ==================== Utility Methods ====================

    private String getFieldValue(TextField field) {
        return field != null && field.getText() != null ? field.getText().trim() : "";
    }

    private void clearError(Label errorLabel) {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
    }

    private void showError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Save all guest details to the booking session.
     */
    private void saveGuestDetails() {
        bookingSession.setFirstName(getFieldValue(firstNameField));
        bookingSession.setLastName(getFieldValue(lastNameField));
        bookingSession.setEmail(getFieldValue(emailField));
        bookingSession.setPhone(getFieldValue(phoneField));
        bookingSession.setCountry(getFieldValue(countryField));
        bookingSession.setIdNumber(getFieldValue(idNumberField));
        bookingSession.setAddress(getFieldValue(addressField));
        bookingSession.setCity(getFieldValue(cityField));
        bookingSession.setState(getFieldValue(stateField));
        bookingSession.setPostalCode(getFieldValue(postalCodeField));
        bookingSession.setSpecialRequests(getFieldValue(specialRequestsField));

        bookingSession.setLoyaltyMember(isExistingMember);

        LOGGER.info("Guest details saved: " + bookingSession.getFullName());
    }

    // ==================== Navigation Handlers ====================

    /**
     * Handle the "Back" button click.
     */
    @FXML
    public void handleBack(ActionEvent event) {
        LOGGER.info("User clicked Back - returning to room selection");
        saveGuestDetails(); // Save partial progress
        navigationService.goToRoomSelection();
    }

    /**
     * Handle the "Next" button click.
     */
    @FXML
    public void handleNext(ActionEvent event) {
        LOGGER.info("User clicked Next - validating guest details");

        if (!validateAllFields()) {
            LOGGER.warning("Guest details validation failed");
            return;
        }

        saveGuestDetails();

        // Navigate to Step 5: Add-On Services
        navigationService.goToAddOnServices();
    }

    /**
     * Handle the "Rules and Regulations" button click.
     */
    @FXML
    public void handleRulesAndRegulations(ActionEvent event) {
        LOGGER.info("User clicked Rules and Regulations");
        navigationService.showRulesAndRegulations();
    }

    // Setters for dependency injection
    public void setNavigationService(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    public void setBookingSession(BookingSession bookingSession) {
        this.bookingSession = bookingSession;
    }

    public void setLoyaltyService(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    public void setValidationService(ValidationService validationService) {
        this.validationService = validationService;
    }
}