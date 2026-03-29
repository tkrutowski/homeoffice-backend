package net.focik.homeoffice.goahead.domain.cost.port.primary;

import net.focik.homeoffice.goahead.domain.cost.Cost;

public interface AddCostUseCase {
    Cost addCost(Cost cost);
}
