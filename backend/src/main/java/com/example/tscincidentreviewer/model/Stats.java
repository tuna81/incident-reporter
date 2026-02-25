package com.example.tscincidentreviewer.model;

import java.util.List;

public record Stats(
    List<StatRow> byIssueLinks,
    List<StatRow> byLabel
) {
}
