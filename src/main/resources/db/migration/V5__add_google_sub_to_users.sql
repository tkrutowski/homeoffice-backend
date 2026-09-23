-- Kolumna na powiazanie istniejacego konta z Google (sub z ID tokena).
-- Logowanie przez Google NIE zaklada nowych kont - tylko wypelnia to pole przy pierwszym
-- udanym logowaniu na juz istniejace, rejestrowane recznie konto (dopasowanie po email).
-- W MySQL wartosci NULL nie sa traktowane jako duplikaty przez UNIQUE, wiec konta bez
-- powiazania z Google moga wspolistniec bez konfliktu.
ALTER TABLE users ADD COLUMN google_sub VARCHAR(255) NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_google_sub UNIQUE (google_sub);
