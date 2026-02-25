package com.example.tscincidentreviewer.controller;

import com.example.tscincidentreviewer.dto.UploadResponse;
import com.example.tscincidentreviewer.exception.InvalidXlsxException;
import com.example.tscincidentreviewer.exception.MissingFileException;
import com.example.tscincidentreviewer.service.XlsxParseService;
import com.example.tscincidentreviewer.store.IncidentStore;
import java.io.IOException;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/incidents")
public class IncidentUploadController {

  private final XlsxParseService xlsxParseService;
  private final IncidentStore incidentStore;

  public IncidentUploadController(XlsxParseService xlsxParseService, IncidentStore incidentStore) {
    this.xlsxParseService = xlsxParseService;
    this.incidentStore = incidentStore;
  }

  @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public UploadResponse upload(@RequestParam(value = "file", required = false) MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new MissingFileException();
    }

    String filename = file.getOriginalFilename();
    if (filename == null) {
      throw new IllegalArgumentException("file name is required");
    }

    String lowerCaseFilename = filename.toLowerCase(Locale.ROOT);
    if (!lowerCaseFilename.endsWith(".xlsx") && !lowerCaseFilename.endsWith(".csv")) {
      throw new IllegalArgumentException("only .xlsx or .csv files are supported");
    }

    try {
      UploadResponse response = xlsxParseService.parse(file.getInputStream());
      incidentStore.save(response.items());
      return response;
    } catch (IOException ex) {
      throw new InvalidXlsxException(ex);
    }
  }
}
