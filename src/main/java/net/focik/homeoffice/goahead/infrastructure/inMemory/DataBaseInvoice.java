package net.focik.homeoffice.goahead.infrastructure.inMemory;

import net.focik.homeoffice.goahead.infrastructure.dto.CustomerDbDto;
import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceDbDto;

import java.util.HashMap;
import java.util.Map;


public class DataBaseInvoice {
    private static HashMap<Integer, InvoiceDbDto> invoiceDbDtoHashMap;

    public static Map<Integer, InvoiceDbDto> getInvoiceDbDtoHashMap() {
        if (invoiceDbDtoHashMap == null)
            invoiceDbDtoHashMap = new HashMap<>();
        return invoiceDbDtoHashMap;
    }


    private DataBaseInvoice() {
    }
}
