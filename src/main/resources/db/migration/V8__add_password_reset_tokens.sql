-- Tokeny resetu hasla ("zapomnialem hasla"). Przechowujemy tylko SHA-256 hash tokena
-- (nie plaintext) - w razie wycieku bazy sam wpis w tej tabeli nie pozwala nikomu
-- zresetowac hasla, bez znajomosci oryginalnego tokena z linku w mailu.
CREATE TABLE password_reset_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- INT (nie BIGINT): musi dokladnie pasowac do users.id (int(11)), inaczej MySQL odrzuca FK
    user_id     INT NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  DATETIME NOT NULL,
    used_at     DATETIME NULL,
    created_by  VARCHAR(100) NULL,
    modified_by VARCHAR(100) NULL,
    created_at  DATETIME NOT NULL,
    modified_at DATETIME NULL,
    UNIQUE KEY uq_password_reset_tokens_token_hash (token_hash),
    KEY idx_password_reset_tokens_user_id (user_id),
    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
);
