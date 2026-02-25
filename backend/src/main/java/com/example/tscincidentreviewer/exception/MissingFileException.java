package com.example.tscincidentreviewer.exception;

public class MissingFileException extends RuntimeException {

  public MissingFileException() {
    super("file is required");
  }
}
