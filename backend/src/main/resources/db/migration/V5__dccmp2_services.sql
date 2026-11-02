-- DCCMP 2.0: additive schema migration. Existing clearance history is preserved.
CREATE TABLE student_directory (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, roll_number VARCHAR(255) NOT NULL UNIQUE,
 student_name VARCHAR(255) NOT NULL, branch VARCHAR(255) NOT NULL, email VARCHAR(255) NOT NULL UNIQUE,
 batch_year VARCHAR(255), updated_at DATETIME(6), updated_by BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE directory_imports (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, admin_id BIGINT, file_name VARCHAR(255),total_rows INT,
 created_rows INT,updated_rows INT,unchanged_rows INT,invalid_rows INT,created_at DATETIME(6),error_csv LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE directory_changes (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,directory_id BIGINT,roll_number VARCHAR(255),field_name VARCHAR(255),
 old_value VARCHAR(255),new_value VARCHAR(255),admin_id BIGINT,import_id BIGINT,changed_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_directory_changes_roll ON directory_changes(roll_number);
CREATE TABLE certificate_requests (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,student_id BIGINT NOT NULL,roll_number VARCHAR(255) NOT NULL,
 student_name VARCHAR(255) NOT NULL,certificate_type VARCHAR(255) NOT NULL,branch VARCHAR(255) NOT NULL,
 hod_department_id BIGINT NOT NULL,purpose TEXT,remarks TEXT,status VARCHAR(255) NOT NULL,
 hod_staff_id BIGINT,hod_remarks VARCHAR(255),hod_decided_at DATETIME(6),
 principal_staff_id BIGINT,principal_remarks VARCHAR(255),principal_decided_at DATETIME(6),
 expires_at DATETIME(6),cancelled_by BIGINT,cancellation_reason VARCHAR(255),cancelled_at DATETIME(6),
 collected_by BIGINT,collected_at DATETIME(6),submitted_at DATETIME(6),version BIGINT DEFAULT 0,
 INDEX idx_cert_student_type(student_id,certificate_type,status), INDEX idx_cert_hod(hod_department_id,status), INDEX idx_cert_expiry(status,expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE certificate_request_events (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,request_id BIGINT,action VARCHAR(255),actor_role VARCHAR(255),actor_id BIGINT,
 remarks TEXT,occurred_at DATETIME(6),INDEX idx_cert_events(request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE certificate_notifications (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,request_id BIGINT,kind VARCHAR(255),recipient VARCHAR(255),
 state VARCHAR(255),attempts INT,last_error VARCHAR(255),attempted_at DATETIME(6),sent_at DATETIME(6),
 UNIQUE KEY uq_cert_notification_kind(request_id,kind)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE original_documents (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,application_id BIGINT,document_name VARCHAR(255),
 expected_quantity INT,collected_quantity INT,active BOOLEAN,created_at DATETIME(6),INDEX idx_orig_doc(application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE original_handover_visits (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,application_id BIGINT,admin_id BIGINT,identity_verified BOOLEAN,
 remarks TEXT,handed_over_at DATETIME(6),INDEX idx_orig_visits(application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE original_handover_items (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,visit_id BIGINT,document_id BIGINT,quantity INT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE original_document_changes (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,application_id BIGINT,document_id BIGINT,admin_id BIGINT,
 action VARCHAR(255),details TEXT,occurred_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE certificate_types (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,code VARCHAR(255) NOT NULL UNIQUE,
 name VARCHAR(255) NOT NULL,active BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO certificate_types(code,name,active) VALUES
 ('STUDY','Study Certificate',true),
 ('BONAFIDE','Bonafide Certificate',true),
 ('LOR','Letter of Recommendation (LOR)',true),
 ('DUPLICATE_TC','Duplicate Transfer Certificate',true),
 ('CONDUCT','Conduct Certificate',true);
