package com.hanson.hotelreservationsystem.util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * Singleton ActivityLogger for the Hotel Reservation System.
 *
 * Responsibilities:
 * - Centralized logging for Kiosk and Admin actions.
 * - File rotation management (1MB limit, 10 files).
 * - Structured formatting (CSV style) for easy reporting.
 */
public class ActivityLogger {

    private static ActivityLogger instance;
    private final Logger logger;
    private FileHandler fileHandler;

    // Log configuration constants based on PDF requirements [cite: 172, 180, 181]
    private static final String LOG_FILE_PATTERN = "system_logs.%g.log";
    private static final int LOG_FILE_LIMIT = 1024 * 1024; // 1MB
    private static final int LOG_FILE_COUNT = 10; // Keep up to 10 files

    /**
     * Private constructor for Singleton pattern.
     */
    private ActivityLogger() {
        // Get a logger instance associated with the package
        logger = Logger.getLogger("com.hanson.hotelreservationsystem");

        // Disable default console handler to prevent clutter if desired,
        // or keep it for debugging. Here we keep it but ensure parent handlers are used.
        logger.setLevel(Level.ALL);
    }

    /**
     * Retrieves the singleton instance of the ActivityLogger.
     * @return The ActivityLogger instance.
     */
    public static synchronized ActivityLogger getInstance() {
        if (instance == null) {
            instance = new ActivityLogger();
        }
        return instance;
    }

    /**
     * Initializes the file handler and formatter.
     * This must be called at application startup.
     */
    public void initialize() {
        try {
            // Configure FileHandler with rotation: 1MB limit, 10 files, append mode [cite: 170-172]
            fileHandler = new FileHandler(LOG_FILE_PATTERN, LOG_FILE_LIMIT, LOG_FILE_COUNT, true);

            // Set a custom formatter to match the required reporting columns [cite: 163]
            fileHandler.setFormatter(new SimpleFormatter() {
                private static final String FORMAT = "%1$tF %1$tT,%2$s%n"; // Date Time, Message

                @Override
                public synchronized String format(LogRecord lr) {
                    // We only want the raw message because logActivity() pre-formats the CSV string
                    // However, we include the system timestamp from the record for precision
                    return String.format(FORMAT,
                            new Date(lr.getMillis()),
                            lr.getMessage()
                    );
                }
            });

            // Add handler to the logger
            logger.addHandler(fileHandler);
            logger.info("LOGGING_STARTED,System,Startup,App,0,Logger initialized successfully");

        } catch (IOException e) {
            // Log to standard error if file logging fails [cite: 177]
            System.err.println("CRITICAL: Failed to initialize logger handler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Logs a specific business activity.
     * Formats the data to satisfy the requirement: Timestamp, Actor, Action, Entity Type, Entity ID, Message[cite: 163].
     *
     * @param actor      Who performed the action (e.g., "Guest", "Admin: John")
     * @param action     The action taken (e.g., "LOGIN", "BOOKING_CREATED")
     * @param entityType The type of object affected (e.g., "RESERVATION", "ROOM")
     * @param entityId   The ID of the object (e.g., "RES-101", "N/A")
     * @param message    A descriptive message
     */
    public void logActivity(String actor, String action, String entityType, String entityId, String message) {
        // Sanitize inputs to remove commas to prevent breaking CSV structure
        String safeMessage = message.replace(",", ";");
        String safeActor = actor.replace(",", " ");

        // Format: Actor,Action,EntityType,EntityID,Message
        // (Timestamp is added automatically by the Formatter in initialize)
        String logPayload = String.format("%s,%s,%s,%s,%s",
                safeActor,
                action,
                entityType,
                entityId,
                safeMessage
        );

        logger.info(logPayload);
    }

    /**
     * Logs exceptions and errors[cite: 186].
     * * @param message Contextual message
     * @param e The exception thrown
     */
    public void logError(String message, Throwable e) {
        // Severe issues must include stack traces [cite: 187]
        logger.log(Level.SEVERE, message, e);
    }
}