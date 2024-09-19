package net.focik.homeoffice.goahead.infrastructure.inMemory;

import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceDbDto;
import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceItemDbDto;

import java.util.HashMap;
import java.util.Map;


public class DataBaseInvoiceItem {
    private static HashMap<Long, InvoiceItemDbDto> invoiceItemDbDtoHashMap;

    public static Map<Long, InvoiceItemDbDto> getInvoiceItemDbDtoHashMap() {
        if (invoiceItemDbDtoHashMap == null)
            invoiceItemDbDtoHashMap = new HashMap<>();
        return invoiceItemDbDtoHashMap;
    }


    private DataBaseInvoiceItem() {
    }
}
