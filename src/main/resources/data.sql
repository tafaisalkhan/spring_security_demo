INSERT INTO user_roles (subject, role_name) VALUES ('alice', 'USER');
INSERT INTO user_roles (subject, role_name) VALUES ('bob', 'USER');
INSERT INTO user_roles (subject, role_name) VALUES ('bob', 'ADMIN');
INSERT INTO user_roles (subject, role_name) VALUES ('ops', 'MONITOR');

INSERT INTO endpoint_permissions (http_method, path, access_type, role_name) VALUES ('GET', '/demo/public', 'PUBLIC', '');
INSERT INTO endpoint_permissions (http_method, path, access_type, role_name) VALUES ('GET', '/demo/controller', 'ROLE', 'USER');
INSERT INTO endpoint_permissions (http_method, path, access_type, role_name) VALUES ('GET', '/demo/service', 'ROLE', 'USER');
INSERT INTO endpoint_permissions (http_method, path, access_type, role_name) VALUES ('GET', '/demo/method', 'ROLE', 'USER');
INSERT INTO endpoint_permissions (http_method, path, access_type, role_name) VALUES ('GET', '/demo/plain', 'ROLE', 'USER');
INSERT INTO endpoint_permissions (http_method, path, access_type, role_name) VALUES ('GET', '/demo/all', 'ROLE', 'ADMIN');
INSERT INTO endpoint_permissions (http_method, path, access_type, role_name) VALUES ('GET', '/admin/status', 'ROLE', 'ADMIN');
INSERT INTO endpoint_permissions (http_method, path, access_type, role_name) VALUES ('GET', '/mobile/status', 'ROLE', 'USER');
INSERT INTO endpoint_permissions (http_method, path, access_type, role_name) VALUES ('GET', '/actuator/health', 'ROLE', 'MONITOR');
INSERT INTO endpoint_permissions (http_method, path, access_type, role_name) VALUES ('GET', '/actuator/info', 'ROLE', 'MONITOR');
