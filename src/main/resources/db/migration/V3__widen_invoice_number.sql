-- Faktury importowane z KSeF (wystawione przez portal, poza aplikacja) maja numery w dowolnym
-- formacie (np. "FV/2026/05/123"), a nie tylko "rok/nr" - varchar(10) jest za waski.
ALTER TABLE goahead_invoice MODIFY number VARCHAR(50) NOT NULL;
