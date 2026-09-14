-- Umozliwia przypisanie propozycji kredytu/zakupu (finance_loan_proposal) do konkretnego
-- domownika, ktorego dotyczy - ustalane automatycznie przy ingest przez dopasowanie adresu
-- nadawcy przekierowania (sourceEmailFrom) do adresu e-mail zapisanego przy koncie uzytkownika
-- (users.email). NULL = dopasowanie sie nie powiodlo, propozycja czeka na reczna weryfikacje
-- przez uprzywilejowanego uzytkownika.
ALTER TABLE finance_loan_proposal ADD COLUMN id_user INT NULL;
