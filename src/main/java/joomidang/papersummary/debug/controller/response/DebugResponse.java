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
public class DebugResponse {
    private String id;
    private String name;
    private String description;
    private LocalDateTime timestamp;
    private List<String> tags;
    private Map<String, Object> properties;
    
    public static DebugResponse createDummyResponse() {
        return DebugResponse.builder()
                .id("debug-123")
                .name("Debug Sample")
                .description("This is a dummy response for debugging purposes")
                .timestamp(LocalDateTime.now())
                .tags(List.of("debug", "test", "dummy"))
                .properties(Map.of(
                    "stringValue", "text value",
                    "numberValue", 42,
                    "booleanValue", true,
                    "nullValue", null
                ))
                .build();
    }
}