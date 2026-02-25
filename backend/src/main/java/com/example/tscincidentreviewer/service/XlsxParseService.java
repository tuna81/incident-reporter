package com.example.tscincidentreviewer.service;

import com.example.tscincidentreviewer.dto.UploadResponse;
import com.example.tscincidentreviewer.exception.InvalidXlsxException;
import com.example.tscincidentreviewer.exception.MissingHeadersException;
import com.example.tscincidentreviewer.model.IncidentRow;
import com.example.tscincidentreviewer.model.StatRow;
import com.example.tscincidentreviewer.model.Stats;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class XlsxParseService {

  enum SourceFormat {
    PREPROCESSED,
    RAW_JIRA
  }

  private record TabularData(List<String> headers, List<Map<String, String>> rows) {
  }

  private static final String PROCESSED_ISSUE_KEY = "Issue Key";
  private static final String PROCESSED_ISSUE_LINKS = "Custom Field (Issue Links)";
  private static final String PROCESSED_LABEL = "Label";
  private static final String PROCESSED_COMMENT = "Comment";

  private static final String RAW_ISSUE_KEY = "Issue key";
  private static final String RAW_ISSUE_LINKS = "Custom field (Issue Links)";
  private static final String RAW_LABEL = "Labels";
  private static final String RAW_COMMENT = "Comment";

  private static final String EMPTY_BUCKET = "(empty)";
  private static final int DEFAULT_MAX_PARSED_ROWS = 50_000;

  private int maxParsedRows = DEFAULT_MAX_PARSED_ROWS;

  XlsxParseService(int maxParsedRows) {
    this.maxParsedRows = validateMaxRows(maxParsedRows);
  }

  public XlsxParseService() {
  }

  @Value("${app.upload.max-rows:50000}")
  void setMaxParsedRows(int maxParsedRows) {
    this.maxParsedRows = validateMaxRows(maxParsedRows);
  }

  public UploadResponse parse(InputStream inputStream) {
    byte[] fileBytes;
    try {
      fileBytes = inputStream.readAllBytes();
    } catch (IOException ex) {
      throw new InvalidXlsxException(ex);
    }

    if (fileBytes.length == 0) {
      throw new InvalidXlsxException(null);
    }

    try {
      return normalizeRows(parseXlsxTable(fileBytes));
    } catch (MissingHeadersException ex) {
      throw ex;
    } catch (InvalidXlsxException ex) {
      throw ex;
    } catch (Exception xlsxError) {
      try {
        return normalizeRows(parseCsvTable(fileBytes));
      } catch (MissingHeadersException ex) {
        throw ex;
      } catch (Exception csvError) {
        throw new InvalidXlsxException(csvError);
      }
    }
  }

  SourceFormat detectSourceFormat(List<String> headers) {
    boolean hasRawIssueKey = false;
    boolean hasProcessedIssueKey = false;

    for (String header : headers) {
      String trimmed = stripBom(header).trim();
      if (RAW_ISSUE_KEY.equals(trimmed)) {
        hasRawIssueKey = true;
      }
      if (PROCESSED_ISSUE_KEY.equals(trimmed)) {
        hasProcessedIssueKey = true;
      }
    }

    if (hasRawIssueKey) {
      return SourceFormat.RAW_JIRA;
    }

    if (hasProcessedIssueKey) {
      return SourceFormat.PREPROCESSED;
    }

    throw new MissingHeadersException(List.of(RAW_ISSUE_KEY, PROCESSED_ISSUE_KEY));
  }

  public String cleanJiraComment(String rawComment) {
    if (rawComment == null || rawComment.isBlank()) {
      return "";
    }

    String[] lines = rawComment.split("\\R", -1);
    List<String> cleanedLines = new ArrayList<>();

    for (String line : lines) {
      String cleaned = cleanJiraCommentLine(line);
      if (!cleaned.isEmpty()) {
        cleanedLines.add(cleaned);
      }
    }

    return String.join("\n", cleanedLines);
  }

  String normalizeRawLabels(String rawLabels) {
    if (rawLabels == null || rawLabels.isBlank()) {
      return "";
    }

    String[] tokens = rawLabels.split("[,;\\s]+");
    StringJoiner joiner = new StringJoiner(", ");

    for (String token : tokens) {
      String cleaned = token.trim();
      if (!cleaned.isEmpty()) {
        joiner.add(cleaned);
      }
    }

    return joiner.toString();
  }

  String normalizeRawIssueLinks(String rawIssueLinks) {
    if (rawIssueLinks == null || rawIssueLinks.isBlank()) {
      return "";
    }

    String[] tokens = rawIssueLinks.split("[,;\\r\\n]+");
    StringJoiner joiner = new StringJoiner(", ");

    for (String token : tokens) {
      String cleaned = token.trim();
      if (!cleaned.isEmpty()) {
        joiner.add(cleaned);
      }
    }

    return joiner.toString();
  }

  private UploadResponse normalizeRows(TabularData table) {
    SourceFormat format = detectSourceFormat(table.headers());

    List<String> requiredHeaders = switch (format) {
      case RAW_JIRA -> List.of(RAW_ISSUE_KEY, RAW_ISSUE_LINKS, RAW_LABEL, RAW_COMMENT);
      case PREPROCESSED -> List.of(PROCESSED_ISSUE_KEY, PROCESSED_ISSUE_LINKS, PROCESSED_LABEL, PROCESSED_COMMENT);
    };

    Map<String, String> resolvedHeaders = resolveRequiredHeaders(table.headers(), requiredHeaders);

    String issueKeyColumn = switch (format) {
      case RAW_JIRA -> resolvedHeaders.get(RAW_ISSUE_KEY);
      case PREPROCESSED -> resolvedHeaders.get(PROCESSED_ISSUE_KEY);
    };

    String issueLinksColumn = switch (format) {
      case RAW_JIRA -> resolvedHeaders.get(RAW_ISSUE_LINKS);
      case PREPROCESSED -> resolvedHeaders.get(PROCESSED_ISSUE_LINKS);
    };

    String labelColumn = switch (format) {
      case RAW_JIRA -> resolvedHeaders.get(RAW_LABEL);
      case PREPROCESSED -> resolvedHeaders.get(PROCESSED_LABEL);
    };

    String commentColumn = switch (format) {
      case RAW_JIRA -> resolvedHeaders.get(RAW_COMMENT);
      case PREPROCESSED -> resolvedHeaders.get(PROCESSED_COMMENT);
    };

    List<IncidentRow> items = new ArrayList<>();

    for (Map<String, String> row : table.rows()) {
      String issueKey = normalizeField(readCell(row, issueKeyColumn));
      String issueLinks = normalizeField(readCell(row, issueLinksColumn));
      String label = normalizeField(readCell(row, labelColumn));
      String comment = normalizeField(readCell(row, commentColumn));

      issueKey = normalizeIssueKey(issueKey);

      if (format == SourceFormat.RAW_JIRA) {
        issueLinks = normalizeRawIssueLinks(issueLinks);
        label = normalizeRawLabels(label);
        comment = cleanJiraComment(comment);
      }

      if (issueKey.isEmpty() && issueLinks.isEmpty() && label.isEmpty() && comment.isEmpty()) {
        continue;
      }

      items.add(new IncidentRow(issueKey, issueLinks, label, comment));
    }

    Stats stats = new Stats(
        buildStats(items, IncidentRow::issueLinks),
        buildStats(items, IncidentRow::label)
    );

    return new UploadResponse(items, stats, format.name());
  }

  private Map<String, String> resolveRequiredHeaders(List<String> headers, List<String> requiredHeaders) {
    Map<String, String> normalizedToHeader = new HashMap<>();
    for (String header : headers) {
      String cleanedHeader = stripBom(header).trim();
      String normalized = normalize(cleanedHeader);
      if (!normalized.isEmpty()) {
        normalizedToHeader.putIfAbsent(normalized, cleanedHeader);
      }
    }

    List<String> missing = new ArrayList<>();
    Map<String, String> resolved = new HashMap<>();

    for (String required : requiredHeaders) {
      String resolvedHeader = normalizedToHeader.get(normalize(required));
      if (resolvedHeader == null) {
        missing.add(required);
      } else {
        resolved.put(required, resolvedHeader);
      }
    }

    if (!missing.isEmpty()) {
      throw new MissingHeadersException(missing);
    }

    return resolved;
  }

  private List<StatRow> buildStats(List<IncidentRow> items, Function<IncidentRow, String> extractor) {
    if (items.isEmpty()) {
      return List.of();
    }

    Map<String, Long> counts = new HashMap<>();
    for (IncidentRow item : items) {
      String value = extractor.apply(item);
      String name = value == null || value.isBlank() ? EMPTY_BUCKET : value;
      counts.put(name, counts.getOrDefault(name, 0L) + 1L);
    }

    double totalItems = items.size();
    return counts.entrySet().stream()
        .sorted((left, right) -> {
          int compareCount = Long.compare(right.getValue(), left.getValue());
          if (compareCount != 0) {
            return compareCount;
          }
          return left.getKey().compareToIgnoreCase(right.getKey());
        })
        .map(entry -> new StatRow(
            entry.getKey(),
            entry.getValue(),
            roundToOneDecimal((entry.getValue() / totalItems) * 100.0)
        ))
        .toList();
  }

  private TabularData parseXlsxTable(byte[] fileBytes) throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
      if (workbook.getNumberOfSheets() == 0) {
        throw new InvalidXlsxException(null);
      }

      Sheet sheet = workbook.getSheetAt(0);
      DataFormatter formatter = new DataFormatter();

      Row headerRow = sheet.getRow(sheet.getFirstRowNum());
      if (headerRow == null || headerRow.getLastCellNum() < 0) {
        return new TabularData(List.of(), List.of());
      }

      List<String> headers = new ArrayList<>();
      for (int col = 0; col < headerRow.getLastCellNum(); col++) {
        headers.add(sanitizeHeader(formatter.formatCellValue(headerRow.getCell(col))));
      }

      List<Map<String, String>> rows = new ArrayList<>();
      int parsedRowCount = 0;
      for (int rowNum = headerRow.getRowNum() + 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
        parsedRowCount++;
        ensureWithinRowLimit(parsedRowCount);

        Row row = sheet.getRow(rowNum);
        Map<String, String> mappedRow = new HashMap<>();

        for (int col = 0; col < headers.size(); col++) {
          String value = row == null ? "" : formatter.formatCellValue(row.getCell(col));
          mappedRow.put(headers.get(col), value);
        }

        rows.add(mappedRow);
      }

      return new TabularData(headers, rows);
    }
  }

  private TabularData parseCsvTable(byte[] fileBytes) throws IOException {
    try (Reader reader = new InputStreamReader(new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8);
         CSVParser parser = CSVFormat.DEFAULT.builder()
             .setHeader()
             .setSkipHeaderRecord(true)
             .build()
             .parse(reader)) {

      if (parser.getHeaderMap() == null || parser.getHeaderMap().isEmpty()) {
        return new TabularData(List.of(), List.of());
      }

      List<Map.Entry<String, Integer>> headerEntries = parser.getHeaderMap().entrySet().stream()
          .sorted(Map.Entry.comparingByValue())
          .toList();

      List<String> headers = headerEntries.stream()
          .map(entry -> sanitizeHeader(entry.getKey()))
          .toList();

      List<Map<String, String>> rows = new ArrayList<>();
      int parsedRowCount = 0;
      for (CSVRecord record : parser) {
        parsedRowCount++;
        ensureWithinRowLimit(parsedRowCount);

        Map<String, String> mappedRow = new HashMap<>();
        for (Map.Entry<String, Integer> headerEntry : headerEntries) {
          String originalHeader = headerEntry.getKey();
          String sanitizedHeader = sanitizeHeader(originalHeader);
          mappedRow.put(sanitizedHeader, readCsvCell(record, originalHeader));
        }
        rows.add(mappedRow);
      }

      return new TabularData(headers, rows);
    }
  }

  private String readCsvCell(CSVRecord record, String header) {
    if (!record.isMapped(header) || !record.isSet(header)) {
      return "";
    }

    String value = record.get(header);
    return value == null ? "" : value;
  }

  private String cleanJiraCommentLine(String line) {
    String trimmed = line == null ? "" : line.trim();
    if (trimmed.isEmpty()) {
      return "";
    }

    String[] parts = trimmed.split(";", 3);
    if (parts.length == 3) {
      return parts[2].trim();
    }

    return trimmed;
  }

  private String readCell(Map<String, String> row, String header) {
    if (row == null || header == null) {
      return "";
    }

    String value = row.get(header);
    return value == null ? "" : value;
  }

  private void ensureWithinRowLimit(int parsedRowCount) {
    if (parsedRowCount > maxParsedRows) {
      throw new IllegalArgumentException("file has too many rows");
    }
  }

  private String normalizeField(String value) {
    return value == null ? "" : value.trim();
  }

  private String normalizeIssueKey(String issueKey) {
    if ("null".equalsIgnoreCase(issueKey)) {
      return "";
    }
    return issueKey;
  }

  private double roundToOneDecimal(double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  private String normalize(String value) {
    return stripBom(value).trim().toLowerCase(Locale.ROOT);
  }

  private String sanitizeHeader(String header) {
    return stripBom(header == null ? "" : header);
  }

  private String stripBom(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\uFEFF", "");
  }

  private int validateMaxRows(int maxParsedRows) {
    if (maxParsedRows <= 0) {
      return DEFAULT_MAX_PARSED_ROWS;
    }
    return maxParsedRows;
  }
}
