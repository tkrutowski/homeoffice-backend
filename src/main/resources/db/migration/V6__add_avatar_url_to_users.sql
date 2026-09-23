-- URL zdjecia profilowego z konta Google (pole "picture" z ID tokena).
-- Wypelniane/odswiezane wylacznie przy logowaniu przez Google; NULL dla userow, ktorzy
-- nigdy nie zalogowali sie przez Google (front pokazuje wtedy avatar z inicjalow jak dzis).
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(512) NULL;
