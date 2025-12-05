module com.hanson.hotelreservationsystem {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    // JPA/Hibernate modules
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.sql;
    requires java.naming;

    // Validation
    requires jakarta.validation;
    requires org.hibernate.validator;

    // Logging
    requires java.logging;

    // Password hashing
    requires jbcrypt;

    // ==================== OPENS for Hibernate reflection ====================
    // Hibernate needs deep reflection access to entity classes
    opens com.hanson.hotelreservationsystem.model to org.hibernate.orm.core, javafx.base;
    opens com.hanson.hotelreservationsystem.model.enums to org.hibernate.orm.core, javafx.base;
    opens com.hanson.hotelreservationsystem.config to org.hibernate.orm.core;

    // ==================== OPENS for JavaFX FXML reflection ====================
    opens com.hanson.hotelreservationsystem to javafx.fxml;
    opens com.hanson.hotelreservationsystem.controller to javafx.fxml;
    opens com.hanson.hotelreservationsystem.controller.Admin to javafx.fxml;
    opens com.hanson.hotelreservationsystem.controller.Kiosk to javafx.fxml;

    // ==================== EXPORTS ====================
    exports com.hanson.hotelreservationsystem;
    exports com.hanson.hotelreservationsystem.model;
    exports com.hanson.hotelreservationsystem.model.enums;
    exports com.hanson.hotelreservationsystem.service;
    exports com.hanson.hotelreservationsystem.repository;
    exports com.hanson.hotelreservationsystem.controller;
    exports com.hanson.hotelreservationsystem.controller.Admin;
    exports com.hanson.hotelreservationsystem.controller.Kiosk;
    exports com.hanson.hotelreservationsystem.config;
    exports com.hanson.hotelreservationsystem.util;
    exports com.hanson.hotelreservationsystem.session;
}