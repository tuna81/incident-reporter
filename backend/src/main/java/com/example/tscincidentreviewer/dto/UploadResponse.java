package com.example.tscincidentreviewer.dto;

import com.example.tscincidentreviewer.model.IncidentRow;
import com.example.tscincidentreviewer.model.Stats;
import java.util.List;

public record UploadResponse(
    List<IncidentRow> items,
    Stats stats,
    String sourceFormat
) {
}
