-- Umozliwia powiazanie zakupu (finance_purchase) z kredytem (finance_loan), gdy jeden lub kilka
-- zakupow zostaje zamienionych na kredyt (np. PayPo: 1 zakup = 1 kredyt, Allegro: N zakupow = 1 kredyt).
-- Zakup zostaje w bazie (historia "co kupiono"), ale przestaje byc liczony jako osobne obciazenie
-- karty - patrz PaymentStatus.CONVERTED.
ALTER TABLE finance_purchase ADD COLUMN id_loan INT NULL;

ALTER TABLE finance_purchase
    ADD CONSTRAINT fk_purchase_loan FOREIGN KEY (id_loan) REFERENCES finance_loan (id);
