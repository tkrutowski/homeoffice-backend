package net.focik.homeoffice.fileService.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.fileService.domain.model.InvoiceClaudeResponseDto;
import net.focik.homeoffice.goahead.domain.cost.Cost;
import net.focik.homeoffice.goahead.domain.cost.CostItem;
import net.focik.homeoffice.goahead.domain.supplier.Supplier;
import net.focik.homeoffice.goahead.domain.supplier.port.secondary.SupplierRepository;
import net.focik.homeoffice.utils.share.PaymentMethod;
import net.focik.homeoffice.utils.share.PaymentStatus;
import net.focik.homeoffice.utils.share.Vat;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeInvoiceParserService {
    private final SupplierRepository supplierRepository;

    public Cost parse(InvoiceClaudeResponseDto dto) {
        Cost cost = new Cost();
        List<CostItem> items = new ArrayList<>();

        if (dto == null) {
            log.warn("InvoiceClaudeResponseDto is null");
            return cost;
        }

        cost.setNumber(dto.getNumber());
        cost.setInvoiceDate(parseDate(dto.getInvoiceDate()));
        cost.setSellDate(parseDate(dto.getSellDate()));
        cost.setPaymentDate(parseDate(dto.getPaymentDate()));
        cost.setPaymentMethod(parsePaymentMethod(dto.getPaymentMethod()));
        cost.setPaymentStatus(PaymentStatus.TO_PAY);

        Supplier supplier;
        if (dto.getSupplierNip() != null && !dto.getSupplierNip().isBlank()) {
            var existingSupplier = supplierRepository.findByNip(dto.getSupplierNip());
            if (existingSupplier.isPresent()) {
                supplier = existingSupplier.get();
                log.debug("Found existing supplier by NIP: {}", dto.getSupplierNip());
            } else {
                supplier = createSupplier(dto);
            }
        } else {
            supplier = createSupplier(dto);
        }
        cost.setSupplier(supplier);

        if (dto.getItems() != null) {
            for (InvoiceClaudeResponseDto.InvoiceItemDto itemDto : dto.getItems()) {
                CostItem item = parseItem(itemDto);
                items.add(item);
            }
        }

        cost.setCostItems(items);
        log.info("Parsed invoice: number={}, supplier={}, items={}",
                cost.getNumber(),
                supplier.getName(),
                items.size());
        return cost;
    }

    private Supplier createSupplier(InvoiceClaudeResponseDto dto) {
        Supplier supplier = new Supplier();
        supplier.setName(dto.getSupplierName());
        supplier.setNip(dto.getSupplierNip());
        supplier.setMail(null);
        supplier.setPhone(null);
        if (dto.getSupplierStreet() != null || dto.getSupplierCity() != null || dto.getSupplierZip() != null) {
            supplier.setAddress(dto.getSupplierCity(), dto.getSupplierStreet(), dto.getSupplierZip());
        }
        supplier.setAccountNumber(dto.getSupplierAccount());
        supplier.setBankName(dto.getSupplierBank());
        return supplier;
    }

    private CostItem parseItem(InvoiceClaudeResponseDto.InvoiceItemDto itemDto) {
        CostItem item = new CostItem();

        if (itemDto == null) {
            return item;
        }

        item.setName(itemDto.getName());
        item.setUnit(itemDto.getUnit() != null ? itemDto.getUnit() : "szt");

        try {
            item.setQuantity(Float.parseFloat(itemDto.getQuantity()));
        } catch (Exception e) {
            log.warn("Failed to parse quantity: {}", itemDto.getQuantity());
            item.setQuantity(1.0f);
        }

        item.setAmountNet(parseMoney(itemDto.getAmountNet()));
        item.setAmountVat(parseMoney(itemDto.getAmountVat()));
        item.setAmountGross(parseMoney(itemDto.getAmountGross()));
        item.setVat(parseVat(itemDto.getVatRate()));

        return item;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            } catch (Exception e2) {
                log.warn("Failed to parse date: {}", dateStr);
                return null;
            }
        }
    }

    private PaymentMethod parsePaymentMethod(String value) {
        if (value == null || value.isBlank()) {
            return PaymentMethod.TRANSFER;
        }

        String normalized = value.toLowerCase().trim();
        return switch (normalized) {
            case "przelew" -> PaymentMethod.TRANSFER;
            case "gotówka", "gotowka" -> PaymentMethod.CASH;
            case "płatność odroczona", "platnosc odroczona" -> PaymentMethod.CASH_LATE;
            default -> {
                log.warn("Unknown payment method: {}", value);
                yield PaymentMethod.TRANSFER;
            }
        };
    }

    private Vat parseVat(String vatRate) {
        if (vatRate == null || vatRate.isBlank()) {
            return null;
        }

        String normalized = vatRate.trim();
        return switch (normalized) {
            case "23" -> Vat.VAT_23;
            case "8" -> Vat.VAT_8;
            case "5" -> Vat.VAT_5;
            case "0" -> Vat.VAT_0;
            case "zw" -> Vat.VAT_ZW;
            default -> {
                log.warn("Unknown VAT rate: {}", vatRate);
                yield null;
            }
        };
    }

    private Money parseMoney(String value) {
        if (value == null || value.isBlank()) {
            return Money.of(0, "PLN");
        }

        try {
            String normalized = value.trim()
                    .replace(",", ".")
                    .replaceAll("[^0-9.]", "");
            if (normalized.isBlank()) {
                return Money.of(0, "PLN");
            }
            return Money.of(new BigDecimal(normalized), "PLN");
        } catch (Exception e) {
            log.warn("Failed to parse money: {}", value);
            return Money.of(0, "PLN");
        }
    }

}
