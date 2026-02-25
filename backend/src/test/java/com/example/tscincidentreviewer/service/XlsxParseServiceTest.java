package com.example.tscincidentreviewer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.tscincidentreviewer.dto.UploadResponse;
import com.example.tscincidentreviewer.exception.MissingHeadersException;
import com.example.tscincidentreviewer.model.IncidentRow;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class XlsxParseServiceTest {

  private final XlsxParseService service = new XlsxParseService();

  @Test
  void detectSourceFormatWhenRawIssueKeyExistsReturnsRawJira() {
    XlsxParseService.SourceFormat format = service.detectSourceFormat(List.of(
        "Issue key",
        "Custom field (Issue Links)",
        "Labels",
        "Comment"
    ));

    assertEquals(XlsxParseService.SourceFormat.RAW_JIRA, format);
  }

  @Test
  void detectSourceFormatWhenProcessedIssueKeyExistsReturnsPreprocessed() {
    XlsxParseService.SourceFormat format = service.detectSourceFormat(List.of(
        "Issue Key",
        "Custom Field (Issue Links)",
        "Label",
        "Comment"
    ));

    assertEquals(XlsxParseService.SourceFormat.PREPROCESSED, format);
  }

  @Test
  void detectSourceFormatWhenBothMissingThrows() {
    assertThrows(
        MissingHeadersException.class,
        () -> service.detectSourceFormat(List.of("Foo", "Bar"))
    );
  }

  @Test
  void cleanJiraCommentSingleLineWithMetadataExtractsComment() {
    String cleaned = service.cleanJiraComment("25/Feb/26 10:06 AM;712020:uuid;actual comment text");

    assertEquals("actual comment text", cleaned);
  }

  @Test
  void cleanJiraCommentMultiLineExtractsEachLineComment() {
    String raw = "25/Feb/26 10:06 AM;111:uuid;first line\n26/Feb/26 10:07 AM;222:uuid;second line";

    String cleaned = service.cleanJiraComment(raw);

    assertEquals("first line\nsecond line", cleaned);
  }

  @Test
  void cleanJiraCommentLineWithoutSemicolonIsKept() {
    String cleaned = service.cleanJiraComment("  plain text comment  ");

    assertEquals("plain text comment", cleaned);
  }

  @Test
  void normalizeRawLabelsWithMultipleSpacesUsesCommaSeparatedFormat() {
    String normalized = service.normalizeRawLabels("a b  c");

    assertEquals("a, b, c", normalized);
  }

  @Test
  void normalizeRawLabelsWithMixedSeparatorsUsesCommaSeparatedFormat() {
    String normalized = service.normalizeRawLabels("a, b; c\nd");

    assertEquals("a, b, c, d", normalized);
  }

  @Test
  void normalizeRawLabelsWithEmptyOrNullReturnsEmptyString() {
    assertEquals("", service.normalizeRawLabels(""));
    assertEquals("", service.normalizeRawLabels(null));
  }

  @Test
  void normalizeRawIssueLinksSingleValue() {
    String normalized = service.normalizeRawIssueLinks("Payments");

    assertEquals("Payments", normalized);
  }

  @Test
  void normalizeRawIssueLinksCommaSeparatedValues() {
    String normalized = service.normalizeRawIssueLinks("Payments, Billing");

    assertEquals("Payments, Billing", normalized);
  }

  @Test
  void normalizeRawIssueLinksSemicolonAndNewlineSeparatedValues() {
    String normalized = service.normalizeRawIssueLinks("Payments;\nBilling");

    assertEquals("Payments, Billing", normalized);
  }

  @Test
  void parseCsvHandlesQuotedMultilineCommentCell() {
    String csv = "\"Issue key\",\"Custom field (Issue Links)\",\"Labels\",\"Comment\"\n"
        + "\"TSC-1\",\"Payments\",\"Bug\",\"25/Feb/26 10:06 AM;111:uuid;first line\n"
        + "26/Feb/26 10:07 AM;222:uuid;second line\"\n";

    UploadResponse response = service.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

    assertEquals(1, response.items().size());

    IncidentRow row = response.items().get(0);
    assertEquals("TSC-1", row.issueKey());
    assertEquals("Payments", row.issueLinks());
    assertEquals("Bug", row.label());
    assertEquals("first line\nsecond line", row.comment());
  }

  @Test
  void parseCsvHandlesQuotedCommaInCommentCell() {
    String csv = "\"Issue key\",\"Custom field (Issue Links)\",\"Labels\",\"Comment\"\n"
        + "\"TSC-2\",\"Billing\",\"Task\",\"25/Feb/26 10:06 AM;333:uuid;text, with comma\"\n";

    UploadResponse response = service.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

    assertEquals(1, response.items().size());

    IncidentRow row = response.items().get(0);
    assertEquals("text, with comma", row.comment());
  }

  @Test
  void parseCsvRawJiraNormalizesMultiLabelField() {
    String csv = "\"Issue key\",\"Custom field (Issue Links)\",\"Labels\",\"Comment\"\n"
        + "\"TSC-3\",\"Ops\",\"bug urgent  sev1\",\"25/Feb/26 10:06 AM;333:uuid;ok\"\n";

    UploadResponse response = service.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

    assertEquals(1, response.items().size());
    assertEquals("bug, urgent, sev1", response.items().get(0).label());
    assertEquals("RAW_JIRA", response.sourceFormat());
  }

  @Test
  void parseCsvRawJiraNormalizesIssueLinksSeparators() {
    String csv = "\"Issue key\",\"Custom field (Issue Links)\",\"Labels\",\"Comment\"\n"
        + "\"TSC-5\",\"Payments;\nBilling\",\"bug\",\"25/Feb/26 10:06 AM;333:uuid;ok\"\n";

    UploadResponse response = service.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

    assertEquals(1, response.items().size());
    assertEquals("Payments, Billing", response.items().get(0).issueLinks());
  }

  @Test
  void parseCsvPreprocessedKeepsLabelUntouched() {
    String csv = "\"Issue Key\",\"Custom Field (Issue Links)\",\"Label\",\"Comment\"\n"
        + "\"TSC-4\",\"Ops\",\"bug urgent sev1\",\"plain\"\n";

    UploadResponse response = service.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

    assertEquals(1, response.items().size());
    assertEquals("bug urgent sev1", response.items().get(0).label());
    assertEquals("PREPROCESSED", response.sourceFormat());
  }

  @Test
  void parseCsvIssueKeyWithNullLiteralNormalizesToEmptyString() {
    String csv = "\"Issue Key\",\"Custom Field (Issue Links)\",\"Label\",\"Comment\"\n"
        + "\" null \",\"Ops\",\"bug\",\"comment\"\n";

    UploadResponse response = service.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

    assertEquals(1, response.items().size());
    assertEquals("", response.items().get(0).issueKey());
  }

  @Test
  void parseCsvRawJiraSampleFromCurlParsesSuccessfully() {
    String csv = "\"Issue key\",\"Custom field (Issue Links)\",\"Labels\",\"Comment\"\n"
        + "\"TSC-10\",\"Payments, Billing\",\"bug urgent\",\"25/Feb/26 10:06 AM;712020:uuid;text of comment\"\n";

    UploadResponse response = service.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

    assertEquals("RAW_JIRA", response.sourceFormat());
    assertEquals(1, response.items().size());

    IncidentRow row = response.items().get(0);
    assertEquals("TSC-10", row.issueKey());
    assertEquals("Payments, Billing", row.issueLinks());
    assertEquals("bug, urgent", row.label());
    assertEquals("text of comment", row.comment());
  }

  @Test
  void parseCsvSmokeDoesNotThrowNoClassDefFoundError() {
    String csv = "\"Issue key\",\"Custom field (Issue Links)\",\"Labels\",\"Comment\"\n"
        + "\"TSC-11\",\"Payments\",\"bug\",\"25/Feb/26 10:06 AM;1:uuid;ok\"\n";

    UploadResponse response = assertDoesNotThrow(
        () -> service.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)))
    );

    assertEquals(1, response.items().size());
    assertEquals("TSC-11", response.items().get(0).issueKey());
  }

  @Test
  void parseCsvFullyEmptyRowIsSkippedAfterNormalization() {
    String csv = "\"Issue Key\",\"Custom Field (Issue Links)\",\"Label\",\"Comment\"\n"
        + "\"   \",\" \",\"\",\"  \"\n";

    UploadResponse response = service.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

    assertEquals(0, response.items().size());
  }

  @Test
  void parseCsvExceedingConfiguredRowLimitThrows() {
    XlsxParseService limitedService = new XlsxParseService(2);
    String csv = "\"Issue Key\",\"Custom Field (Issue Links)\",\"Label\",\"Comment\"\n"
        + "\"TSC-1\",\"A\",\"L1\",\"c1\"\n"
        + "\"TSC-2\",\"B\",\"L2\",\"c2\"\n"
        + "\"TSC-3\",\"C\",\"L3\",\"c3\"\n";

    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> limitedService.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)))
    );

    assertEquals("file has too many rows", ex.getMessage());
  }
}
