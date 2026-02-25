package com.example.tscincidentreviewer.store;

import com.example.tscincidentreviewer.model.IncidentRow;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class IncidentStore {

  private final AtomicReference<List<IncidentRow>> latestItemsRef = new AtomicReference<>();

  public void save(List<IncidentRow> items) {
    latestItemsRef.set(List.copyOf(items));
  }

  public Optional<List<IncidentRow>> getLatestItems() {
    return Optional.ofNullable(latestItemsRef.get());
  }
}
