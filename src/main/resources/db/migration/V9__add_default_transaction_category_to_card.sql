-- Domyslna kategoria transakcji bankowej tworzonej przy oplaceniu zakupu ta karta.
-- NULL = transakcja powstaje bez kategorii (wczesniej kategoria byla zaszyta w kodzie po nazwie karty).
ALTER TABLE finance_card ADD COLUMN default_transaction_category_id INT NULL;

-- Przeniesienie dotychczasowego mapowania zaszytego w PurchaseFacade.
UPDATE finance_card SET default_transaction_category_id = 8 WHERE LOWER(card_name) = 'alfa';
UPDATE finance_card SET default_transaction_category_id = 9 WHERE LOWER(card_name) = 'impresja';
