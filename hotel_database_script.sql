
    create table activity_logs (
        success bit,
        created_at datetime(6) not null,
        entity_id bigint,
        id bigint not null auto_increment,
        timestamp datetime(6) not null,
        updated_at datetime(6),
        ip_address varchar(45),
        action varchar(50) not null,
        actor varchar(50) not null,
        entity_type varchar(50),
        error_message varchar(200),
        message varchar(500),
        new_value varchar(500),
        old_value varchar(500),
        primary key (id)
    ) engine=InnoDB;

    create table admins (
        failed_login_attempts integer,
        is_active bit,
        subscribe_to_notifications bit,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        last_login datetime(6),
        locked_until datetime(6),
        updated_at datetime(6),
        username varchar(30) not null,
        first_name varchar(50) not null,
        last_name varchar(50) not null,
        email varchar(100) not null,
        password_hash varchar(100) not null,
        role enum ('ADMIN','MANAGER') not null,
        primary key (id)
    ) engine=InnoDB;

    create table feedback_sentiment_tags (
        feedback_id bigint not null,
        sentiment_tag enum ('POSITIVE','NEUTRAL','NEGATIVE')
    ) engine=InnoDB;

    create table feedbacks (
        cleanliness_rating integer,
        comfort_rating integer,
        location_rating integer,
        rating integer not null,
        service_rating integer,
        submitted_via_kiosk bit,
        value_rating integer,
        would_recommend bit,
        created_at datetime(6) not null,
        guest_id bigint not null,
        id bigint not null auto_increment,
        reservation_id bigint not null,
        submitted_at datetime(6) not null,
        updated_at datetime(6),
        comments varchar(1000),
        primary key (id)
    ) engine=InnoDB;

    create table guests (
        is_loyalty_member bit,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        phone varchar(20) not null,
        postal_code varchar(20),
        city varchar(50),
        country varchar(50),
        first_name varchar(50) not null,
        id_number varchar(50),
        id_type varchar(50),
        last_name varchar(50) not null,
        state_province varchar(50),
        email varchar(100) not null,
        address varchar(200),
        primary key (id)
    ) engine=InnoDB;

    create table loyalty_accounts (
        enrollment_date date not null,
        is_active bit,
        last_activity_date date,
        lifetime_points integer not null,
        points_balance integer not null,
        created_at datetime(6) not null,
        guest_id bigint not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        loyalty_number varchar(20) not null,
        tier varchar(20),
        primary key (id)
    ) engine=InnoDB;

    create table loyalty_transactions (
        balance_after integer not null,
        points integer not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        loyalty_account_id bigint not null,
        reservation_id bigint,
        transaction_date datetime(6) not null,
        updated_at datetime(6),
        processed_by varchar(50),
        description varchar(200),
        transaction_type enum ('EARN','REDEEM','BONUS','ADJUSTMENT','EXPIRE','REFUND') not null,
        primary key (id)
    ) engine=InnoDB;

    create table payments (
        amount decimal(10,2) not null,
        card_last_four varchar(4),
        refund_amount decimal(10,2),
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        payment_date datetime(6) not null,
        refund_date datetime(6),
        reservation_id bigint not null,
        updated_at datetime(6),
        card_type varchar(20),
        payment_method varchar(30) not null,
        processed_by varchar(50),
        transaction_id varchar(100),
        notes varchar(200),
        refund_reason varchar(200),
        status enum ('PENDING','COMPLETED','FAILED','CANCELLED','REFUNDED','PARTIALLY_REFUNDED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table reservation_addons (
        date_added date,
        quantity integer not null,
        total_price decimal(10,2) not null,
        unit_price decimal(10,2) not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        reservation_id bigint not null,
        updated_at datetime(6),
        addon_type enum ('WIFI','BREAKFAST','PARKING','SPA') not null,
        primary key (id)
    ) engine=InnoDB;

    create table reservation_rooms (
        num_guests integer not null,
        price_multiplier decimal(5,2),
        room_price decimal(10,2) not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        reservation_id bigint not null,
        room_id bigint not null,
        updated_at datetime(6),
        room_type enum ('SINGLE','DOUBLE','DELUXE','PENTHOUSE'),
        primary key (id)
    ) engine=InnoDB;

    create table reservations (
        addons_total decimal(10,2),
        amount_paid decimal(10,2),
        booked_via_kiosk bit,
        check_in_date date not null,
        check_out_date date not null,
        discount_amount decimal(10,2),
        discount_percentage decimal(5,2),
        loyalty_discount decimal(10,2),
        loyalty_points_used integer,
        num_adults integer not null,
        num_children integer not null,
        subtotal decimal(10,2),
        tax_amount decimal(10,2),
        total_amount decimal(10,2),
        actual_check_in datetime(6),
        actual_check_out datetime(6),
        created_at datetime(6) not null,
        guest_id bigint not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        confirmation_number varchar(20) not null,
        discount_applied_by varchar(50),
        special_requests varchar(500),
        status enum ('PENDING','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED','NO_SHOW') not null,
        primary key (id)
    ) engine=InnoDB;

    create table rooms (
        base_price_override float(53),
        floor integer not null,
        has_view bit,
        is_accessible bit,
        is_smoking bit,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        room_number varchar(10) not null,
        description varchar(500),
        room_type enum ('SINGLE','DOUBLE','DELUXE','PENTHOUSE') not null,
        status enum ('AVAILABLE','OCCUPIED','RESERVED','MAINTENANCE','CLEANING') not null,
        primary key (id)
    ) engine=InnoDB;

    create table waitlist_entries (
        desired_check_in date not null,
        desired_check_out date not null,
        num_guests integer not null,
        priority integer,
        added_at datetime(6) not null,
        converted_at datetime(6),
        created_at datetime(6) not null,
        guest_id bigint not null,
        id bigint not null auto_increment,
        notified_at datetime(6),
        reservation_id bigint,
        updated_at datetime(6),
        contact_phone varchar(20),
        status varchar(20) not null,
        added_by varchar(50),
        contact_email varchar(100),
        notes varchar(200),
        desired_room_type enum ('SINGLE','DOUBLE','DELUXE','PENTHOUSE') not null,
        primary key (id)
    ) engine=InnoDB;

    create index idx_actlog_timestamp 
       on activity_logs (timestamp);

    create index idx_actlog_actor 
       on activity_logs (actor);

    create index idx_actlog_action 
       on activity_logs (action);

    create index idx_actlog_entity 
       on activity_logs (entity_type, entity_id);

    create index idx_admin_username 
       on admins (username);

    create index idx_admin_email 
       on admins (email);

    alter table admins 
       add constraint UK_mi8vkhus4xbdbqcac2jm4spvd unique (username);

    alter table admins 
       add constraint UK_47bvqemyk6vlm0w7crc3opdd4 unique (email);

    create index idx_feedback_reservation 
       on feedbacks (reservation_id);

    create index idx_feedback_guest 
       on feedbacks (guest_id);

    create index idx_feedback_rating 
       on feedbacks (rating);

    create index idx_feedback_date 
       on feedbacks (submitted_at);

    alter table feedbacks 
       add constraint UK_luah6pwl1a8gkb66h1o28jcit unique (reservation_id);

    create index idx_guest_email 
       on guests (email);

    create index idx_guest_phone 
       on guests (phone);

    alter table guests 
       add constraint UK_dadfu7mrhcouq9pffy50363r0 unique (email);

    create index idx_loyalty_number 
       on loyalty_accounts (loyalty_number);

    create index idx_loyalty_guest 
       on loyalty_accounts (guest_id);

    alter table loyalty_accounts 
       add constraint UK_m9f6nstxrmnq1s8vcv61g8n84 unique (guest_id);

    alter table loyalty_accounts 
       add constraint UK_tiy0d8axlfe6hh4uff0jl3akq unique (loyalty_number);

    create index idx_loytrans_account 
       on loyalty_transactions (loyalty_account_id);

    create index idx_loytrans_date 
       on loyalty_transactions (transaction_date);

    create index idx_loytrans_type 
       on loyalty_transactions (transaction_type);

    create index idx_payment_reservation 
       on payments (reservation_id);

    create index idx_payment_date 
       on payments (payment_date);

    create index idx_payment_status 
       on payments (status);

    create index idx_resroom_reservation 
       on reservation_rooms (reservation_id);

    create index idx_resroom_room 
       on reservation_rooms (room_id);

    create index idx_reservation_status 
       on reservations (status);

    create index idx_reservation_checkin 
       on reservations (check_in_date);

    create index idx_reservation_checkout 
       on reservations (check_out_date);

    create index idx_reservation_confirmation 
       on reservations (confirmation_number);

    alter table reservations 
       add constraint UK_jhyesu3iq9wow7ul4oig3vt5b unique (confirmation_number);

    create index idx_room_number 
       on rooms (room_number);

    create index idx_room_type 
       on rooms (room_type);

    create index idx_room_status 
       on rooms (status);

    alter table rooms 
       add constraint UK_7ljglxlj90ln3lbas4kl983m2 unique (room_number);

    create index idx_waitlist_guest 
       on waitlist_entries (guest_id);

    create index idx_waitlist_room_type 
       on waitlist_entries (desired_room_type);

    create index idx_waitlist_dates 
       on waitlist_entries (desired_check_in, desired_check_out);

    create index idx_waitlist_status 
       on waitlist_entries (status);

    alter table feedback_sentiment_tags 
       add constraint FKlovu7rhvopp312kcoy424us6b 
       foreign key (feedback_id) 
       references feedbacks (id);

    alter table feedbacks 
       add constraint FKrws2b3lmektqnrb0en35d810d 
       foreign key (guest_id) 
       references guests (id);

    alter table feedbacks 
       add constraint FKqi4d8kwncrdb0rbr0da9nfux7 
       foreign key (reservation_id) 
       references reservations (id);

    alter table loyalty_accounts 
       add constraint FKiun7qsotaxf8069q2at5bje8l 
       foreign key (guest_id) 
       references guests (id);

    alter table loyalty_transactions 
       add constraint FK429dwg0ieqyqbtnkfb7v9ues3 
       foreign key (loyalty_account_id) 
       references loyalty_accounts (id);

    alter table payments 
       add constraint FKp8yh4sjt3u0g6aru1oxfh3o14 
       foreign key (reservation_id) 
       references reservations (id);

    alter table reservation_addons 
       add constraint FKt3uycxqv808ujdab43y270diu 
       foreign key (reservation_id) 
       references reservations (id);

    alter table reservation_rooms 
       add constraint FK96vqam6q3l5mic9bf9unp1q9j 
       foreign key (reservation_id) 
       references reservations (id);

    alter table reservation_rooms 
       add constraint FK81imhuuxynjj2ivy82koy38nm 
       foreign key (room_id) 
       references rooms (id);

    alter table reservations 
       add constraint FKa0e6sbbuw25f8cwciqaune89n 
       foreign key (guest_id) 
       references guests (id);

    alter table waitlist_entries 
       add constraint FKjjmu7uwrrjjn05idjigx8p8em 
       foreign key (guest_id) 
       references guests (id);
