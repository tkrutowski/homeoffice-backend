package net.focik.homeoffice.finance.domain.purchase.port.primary;

import java.time.LocalDate;

public interface CalculatePaymentDeadlineUseCase {

    /**
     * Wylicza termin płatności zakupu na podstawie typu karty i daty zakupu
     * (bez zapisywania zakupu - do podglądu np. w formularzu).
     */
    LocalDate calculatePaymentDeadline(int idCard, LocalDate purchaseDate);
}
