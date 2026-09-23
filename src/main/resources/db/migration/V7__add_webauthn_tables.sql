-- Tabele wymagane przez wbudowane repozytoria JDBC Spring Security WebAuthn
-- (JdbcPublicKeyCredentialUserEntityRepository / JdbcUserCredentialRepository,
-- pakiet org.springframework.security.web.webauthn.management, wersja 7.1.1).
--
-- Nazwy tabel/kolumn i typy zostaly ustalone przez odczytanie zapytan SQL wbudowanych
-- w bytecode tych klas (biblioteka nie publikuje gotowego pliku ze schematem) -
-- nie sa dowolne, musza pasowac dokladnie 1:1.
--
-- "user_entities" nie ma zadnego zwiazku FK z "users" - dopasowanie do konta z tej
-- aplikacji odbywa sie w warstwie aplikacyjnej, po polu "name" (= users.username).

CREATE TABLE user_entities (
    id           VARCHAR(255) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_entities_name (name)
);

CREATE TABLE user_credentials (
    credential_id                 VARCHAR(255) NOT NULL,
    user_entity_user_id           VARCHAR(255) NOT NULL,
    public_key                    BLOB         NOT NULL,
    signature_count               BIGINT       NOT NULL DEFAULT 0,
    uv_initialized                TINYINT(1)   NOT NULL DEFAULT 0,
    backup_eligible                TINYINT(1)  NOT NULL DEFAULT 0,
    backup_state                  TINYINT(1)   NOT NULL DEFAULT 0,
    authenticator_transports      VARCHAR(255) NULL,
    public_key_credential_type    VARCHAR(50)  NOT NULL,
    attestation_object            BLOB         NULL,
    attestation_client_data_json  BLOB         NULL,
    created                       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used                     TIMESTAMP    NULL DEFAULT NULL,
    label                         VARCHAR(255) NULL,
    PRIMARY KEY (credential_id),
    KEY idx_user_credentials_user_entity_user_id (user_entity_user_id),
    CONSTRAINT fk_user_credentials_user_entity
        FOREIGN KEY (user_entity_user_id) REFERENCES user_entities (id)
        ON DELETE CASCADE
);
