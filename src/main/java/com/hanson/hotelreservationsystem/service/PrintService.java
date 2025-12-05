package com.hanson.hotelreservationsystem.service;

import com.hanson.hotelreservationsystem.session.BookingSession;
import com.hanson.hotelreservationsystem.session.BookingSession.RoomSelection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service responsible for generating and "printing" (exporting) booking receipts.
 * * Adheres to requirements:
 * - Exports documents to TXT format.
 * - [cite_start]Handles the logic for receipt generation separate from the UI (Business Tier)[cite: 28, 29].
 * - [cite_start]Logs activities and errors[cite: 161, 185].
 * - Saves the receipt directly to the application's root folder.
 */
public class PrintService {

    private static final Logger LOGGER = Logger.getLogger(PrintService.class.getName());
    // Updated path to save to the root directory
    private static final String RECEIPT_PATH_FORMAT = "Receipt_%s.txt";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generates a formatted receipt for the given booking session and saves it to a file.
     * * @param session The current completed booking session.
     * @throws RuntimeException if printing fails.
     */
    public void printBookingConfirmation(BookingSession session) {
        if (session == null) {
            LOGGER.log(Level.WARNING, "Attempted to print receipt for null session.");
            return;
        }

        LOGGER.info("Generating receipt for confirmation: " + session.getConfirmationNumber());

        String receiptContent = buildReceiptContent(session);
        saveReceiptToFile(receiptContent, session.getConfirmationNumber());
    }

    /**
     * Constructs the text content of the receipt.
     */
    private String buildReceiptContent(BookingSession session) {
        StringBuilder sb = new StringBuilder();
        String line = "----------------------------------------\n";

        // --- Header ---
        sb.append(line);
        sb.append(String.format("%30s\n", "HOTEL HANSON"));
        sb.append(String.format("%30s\n", "Kiosk Reservation"));
        sb.append(line);
        sb.append("Date: ").append(LocalDateTime.now().format(TIME_FMT)).append("\n");
        sb.append("Conf #: ").append(session.getConfirmationNumber()).append("\n");
        sb.append(line);

        // --- Guest Details ---
        sb.append("Guest: ").append(session.getFullName()).append("\n");
        sb.append("Check-in:  ").append(session.getCheckInDate().format(DATE_FMT)).append("\n");
        sb.append("Check-out: ").append(session.getCheckOutDate().format(DATE_FMT)).append("\n");
        sb.append("Nights:    ").append(session.getNights()).append("\n");
        sb.append("Guests:    ").append(session.getAdultCount()).append(" Adult(s)");

        if (session.getChildCount() > 0) {
            sb.append(", ").append(session.getChildCount()).append(" Child(ren)");
        }
        sb.append("\n");
        sb.append(line);

        // --- Room Details ---
        sb.append("ROOM SELECTION:\n");
        for (RoomSelection room : session.getSelectedRooms()) {
            if (room.getQuantity() > 0) {
                // Format: 2x Double Room
                sb.append(String.format("%dx %-25s\n",
                        room.getQuantity(),
                        room.getRoomType().getDisplayName()));
            }
        }

        sb.append(line);

        // --- Financials ---

        sb.append(String.format("Total Estimate:        $%.2f\n", session.getTotalAmount()));

        sb.append(line);

        // --- Disclaimer / Footer ---
        sb.append("\n");
        sb.append("        PAYMENT REQUIRED AT DESK\n");
        sb.append("    Please present this receipt upon\n");
        sb.append("      arrival at the front desk.\n");
        sb.append("\n");
        sb.append("       Thank you for choosing us!\n");
        sb.append(line);

        return sb.toString();
    }

    /**
     * Saves the generated string to a text file in the root directory to simulate printing.
     * * @param content The receipt text.
     * @param confirmationNumber Used for the filename.
     */
    private void saveReceiptToFile(String content, String confirmationNumber) {
        String filename = String.format(RECEIPT_PATH_FORMAT, confirmationNumber);

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
            LOGGER.info("Receipt successfully printed to root folder: " + filename);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write receipt file to root folder: " + filename, e);

            // Re-throw to let the controller handle the printing failure alert
            throw new RuntimeException("Printing failed: Could not write file to root directory.", e);
        }
    }
}