package net.focik.homeoffice.goahead.domain.cost.port.primary;

import net.focik.homeoffice.goahead.domain.cost.Cost;
import net.focik.homeoffice.utils.share.PaymentStatus;

public interface UpdateCostUseCase {
    Cost updateCost(Cost cost);

    void updatePaymentStatus(Integer id, PaymentStatus paymentStatus);
}
