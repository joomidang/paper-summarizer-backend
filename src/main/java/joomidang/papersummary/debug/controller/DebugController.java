package joomidang.papersummary.debug.controller;

import java.util.Map;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.debug.controller.response.DebugSuccessCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 디버깅용 더미 응답을 제공하는 컨트롤러
 */
@Slf4j
@RestController
@Tag(name="Debug", description = "디버깅용 더미 응답 API")
@RequestMapping("/api/debug")
@SecurityRequirements
public class DebugController {

    @Operation(summary = "커스텀 더미 응답", description = "Request body에 입력한 형태 그대로 응답. 간단한 테스트할때 쓰면 좋을듯?")
    @SecurityRequirement(name = "")
    @PostMapping("/custom")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomDummyResponse(@RequestBody Map<String, Object> data) {

        // 커스터마이징된 응답 생성


        return ResponseEntity.ok(ApiResponse.successWithData(
                DebugSuccessCode.CUSTOM_DEBUG_RESPONSE_FETCHED, data));
    }
}