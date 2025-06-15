package joomidang.papersummary.common.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name="Health Check", description = "서버 상태 확인을 위한 API")
public class HealthCheckController {
    @Operation(summary = "서버 상태 확인", description = "현재 서버의 상태와 타임스탬프를 반환합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value={
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "서버가 정상 작동 중",
            content = @Content(mediaType = "application/json",
            examples = {
                    @ExampleObject(
                            value = "{\n" +
                                    "  \"status\": \"UP\",\n" +
                                    "  \"timestamp\": \"2025-05-18T00:00:10.669564300\"\n" +
                                    "}"
                    )
            }))
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();

        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}
