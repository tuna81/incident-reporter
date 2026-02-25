package com.example.tscincidentreviewer.exception;

public class InvalidXlsxException extends RuntimeException {

  public InvalidXlsxException(Throwable cause) {
    super("invalid xlsx", cause);
  }
}
