package net.focik.homeoffice.goahead.infrastructure.inMemory;

import net.focik.homeoffice.goahead.infrastructure.dto.CustomerDbDto;

import java.util.HashMap;
import java.util.Map;


public class DataBaseCustomer {
    private static HashMap<Integer, CustomerDbDto> customerHashMap;

    public static Map<Integer, CustomerDbDto> getCustomerHashMap() {
        if (customerHashMap == null)
            customerHashMap = new HashMap<>();
        return customerHashMap;
    }


    private DataBaseCustomer() {
    }
}
