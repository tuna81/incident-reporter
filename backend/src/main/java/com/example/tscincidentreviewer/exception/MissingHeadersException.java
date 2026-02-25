package com.example.tscincidentreviewer.exception;

import java.util.List;

public class MissingHeadersException extends RuntimeException {

  private final List<String> missingHeaders;

  public MissingHeadersException(List<String> missingHeaders) {
    super("missing required headers: " + String.join(", ", missingHeaders));
    this.missingHeaders = List.copyOf(missingHeaders);
  }

  public List<String> getMissingHeaders() {
    return missingHeaders;
  }
}
