package com.example.tscincidentreviewer.controller;

import com.example.tscincidentreviewer.model.IncidentRow;
import com.example.tscincidentreviewer.store.IncidentStore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
public class IncidentExportController {

  private static final String JIRA_BASE_URL =
      "https://bitpace.atlassian.net/jira/servicedesk/projects/TSC/queues/custom/189/";

  private final IncidentStore incidentStore;

  public IncidentExportController(IncidentStore incidentStore) {
    this.incidentStore = incidentStore;
  }

  @GetMapping(path = "/export/xlsx")
  public ResponseEntity<byte[]> exportXlsx() {
    List<IncidentRow> items = incidentStore.getLatestItems()
        .orElseThrow(() -> new IllegalArgumentException("no data to export"));

    try {
      byte[] file = buildWorkbook(items);
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              ContentDisposition.attachment().filename("tsc_report_normalized.xlsx").build().toString()
          )
          .body(file);
    } catch (IOException ex) {
      throw new IllegalStateException("failed to export xlsx", ex);
    }
  }

  private byte[] buildWorkbook(List<IncidentRow> items) throws IOException {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("incidents");
      CreationHelper creationHelper = workbook.getCreationHelper();

      CellStyle headerStyle = createHeaderStyle(workbook);
      CellStyle hyperlinkStyle = createHyperlinkStyle(workbook);
      CellStyle wrapTextStyle = createWrapTextStyle(workbook);

      Row header = sheet.createRow(0);
      createHeaderCell(header, 0, "Issue Key", headerStyle);
      createHeaderCell(header, 1, "Custom Field (Issue Links)", headerStyle);
      createHeaderCell(header, 2, "Label", headerStyle);
      createHeaderCell(header, 3, "Comment", headerStyle);

      int rowIndex = 1;
      for (IncidentRow item : items) {
        Row row = sheet.createRow(rowIndex++);

        Cell issueKeyCell = row.createCell(0);
        issueKeyCell.setCellValue(item.issueKey());
        if (!item.issueKey().isBlank()) {
          Hyperlink hyperlink = creationHelper.createHyperlink(HyperlinkType.URL);
          hyperlink.setAddress(JIRA_BASE_URL + item.issueKey());
          issueKeyCell.setHyperlink(hyperlink);
          issueKeyCell.setCellStyle(hyperlinkStyle);
        }

        row.createCell(1).setCellValue(item.issueLinks());
        row.createCell(2).setCellValue(item.label());

        Cell commentCell = row.createCell(3);
        commentCell.setCellValue(item.comment());
        commentCell.setCellStyle(wrapTextStyle);
      }

      for (int col = 0; col <= 2; col++) {
        sheet.autoSizeColumn(col);
      }

      workbook.write(output);
      return output.toByteArray();
    }
  }

  private void createHeaderCell(Row row, int column, String value, CellStyle style) {
    Cell cell = row.createCell(column);
    cell.setCellValue(value);
    cell.setCellStyle(style);
  }

  private CellStyle createHeaderStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    return style;
  }

  private CellStyle createHyperlinkStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setColor(IndexedColors.BLUE.getIndex());
    font.setUnderline(Font.U_SINGLE);
    style.setFont(font);
    return style;
  }

  private CellStyle createWrapTextStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setWrapText(true);
    return style;
  }
}
