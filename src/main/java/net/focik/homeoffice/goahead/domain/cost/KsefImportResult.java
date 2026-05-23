package net.focik.homeoffice.goahead.domain.cost;

import java.util.List;

public record KsefImportResult(List<Cost> newCosts, int found, int duplicates) {}
