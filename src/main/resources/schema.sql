CREATE TABLE user_roles (
    subject VARCHAR(100) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (subject, role_name)
);

CREATE TABLE endpoint_permissions (
    http_method VARCHAR(10) NOT NULL,
    path VARCHAR(200) NOT NULL,
    access_type VARCHAR(10) NOT NULL,
    role_name VARCHAR(50),
    PRIMARY KEY (http_method, path, access_type, role_name)
);
