DROP TABLE IF EXISTS sensor_string_history CASCADE;
DROP TABLE IF EXISTS sensor_numeric_history CASCADE;
DROP TABLE IF EXISTS widgets CASCADE;
DROP TABLE IF EXISTS sensor CASCADE;
DROP TABLE IF EXISTS equipment CASCADE;
DROP TABLE IF EXISTS dashboard CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dashboard (
    dashboard_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dashboard_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE equipment (
    equipment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    equipment_name VARCHAR(255) NOT NULL,
    field VARCHAR(255),
    dashboard_id BIGINT NOT NULL,
    FOREIGN KEY (dashboard_id) REFERENCES dashboard(dashboard_id) ON DELETE CASCADE
);

CREATE TABLE sensor (
    sensor_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sensor_name VARCHAR(255) NOT NULL,
    equipment_id BIGINT NOT NULL,
    FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id) ON DELETE CASCADE
);

CREATE TABLE widgets (
    widget_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dashboard_id BIGINT NOT NULL,
    equipment_id BIGINT,
    sensor_id BIGINT,
    widget_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    chart_type VARCHAR(50),
    data_type VARCHAR(50),
    unit VARCHAR(50),
    pos_x INT NOT NULL DEFAULT 0,
    pos_y INT NOT NULL DEFAULT 0,
    width INT NOT NULL DEFAULT 2,
    height INT NOT NULL DEFAULT 2,
    config_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (dashboard_id) REFERENCES dashboard(dashboard_id) ON DELETE CASCADE,
    FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id) ON DELETE SET NULL,
    FOREIGN KEY (sensor_id) REFERENCES sensor(sensor_id) ON DELETE SET NULL
);

CREATE TABLE sensor_numeric_history (
    sensor_numeric_history_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    numeric_value DOUBLE NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    unit VARCHAR(255),
    sensor_id BIGINT NOT NULL,
    FOREIGN KEY (sensor_id) REFERENCES sensor(sensor_id) ON DELETE CASCADE
);

CREATE TABLE sensor_string_history (
    sensor_string_history_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    status VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    sensor_id BIGINT NOT NULL,
    FOREIGN KEY (sensor_id) REFERENCES sensor(sensor_id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
    token_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_dashboard_user_id ON dashboard(user_id);
CREATE INDEX idx_equipment_dashboard_id ON equipment(dashboard_id);
CREATE INDEX idx_sensor_equipment_id ON sensor(equipment_id);
CREATE INDEX idx_widgets_dashboard_id ON widgets(dashboard_id);
CREATE INDEX idx_widgets_equipment_id ON widgets(equipment_id);
CREATE INDEX idx_widgets_sensor_id ON widgets(sensor_id);
