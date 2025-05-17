package joomidang.papersummary.debug.controller.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexDebugResponse {
    private String id;
    private String name;
    private NestedObject data;
    private List<NestedObject> items;
    private Map<String, NestedObject> mappedItems;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NestedObject {
        private String id;
        private String name;
        private String description;
        private LocalDateTime createdAt;
        private Map<String, Object> attributes;
    }
    
    public static ComplexDebugResponse createComplexDummyResponse() {
        NestedObject mainData = NestedObject.builder()
                .id("main-data-1")
                .name("Main Data")
                .description("This is the main data object")
                .createdAt(LocalDateTime.now())
                .attributes(Map.of(
                    "priority", "high",
                    "status", "active",
                    "count", 5
                ))
                .build();
                
        NestedObject item1 = NestedObject.builder()
                .id("item-1")
                .name("First Item")
                .description("This is the first item")
                .createdAt(LocalDateTime.now().minusDays(1))
                .attributes(Map.of(
                    "priority", "medium",
                    "status", "pending",
                    "count", 3
                ))
                .build();
                
        NestedObject item2 = NestedObject.builder()
                .id("item-2")
                .name("Second Item")
                .description("This is the second item")
                .createdAt(LocalDateTime.now().minusDays(2))
                .attributes(Map.of(
                    "priority", "low",
                    "status", "completed",
                    "count", 7
                ))
                .build();
                
        return ComplexDebugResponse.builder()
                .id("complex-debug-123")
                .name("Complex Debug Sample")
                .data(mainData)
                .items(List.of(item1, item2))
                .mappedItems(Map.of(
                    "first", item1,
                    "second", item2
                ))
                .build();
    }
}