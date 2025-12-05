package com.hanson.hotelreservationsystem.controller.Admin;

import com.hanson.hotelreservationsystem.repository.GuestRepository;
import com.hanson.hotelreservationsystem.model.*;
import com.hanson.hotelreservationsystem.model.enums.*;
import com.hanson.hotelreservationsystem.service.*;
import com.hanson.hotelreservationsystem.session.AdminSession;
import com.hanson.hotelreservationsystem.util.ActivityLogger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Admin Loyalty Dashboard Screen.
 *
 * Responsibilities:
 * - Display loyalty program statistics
 * - Search and view member accounts
 * - View transaction history
 * - Manually adjust points (bonus, adjustment)
 * - Enroll new members
 * - View tier distribution
 *
 * Features:
 * - Member search by email, phone, or loyalty number
 * - Transaction filtering by type and date
 * - Points adjustment with audit trail
 * - Tier management view
 */
public class AdminLoyaltyDashboardController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AdminLoyaltyDashboardController.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // ==================== Statistics Cards ====================
    @FXML private Label totalMembersLabel;
    @FXML private Label pointsOutstandingLabel;
    @FXML private Label pointsRedeemedLabel;
    @FXML private Label averagePointsLabel;

    // ==================== Member Search ====================
    @FXML private TextField memberSearchField;
    @FXML private Button searchMemberButton;
    @FXML private ComboBox<String> tierFilter;

    // ==================== Members Table ====================
    @FXML private TableView<LoyaltyAccount> membersTable;
    @FXML private TableColumn<LoyaltyAccount, String> loyaltyNumberColumn;
    @FXML private TableColumn<LoyaltyAccount, String> memberNameColumn;
    @FXML private TableColumn<LoyaltyAccount, String> memberEmailColumn;
    @FXML private TableColumn<LoyaltyAccount, String> memberPhoneColumn;
    @FXML private TableColumn<LoyaltyAccount, String> pointsBalanceColumn;
    @FXML private TableColumn<LoyaltyAccount, String> tierColumn;
    @FXML private TableColumn<LoyaltyAccount, String> memberSinceColumn;
    @FXML private TableColumn<LoyaltyAccount, String> statusColumn;

    // ==================== Member Details Panel ====================
    @FXML private Label selectedMemberNameLabel;
    @FXML private Label selectedMemberNumberLabel;
    @FXML private Label selectedMemberTierLabel;
    @FXML private Label selectedMemberPointsLabel;
    @FXML private Label selectedMemberLifetimeLabel;
    @FXML private Label selectedMemberStatusLabel;

    // ==================== Transactions Table ====================
    @FXML private TableView<LoyaltyTransaction> transactionsTable;
    @FXML private TableColumn<LoyaltyTransaction, String> txnDateColumn;
    @FXML private TableColumn<LoyaltyTransaction, String> txnTypeColumn;
    @FXML private TableColumn<LoyaltyTransaction, String> txnPointsColumn;
    @FXML private TableColumn<LoyaltyTransaction, String> txnDescriptionColumn;
    @FXML private TableColumn<LoyaltyTransaction, String> txnReservationColumn;

    // ==================== Action Buttons ====================
    @FXML private Button addPointsButton;
    @FXML private Button deductPointsButton;
    @FXML private Button enrollMemberButton;
    @FXML private Button viewDetailsButton;
    @FXML private Button exportButton;

    // ==================== Data ====================
    private ObservableList<LoyaltyAccount> allMembers = FXCollections.observableArrayList();
    private FilteredList<LoyaltyAccount> filteredMembers;
    private ObservableList<LoyaltyTransaction> memberTransactions = FXCollections.observableArrayList();
    private LoyaltyAccount selectedMember;

    // ==================== Services ====================
    private NavigationService navigationService;
    private LoyaltyService loyaltyService;
    private AdminSession adminSession;
    private ActivityLogger activityLogger;
    private GuestRepository guestRepository = GuestRepository.getInstance();

    public AdminLoyaltyDashboardController() {
        this.navigationService = NavigationService.getInstance();
        this.loyaltyService = LoyaltyService.getInstance();
        this.adminSession = AdminSession.getInstance();
        this.activityLogger = ActivityLogger.getInstance();
    }

    public AdminLoyaltyDashboardController(NavigationService navigationService,
                                           LoyaltyService loyaltyService,
                                           AdminSession adminSession,
                                           ActivityLogger activityLogger) {
        this.navigationService = navigationService;
        this.loyaltyService = loyaltyService;
        this.adminSession = adminSession;
        this.activityLogger = activityLogger;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Admin Loyalty Dashboard");

        if (!adminSession.isLoggedIn()) {
            Platform.runLater(() -> navigationService.goToAdminLogin());
            return;
        }

        setupFilters();
        setupMembersTable();
        setupTransactionsTable();
        setupTableSelectionListener();
        loadLoyaltyData();
        updateStatistics();
        clearMemberDetails();

        logActivity("VIEW_LOYALTY_DASHBOARD", "LOYALTY", "N/A", "Admin accessed loyalty dashboard");
    }

    // ==================== Helper Methods ====================

    /**
     * Formats the tier string to a display-friendly format.
     * Converts "BRONZE" -> "Bronze", "SILVER" -> "Silver", etc.
     *
     * @param tier The raw tier string from the model
     * @return Formatted display name
     */
    private String formatTierDisplayName(String tier) {
        if (tier == null || tier.isEmpty()) {
            return "Bronze";
        }
        // Convert "BRONZE" -> "Bronze", "SILVER" -> "Silver", etc.
        return tier.substring(0, 1).toUpperCase() + tier.substring(1).toLowerCase();
    }

    /**
     * Adds points to a loyalty account (for admin adjustments).
     * Since the model doesn't have a simple addPoints method, we manipulate the balance directly.
     *
     * @param account The loyalty account
     * @param points Points to add
     */
    private void addPointsToAccount(LoyaltyAccount account, int points) {
        account.setPointsBalance(account.getPointsBalance() + points);
        account.setLifetimePoints(account.getLifetimePoints() + points);
        account.setLastActivityDate(LocalDate.now());
    }

    /**
     * Deducts points from a loyalty account (for admin adjustments).
     * Since the model doesn't have a simple deductPoints method, we manipulate the balance directly.
     *
     * @param account The loyalty account
     * @param points Points to deduct
     */
    private void deductPointsFromAccount(LoyaltyAccount account, int points) {
        account.setPointsBalance(account.getPointsBalance() - points);
        account.setLastActivityDate(LocalDate.now());
    }

    private void setupFilters() {
        if (tierFilter != null) {
            tierFilter.setItems(FXCollections.observableArrayList(
                    "All Tiers", "Bronze", "Silver", "Gold", "Platinum"
            ));
            tierFilter.getSelectionModel().selectFirst();
            tierFilter.setOnAction(e -> applyFilters());
        }

        // Search field listener
        if (memberSearchField != null) {
            memberSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
    }

    private void setupMembersTable() {
        if (membersTable == null) return;

        if (loyaltyNumberColumn != null) {
            loyaltyNumberColumn.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getLoyaltyNumber()));
        }

        if (memberNameColumn != null) {
            memberNameColumn.setCellValueFactory(cell -> {
                Guest guest = cell.getValue().getGuest();
                return new SimpleStringProperty(guest != null ? guest.getFullName() : "N/A");
            });
        }

        if (memberEmailColumn != null) {
            memberEmailColumn.setCellValueFactory(cell -> {
                Guest guest = cell.getValue().getGuest();
                return new SimpleStringProperty(guest != null ? guest.getEmail() : "N/A");
            });
        }

        if (memberPhoneColumn != null) {
            memberPhoneColumn.setCellValueFactory(cell -> {
                Guest guest = cell.getValue().getGuest();
                return new SimpleStringProperty(guest != null ? guest.getPhone() : "N/A");
            });
        }

        if (pointsBalanceColumn != null) {
            pointsBalanceColumn.setCellValueFactory(cell ->
                    new SimpleStringProperty(String.format("%,d pts", cell.getValue().getPointsBalance())));
        }

        if (tierColumn != null) {
            tierColumn.setCellValueFactory(cell -> {
                String tier = cell.getValue().getTier(); // FIXED: getTier() returns String, not enum
                return new SimpleStringProperty(formatTierDisplayName(tier));
            });

            tierColumn.setCellFactory(column -> new TableCell<LoyaltyAccount, String>() {
                @Override
                protected void updateItem(String tier, boolean empty) {
                    super.updateItem(tier, empty);
                    if (empty || tier == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(tier);
                        switch (tier) {
                            case "Platinum" -> setStyle("-fx-text-fill: #9c27b0; -fx-font-weight: bold;");
                            case "Gold" -> setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;");
                            case "Silver" -> setStyle("-fx-text-fill: #607d8b; -fx-font-weight: bold;");
                            default -> setStyle("-fx-text-fill: #795548;");
                        }
                    }
                }
            });
        }

        if (memberSinceColumn != null) {
            memberSinceColumn.setCellValueFactory(cell -> {
                LocalDate date = cell.getValue().getEnrollmentDate(); // FIXED: was getCreatedAt()
                return new SimpleStringProperty(date != null ? date.format(DATE_FORMAT) : "N/A");
            });
        }

        if (statusColumn != null) {
            statusColumn.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().isActive() ? "Active" : "Inactive"));
        }

        membersTable.setItems(allMembers);
    }

    private void setupTransactionsTable() {
        if (transactionsTable == null) return;

        if (txnDateColumn != null) {
            txnDateColumn.setCellValueFactory(cell -> {
                LocalDateTime date = cell.getValue().getTransactionDate();
                return new SimpleStringProperty(date != null ? date.format(DATETIME_FORMAT) : "N/A");
            });
        }

        if (txnTypeColumn != null) {
            txnTypeColumn.setCellValueFactory(cell -> {
                LoyaltyTransactionType type = cell.getValue().getTransactionType(); // FIXED: was getType()
                return new SimpleStringProperty(type != null ? type.getDisplayName() : "N/A");
            });

            txnTypeColumn.setCellFactory(column -> new TableCell<LoyaltyTransaction, String>() {
                @Override
                protected void updateItem(String type, boolean empty) {
                    super.updateItem(type, empty);
                    if (empty || type == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(type);
                        if (type.contains("Earn") || type.contains("Bonus")) {
                            setStyle("-fx-text-fill: #28a745;");
                        } else if (type.contains("Redeem") || type.contains("Expire")) {
                            setStyle("-fx-text-fill: #dc3545;");
                        } else {
                            setStyle("-fx-text-fill: #17a2b8;");
                        }
                    }
                }
            });
        }

        if (txnPointsColumn != null) {
            txnPointsColumn.setCellValueFactory(cell -> {
                int points = cell.getValue().getPoints();
                LoyaltyTransactionType type = cell.getValue().getTransactionType(); // FIXED: was getType()
                String prefix = (type != null && type.isCredit()) ? "+" : "-";
                return new SimpleStringProperty(prefix + String.format("%,d", Math.abs(points)));
            });
        }

        if (txnDescriptionColumn != null) {
            txnDescriptionColumn.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getDescription()));
        }

        if (txnReservationColumn != null) {
            txnReservationColumn.setCellValueFactory(cell -> {
                Long resId = cell.getValue().getReservationId(); // FIXED: was getReservation()
                return new SimpleStringProperty(resId != null ? "RES-" + resId : "-");
            });
        }

        transactionsTable.setItems(memberTransactions);
    }

    private void setupTableSelectionListener() {
        if (membersTable != null) {
            membersTable.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldSel, newSel) -> {
                        selectedMember = newSel;
                        updateMemberDetails(newSel);
                        loadMemberTransactions(newSel);
                        updateActionButtons(newSel);
                    });
        }
    }

    @FXML
    public void handleEnrollMember(ActionEvent event) {
        // 1. Create Dialog
        Dialog<Guest> dialog = new Dialog<>();
        dialog.setTitle("Enroll New Member");
        dialog.setHeaderText("Enter Guest Details");

        ButtonType enrollBtnType = new ButtonType("Enroll", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(enrollBtnType, ButtonType.CANCEL);

        // 2. Create Fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField firstNameFld = new TextField();
        firstNameFld.setPromptText("First Name");
        TextField lastNameFld = new TextField();
        lastNameFld.setPromptText("Last Name");
        TextField emailFld = new TextField();
        emailFld.setPromptText("Email Address");
        TextField phoneFld = new TextField();
        phoneFld.setPromptText("Phone Number");

        grid.add(new Label("First Name:*"), 0, 0);
        grid.add(firstNameFld, 1, 0);
        grid.add(new Label("Last Name:*"), 0, 1);
        grid.add(lastNameFld, 1, 1);
        grid.add(new Label("Email:*"), 0, 2);
        grid.add(emailFld, 1, 2);
        grid.add(new Label("Phone:*"), 0, 3);
        grid.add(phoneFld, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // 3. Convert Result to Guest Object
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == enrollBtnType) {
                // Basic Validation
                if (firstNameFld.getText().isEmpty() || lastNameFld.getText().isEmpty() ||
                        emailFld.getText().isEmpty() || phoneFld.getText().isEmpty()) {
                    return null; // Will be caught as empty result
                }

                Guest g = new Guest();
                g.setFirstName(firstNameFld.getText().trim());
                g.setLastName(lastNameFld.getText().trim());
                g.setEmail(emailFld.getText().trim());
                g.setPhone(phoneFld.getText().trim());
                // Set defaults for required fields not in form
                g.setCountry("Unknown");
                return g;
            }
            return null;
        });

        Optional<Guest> result = dialog.showAndWait();

        // 4. Process Enrollment
        result.ifPresent(transientGuest -> {
            try {
                // A. Check if already a loyalty member
                if (loyaltyService.findAccountByEmailOrPhone(transientGuest.getEmail(), transientGuest.getPhone()).isPresent()) {
                    showAlert(Alert.AlertType.WARNING, "Duplicate",
                            "A loyalty account already exists for " + transientGuest.getEmail());
                    return;
                }

                // B. Find or Create Guest in DB
                // This prevents creating duplicate Guest records if they visited before but weren't members
                Guest guestToEnroll;
                Optional<Guest> existingGuest = guestRepository.findByEmail(transientGuest.getEmail());

                if (existingGuest.isPresent()) {
                    guestToEnroll = existingGuest.get();
                    LOGGER.info("Enrolling existing guest: " + guestToEnroll.getFullName());
                } else {
                    // Create new guest record
                    guestToEnroll = guestRepository.save(transientGuest);
                }

                // C. Create Loyalty Account
                LoyaltyAccount newAccount = loyaltyService.enrollGuest(guestToEnroll);

                // D. Success
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Enrolled " + guestToEnroll.getFullName() + "\nLoyalty Number: " + newAccount.getLoyaltyNumber());

                logActivity("ENROLL_MEMBER", "LOYALTY", newAccount.getLoyaltyNumber(),
                        "Enrolled new member: " + guestToEnroll.getFullName());

                // E. Refresh Table
                loadLoyaltyData();

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Enrollment failed", e);
                showAlert(Alert.AlertType.ERROR, "Error", "Enrollment failed: " + e.getMessage());
            }
        });
    }

    private void updateMemberDetails(LoyaltyAccount member) {
        if (member == null) {
            clearMemberDetails();
            return;
        }

        Guest guest = member.getGuest();
        setLabelText(selectedMemberNameLabel, guest != null ? guest.getFullName() : "N/A");
        setLabelText(selectedMemberNumberLabel, member.getLoyaltyNumber());
        setLabelText(selectedMemberTierLabel, formatTierDisplayName(member.getTier())); // FIXED: was getTier().getDisplayName()
        setLabelText(selectedMemberPointsLabel, String.format("%,d points", member.getPointsBalance()));
        setLabelText(selectedMemberLifetimeLabel, String.format("%,d points", member.getLifetimePoints()));
        setLabelText(selectedMemberStatusLabel, member.isActive() ? "Active" : "Inactive");
    }

    private void clearMemberDetails() {
        setLabelText(selectedMemberNameLabel, "Select a member");
        setLabelText(selectedMemberNumberLabel, "-");
        setLabelText(selectedMemberTierLabel, "-");
        setLabelText(selectedMemberPointsLabel, "-");
        setLabelText(selectedMemberLifetimeLabel, "-");
        setLabelText(selectedMemberStatusLabel, "-");
    }

    private void loadMemberTransactions(LoyaltyAccount member) {
        memberTransactions.clear();
        if (member == null || loyaltyService == null) return;

        memberTransactions.addAll(loyaltyService.getTransactionHistory(member));

    }

    private void updateActionButtons(LoyaltyAccount selected) {
        boolean hasSelection = selected != null;

        if (addPointsButton != null) addPointsButton.setDisable(!hasSelection);
        if (deductPointsButton != null) deductPointsButton.setDisable(!hasSelection);
        if (viewDetailsButton != null) viewDetailsButton.setDisable(!hasSelection);
    }

    private void loadLoyaltyData() {
        LOGGER.info("DEBUG: Starting loadLoyaltyData...");
        allMembers.clear();

        if (loyaltyService != null) {
            java.util.List<LoyaltyAccount> freshList = loyaltyService.getAllActiveAccounts();

            LOGGER.info("DEBUG: LoyaltyService returned " + freshList.size() + " records.");

            // Print names to verify the new person is in the list
            for (LoyaltyAccount acc : freshList) {
                String name = (acc.getGuest() != null) ? acc.getGuest().getFullName() : "Unknown";
                LOGGER.info("DEBUG: Found member: " + name + " (" + acc.getLoyaltyNumber() + ")");
            }
            allMembers.addAll(freshList);
        }else{
            LOGGER.severe("DEBUG: LoyaltyService is NULL!");
        }

        filteredMembers = new FilteredList<>(allMembers, p -> true);
        membersTable.setItems(filteredMembers);
        membersTable.refresh(); // Force UI refresh

        LOGGER.info("Loaded " + allMembers.size() + " loyalty members");
    }

    private void applyFilters() {
        if (filteredMembers == null) return;

        filteredMembers.setPredicate(member -> {
            // Search filter
            String search = memberSearchField != null ? memberSearchField.getText().toLowerCase() : "";
            if (!search.isEmpty()) {
                Guest guest = member.getGuest();
                boolean matchesSearch =
                        member.getLoyaltyNumber().toLowerCase().contains(search) ||
                                (guest != null && guest.getFullName().toLowerCase().contains(search)) ||
                                (guest != null && guest.getEmail() != null &&
                                        guest.getEmail().toLowerCase().contains(search)) ||
                                (guest != null && guest.getPhone() != null &&
                                        guest.getPhone().contains(search));
                if (!matchesSearch) return false;
            }

            // Tier filter
            if (tierFilter != null && tierFilter.getValue() != null &&
                    !"All Tiers".equals(tierFilter.getValue())) {
                String tierDisplayName = formatTierDisplayName(member.getTier()); // FIXED: was getTier().getDisplayName()
                if (!tierFilter.getValue().equals(tierDisplayName)) return false;
            }

            return true;
        });
    }

    private void updateStatistics() {
        // In production, get from loyaltyService.getLoyaltyStats()
        setLabelText(totalMembersLabel, String.valueOf(allMembers.size()));
        setLabelText(pointsOutstandingLabel, "0");
        setLabelText(pointsRedeemedLabel, "0");
        setLabelText(averagePointsLabel, "0");
    }

    // ==================== Event Handlers ====================
    @FXML
    public void handleFilterChange(ActionEvent event) {
        applyFilters();
    }

    @FXML
    public void handleSearch(KeyEvent event) {
        applyFilters();
    }

    @FXML
    public void handleSearchMember(ActionEvent event) {
        applyFilters();
    }

    @FXML
    public void handleTierFilterChange(ActionEvent event) {
        applyFilters();
    }

    @FXML
    public void handleAddPoints(ActionEvent event) {
        if (selectedMember == null) return;

        TextInputDialog dialog = new TextInputDialog("100");
        dialog.setTitle("Add Points");
        dialog.setHeaderText("Add bonus points to " + selectedMember.getGuest().getFullName());
        dialog.setContentText("Points to add:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(pointsStr -> {
            try {
                int points = Integer.parseInt(pointsStr);
                if (points <= 0) throw new NumberFormatException();

                // FIXED: was selectedMember.addPoints(points) - method doesn't exist
                // In production: loyaltyService.addBonusPoints(selectedMember, points, "Admin adjustment");
                addPointsToAccount(selectedMember, points);

                showAlert(Alert.AlertType.INFORMATION, "Points Added",
                        points + " bonus points added to " + selectedMember.getGuest().getFullName());

                logActivity("ADD_POINTS", "LOYALTY", selectedMember.getLoyaltyNumber(),
                        "Added " + points + " points to " + selectedMember.getGuest().getFullName());

                updateMemberDetails(selectedMember);
                loadMemberTransactions(selectedMember);

            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid positive number.");
            }
        });
    }

    @FXML
    public void handleDeductPoints(ActionEvent event) {
        if (selectedMember == null) return;

        TextInputDialog dialog = new TextInputDialog("100");
        dialog.setTitle("Deduct Points");
        dialog.setHeaderText("Deduct points from " + selectedMember.getGuest().getFullName());
        dialog.setContentText("Points to deduct:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(pointsStr -> {
            try {
                int points = Integer.parseInt(pointsStr);
                if (points <= 0 || points > selectedMember.getPointsBalance()) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Amount",
                            "Cannot deduct more than current balance (" +
                                    selectedMember.getPointsBalance() + " points)");
                    return;
                }

                // FIXED: was selectedMember.deductPoints(points) - method doesn't exist
                // In production: loyaltyService.adjustPoints(selectedMember, -points, "Admin deduction");
                deductPointsFromAccount(selectedMember, points);

                showAlert(Alert.AlertType.INFORMATION, "Points Deducted",
                        points + " points deducted from " + selectedMember.getGuest().getFullName());

                logActivity("DEDUCT_POINTS", "LOYALTY", selectedMember.getLoyaltyNumber(),
                        "Deducted " + points + " points from " + selectedMember.getGuest().getFullName());

                updateMemberDetails(selectedMember);
                loadMemberTransactions(selectedMember);

            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid number.");
            }
        });
    }

    @FXML
    public void handleViewDetails(ActionEvent event) {
        if (selectedMember == null) return;
        showAlert(Alert.AlertType.INFORMATION, "Member Details",
                "Detailed view for " + selectedMember.getLoyaltyNumber());
    }

    @FXML
    public void handleExport(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Export",
                "Loyalty data export would be generated here.");
        logActivity("EXPORT_LOYALTY", "LOYALTY", "N/A", "Exported loyalty data");
    }

    @FXML
    public void handleBack(ActionEvent event) {
        navigationService.navigateTo("/com/hanson/hotelreservationsystem/admin/adminDashboard.fxml");
    }

    private void setLabelText(Label label, String text) {
        if (label != null) label.setText(text != null ? text : "N/A");
    }

    private void logActivity(String action, String entityType, String entityId, String message) {
        if (activityLogger != null) {
            activityLogger.logActivity(adminSession.getActorName(), action, entityType, entityId, message);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Setters for DI
    public void setNavigationService(NavigationService navigationService) { this.navigationService = navigationService; }
    public void setLoyaltyService(LoyaltyService loyaltyService) { this.loyaltyService = loyaltyService; }
    public void setAdminSession(AdminSession adminSession) { this.adminSession = adminSession; }
    public void setActivityLogger(ActivityLogger activityLogger) { this.activityLogger = activityLogger; }
}