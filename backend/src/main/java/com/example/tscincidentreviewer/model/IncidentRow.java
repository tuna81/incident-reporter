package com.example.tscincidentreviewer.model;

public record IncidentRow(
    String issueKey,
    String issueLinks,
    String label,
    String comment
) {
}
