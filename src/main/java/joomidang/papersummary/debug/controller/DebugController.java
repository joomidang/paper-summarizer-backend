package joomidang.papersummary.debug.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.debug.controller.response.ComplexDebugResponse;
import joomidang.papersummary.debug.controller.response.DebugResponse;
import joomidang.papersummary.debug.controller.response.DebugSuccessCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 디버깅용 더미 응답을 제공하는 컨트롤러
 */
@Slf4j
@RestController
@Tag(name="Debug", description = "디버깅용 더미 응답 API")
@RequestMapping("/api/debug")
@SecurityRequirements
public class DebugController {

    /**
     * 기본 더미 응답 제공
     */
    @Operation(summary = "기본 더미 응답", description = "기본 더미 응답을 제공합니다")
    @GetMapping("/dummy")
    @SecurityRequirements
    public ResponseEntity<ApiResponse<DebugResponse>> getDummyResponse() {
        log.debug("디버깅용 더미 응답 요청 받음");

        DebugResponse response = DebugResponse.createDummyResponse();

        log.debug("디버깅용 더미 응답 생성 완료: {}", response);
        return ResponseEntity.ok(
                ApiResponse.successWithData(
                        DebugSuccessCode.DEBUG_RESPONSE_FETCHED, response));
    }

    /**
     * 복잡한 구조의 더미 응답 제공
     */
    @Operation(summary = "복잡한 더미 응답", description = "복잡한 구조의 더미 응답을 제공합니다")
    @SecurityRequirement(name = "")
    @GetMapping("/complex")
    public ResponseEntity<ApiResponse<ComplexDebugResponse>> getComplexDummyResponse() {
        log.info("복잡한 구조의 디버깅용 더미 응답 요청 받음");

        ComplexDebugResponse response = ComplexDebugResponse.createComplexDummyResponse();

        log.debug("복잡한 구조의 디버깅용 더미 응답 생성 완료");
        return ResponseEntity.ok(ApiResponse.successWithData(DebugSuccessCode.COMPLEX_DEBUG_RESPONSE_FETCHED, response));
    }

    /**
     * 커스터마이징 가능한 더미 응답 제공
     * @param id 응답 ID (기본값: "custom-123")
     * @param name 응답 이름 (기본값: "Custom Debug Sample")
     * @param description 응답 설명 (기본값: "This is a customizable dummy response")
     * @param tagString 태그 목록 (쉼표로 구분, 기본값: "custom,debug,test")
     * @return 커스터마이징된 더미 응답
     */
    @Operation(summary = "커스텀 더미 응답", description = "커스터마이징 가능한 더미 응답을 제공합니다")
    @SecurityRequirement(name = "")
    @GetMapping("/custom")
    public ResponseEntity<ApiResponse<DebugResponse>> getCustomDummyResponse(
            @RequestParam(required = false, defaultValue = "custom-123") String id,
            @RequestParam(required = false, defaultValue = "Custom Debug Sample") String name,
            @RequestParam(required = false, defaultValue = "This is a customizable dummy response") String description,
            @RequestParam(required = false, defaultValue = "custom,debug,test") String tagString) {

        log.debug("커스터마이징 가능한 디버깅용 더미 응답 요청 받음: id={}, name={}", id, name);

        // 쉼표로 구분된 태그 문자열을 리스트로 변환
        List<String> tags = List.of(tagString.split(","));

        // 커스텀 속성 맵 생성
        Map<String, Object> properties = new HashMap<>();
        properties.put("customId", id);
        properties.put("timestamp", System.currentTimeMillis());
        properties.put("requestTime", LocalDateTime.now().toString());

        // 커스터마이징된 응답 생성
        DebugResponse response = DebugResponse.builder()
                .id(id)
                .name(name)
                .description(description)
                .timestamp(LocalDateTime.now())
                .tags(tags)
                .properties(properties)
                .build();

        log.debug("커스터마이징 가능한 디버깅용 더미 응답 생성 완료: id={}", id);
        return ResponseEntity.ok(ApiResponse.successWithData(DebugSuccessCode.CUSTOM_DEBUG_RESPONSE_FETCHED, response));
    }
}