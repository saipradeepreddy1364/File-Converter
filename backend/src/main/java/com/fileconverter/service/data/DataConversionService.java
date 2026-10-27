package com.fileconverter.service.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class DataConversionService {

    private static final Logger log = LoggerFactory.getLogger(DataConversionService.class);

    private final ObjectMapper jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final XmlMapper xmlMapper = new XmlMapper();

    public void csvToXlsx(File csvFile, File outputFile) throws IOException {
        try (Reader reader = new FileReader(csvFile, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.parse(reader);
             Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            Sheet sheet = workbook.createSheet("Data");
            int rowIdx = 0;

            for (CSVRecord record : parser) {
                Row row = sheet.createRow(rowIdx++);
                for (int c = 0; c < record.size(); c++) {
                    String val = record.get(c);
                    // Try parsing as number
                    try {
                        double num = Double.parseDouble(val);
                        row.createCell(c).setCellValue(num);
                    } catch (NumberFormatException e) {
                        row.createCell(c).setCellValue(val);
                    }
                }
            }
            workbook.write(fos);
        }
    }

    public void xlsxToCsv(File xlsxFile, int sheetIndex, File outputFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(xlsxFile);
             Workbook workbook = new XSSFWorkbook(fis);
             FileWriter fw = new FileWriter(outputFile, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(fw, CSVFormat.DEFAULT)) {

            int idx = Math.max(0, Math.min(workbook.getNumberOfSheets() - 1, sheetIndex));
            Sheet sheet = workbook.getSheetAt(idx);

            for (Row row : sheet) {
                List<String> values = new ArrayList<>();
                for (Cell cell : row) {
                    values.add(getCellValue(cell));
                }
                printer.printRecord(values);
            }
        }
    }

    public void csvToJson(File csvFile, File outputFile) throws IOException {
        try (Reader reader = new FileReader(csvFile, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {

            List<String> headers = parser.getHeaderNames();
            List<Map<String, String>> rows = new ArrayList<>();

            for (CSVRecord record : parser) {
                Map<String, String> row = new LinkedHashMap<>();
                for (String header : headers) {
                    if (record.isSet(header)) {
                        row.put(header, record.get(header));
                    }
                }
                rows.add(row);
            }

            jsonMapper.writeValue(outputFile, rows);
        }
    }

    public void jsonToCsv(File jsonFile, File outputFile) throws IOException {
        JsonNode rootNode = jsonMapper.readTree(jsonFile);
        if (!rootNode.isArray()) {
            throw new IllegalArgumentException("JSON root must be an array of objects for CSV conversion");
        }

        Set<String> headers = new LinkedHashSet<>();
        for (JsonNode item : rootNode) {
            item.fieldNames().forEachRemaining(headers::add);
        }

        try (FileWriter fw = new FileWriter(outputFile, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(fw, CSVFormat.DEFAULT.builder().setHeader(headers.toArray(new String[0])).build())) {

            for (JsonNode item : rootNode) {
                List<String> record = new ArrayList<>();
                for (String h : headers) {
                    JsonNode val = item.get(h);
                    record.add(val != null && !val.isNull() ? val.asText() : "");
                }
                printer.printRecord(record);
            }
        }
    }

    public String formatJson(String rawJson) throws IOException {
        Object jsonObject = jsonMapper.readValue(rawJson, Object.class);
        return jsonMapper.writeValueAsString(jsonObject);
    }

    public String formatXml(String rawXml) throws IOException {
        JsonNode node = xmlMapper.readTree(rawXml);
        return xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }

    public String xmlToJson(String rawXml) throws IOException {
        JsonNode node = xmlMapper.readTree(rawXml);
        return jsonMapper.writeValueAsString(node);
    }

    public String jsonToXml(String rawJson) throws IOException {
        JsonNode node = jsonMapper.readTree(rawJson);
        return xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }

    public Map<String, Object> previewSpreadsheet(File spreadsheetFile, int maxRows) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        try (FileInputStream fis = new FileInputStream(spreadsheetFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            List<String> sheetNames = new ArrayList<>();
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                sheetNames.add(workbook.getSheetName(s));
            }
            result.put("sheets", sheetNames);

            Sheet firstSheet = workbook.getSheetAt(0);
            List<List<String>> rows = new ArrayList<>();
            int count = 0;
            for (Row row : firstSheet) {
                if (count++ >= maxRows) break;
                List<String> rowVals = new ArrayList<>();
                for (Cell cell : row) {
                    rowVals.add(getCellValue(cell));
                }
                rows.add(rowVals);
            }
            result.put("previewRows", rows);
        }
        return result;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toString();
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getCellFormula();
                }
            default: return "";
        }
    }
}
