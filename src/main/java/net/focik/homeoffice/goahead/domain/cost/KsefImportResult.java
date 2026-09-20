package net.focik.homeoffice.goahead.domain.cost;

import net.focik.homeoffice.async.AsyncTaskError;

import java.util.List;

public record KsefImportResult(List<Cost> newCosts, int found, int duplicates, List<AsyncTaskError> errors) {}
