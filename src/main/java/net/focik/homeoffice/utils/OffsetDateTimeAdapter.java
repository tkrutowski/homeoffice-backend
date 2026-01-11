package net.focik.homeoffice.utils;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeAdapter extends XmlAdapter<String, OffsetDateTime> {

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public OffsetDateTime unmarshal(String v) throws Exception {
        return OffsetDateTime.parse(v, formatter);
    }

    @Override
    public String marshal(OffsetDateTime v) throws Exception {
        return v.format(formatter);
    }
}
