package net.focik.homeoffice.utils;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StringHelperTest {

    @Test
    void extractTextBetweenColonAndParenthesis() {
        //given
        String input1 = "Cykl: HAJMDAL (tom 1)";
        String input2 = "Cykl: Star Force (tom 3)";
        String input3 = "No match here";
        String input4 = "Cykl: Without Parenthesis";
        String input5 = "";

        //when
        String result1 = StringHelper.extractTextBetweenColonAndParenthesis(input1);
        String result2 = StringHelper.extractTextBetweenColonAndParenthesis(input2);
        String result3 = StringHelper.extractTextBetweenColonAndParenthesis(input3);
        String result4 = StringHelper.extractTextBetweenColonAndParenthesis(input4);
        String result5 = StringHelper.extractTextBetweenColonAndParenthesis(input5);

        //then
        assertEquals("HAJMDAL", result1);
        assertEquals("Star Force", result2);
        assertEquals("", result3);
        assertEquals("", result4);
        assertEquals("", result5);
    }

    @Test
    void extractNumberFromParentheses() {
        //given
        String input1 = "Cykl: HAJMDAL (tom 1)";
        String input2 = "Cykl: Star Force (tom 3)";
        String input3 = "No numbers (in parentheses)";
        String input4 = "Multiple numbers (123) and (456)";
        String input5 = "Cykl: Without Parenthesis";
        String input6 = "";
        String input7 = "Cykl: Pola dawno zapomnianych bitew (tom 0.5)";

        //when
        String result1 = StringHelper.extractNumberFromParentheses(input1);
        String result2 = StringHelper.extractNumberFromParentheses(input2);
        String result3 = StringHelper.extractNumberFromParentheses(input3);
        String result4 = StringHelper.extractNumberFromParentheses(input4);
        String result5 = StringHelper.extractNumberFromParentheses(input5);
        String result6 = StringHelper.extractNumberFromParentheses(input6);
        String result7 = StringHelper.extractNumberFromParentheses(input7);

        //then
        assertEquals("1", result1);
        assertEquals("3", result2);
        assertEquals("", result3);
        assertEquals("123", result4); // Only the first number in parentheses is extracted
        assertEquals("", result5);
        assertEquals("", result6);
        assertEquals("0.5", result7);
    }

    @Test
    void testExtractFileExtension() {
        // given
        String url1 = "https://s.lubimyczytac.pl/upload/books/4802000/4802688/589738-352x500.jpg";
        String url2 = "https://example.com/path/to/image.png";
        String url3 = "https://example.com/path/to/image";
        String url4 = "https://example.com/path.to/file/image.jpeg";
        String url5 = "https://example.com/path.to/file/image";
        String url6 = "";
        String url7 = null;

        // when
        String result1 = StringHelper.extractFileExtension(url1);
        String result2 = StringHelper.extractFileExtension(url2);
        String result3 = StringHelper.extractFileExtension(url3);
        String result4 = StringHelper.extractFileExtension(url4);
        String result5 = StringHelper.extractFileExtension(url5);
        String result6 = StringHelper.extractFileExtension(url6);
        String result7 = StringHelper.extractFileExtension(url7);

        // then
        assertEquals("jpg", result1);
        assertEquals("png", result2);
        assertEquals("", result3);
        assertEquals("jpeg", result4);
        assertEquals("", result5);
        assertEquals("", result6);
        assertEquals("", result7);
    }

    @Test
    void testMapToString() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");

        String result = StringHelper.mapToString(map, ";;");
        // New format: JSON
        assertTrue(result.contains("\"key1\":\"value1\"") || result.contains("key1=value1"),
                "Result should contain mapping in JSON or fallback format");
        assertEquals(map, StringHelper.stringToMap(result, ";;"));
    }

    @Test
    void testMapToStringEmptyMap() {
        Map<String, String> map = Map.of();
        String result = StringHelper.mapToString(map, ";;");
        assertEquals("", result);
    }

    @Test
    void testMapToStringWithSpecialCharacters() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("color", "red");
        map.put("description", "Cost=500PLN");  // Contains "="
        map.put("notes", "First;;Second");      // Contains ";;"

        String result = StringHelper.mapToString(map, ";;");
        // JSON format handles special characters safely
        Map<String, String> converted = StringHelper.stringToMap(result, ";;");
        assertEquals(map, converted);
    }

    @Test
    void testStringToMapJsonFormat() {
        String input = "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\"}";
        Map<String, String> expected = Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3"
        );
        assertEquals(expected, StringHelper.stringToMap(input, ";;"));
    }

    @Test
    void testStringToMapLegacyFormat() {
        // Test backward compatibility with old format
        String input = "key1=value1;;key2=value2;;key3=value3";
        Map<String, String> expected = Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3"
        );
        assertEquals(expected, StringHelper.stringToMap(input, ";;"));
    }

    @Test
    void testStringToMapEmptyString() {
        String input = "";
        Map<String, String> expected = Map.of();
        assertEquals(expected, StringHelper.stringToMap(input, ";;"));
    }

    @Test
    void testStringToMapLegacyWithEquals() {
        // Old format didn't handle "=" in values, but new split("=", 2) handles it
        String input = "key1=value1;;key2=Cost=500PLN";
        Map<String, String> result = StringHelper.stringToMap(input, ";;");
        assertEquals("Cost=500PLN", result.get("key2"));
    }

    @Test
    void testBothConversions() {
        Map<String, String> originalMap = new LinkedHashMap<>();
        originalMap.put("key1", "value1");
        originalMap.put("key2", "value2");
        originalMap.put("key3", "value3");

        String mapAsString = StringHelper.mapToString(originalMap, ";;");
        Map<String, String> convertedBackMap = StringHelper.stringToMap(mapAsString, ";;");
        assertEquals(originalMap, convertedBackMap);
    }

    @Test
    void testBothConversionsWithSpecialCharacters() {
        Map<String, String> originalMap = new LinkedHashMap<>();
        originalMap.put("color", "red");
        originalMap.put("description", "Cost=500PLN");
        originalMap.put("notes", "First;;Second");
        originalMap.put("url", "https://example.com?param1=value1&param2=value2");

        String mapAsString = StringHelper.mapToString(originalMap, ";;");
        Map<String, String> convertedBackMap = StringHelper.stringToMap(mapAsString, ";;");
        assertEquals(originalMap, convertedBackMap);
    }
}