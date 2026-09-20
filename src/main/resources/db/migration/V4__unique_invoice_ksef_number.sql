-- Zabezpieczenie przed zdublowaniem faktury importowanej z KSeF (np. dwa rownolegle importy
-- z tego samego zakresu). MySQL dopuszcza wiele wartosci NULL w indeksie unikalnym, wiec
-- faktury jeszcze niewyslane do KSeF (ksef_number IS NULL) nie koliduja.
ALTER TABLE goahead_invoice ADD CONSTRAINT uq_goahead_invoice_ksef_number UNIQUE (ksef_number);
