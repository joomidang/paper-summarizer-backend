package joomidang.papersummary.sample.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import joomidang.papersummary.sample.dto.SampleRequest;
import joomidang.papersummary.sample.dto.SampleResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RestDocs을 만들기 위한 참고용 Controller 기본적인 crud지원
 */
@RestController
@RequestMapping("/api/samples")
@Tag(name = "Sample", description = "샘플 리소스에 대한 CRUD API")
public class SampleController {

    private final Map<Long, SampleResponse> samples = new HashMap<>();
    private long nextId = 1;

    @Operation(
            summary = "모든 샘플 조회",
            description = "시스템에 등록된 모든 샘플 목록을 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "샘플 목록 조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = SampleResponse.class))
            )
    )
    @GetMapping
    public ResponseEntity<List<SampleResponse>> getAllSamples() {
        return ResponseEntity.ok(new ArrayList<>(samples.values()));
    }


    /*
    @param id
     */
    @Operation(
            summary = "특정 샘플 조회",
            description = "ID를 기준으로 특정 샘플의 상세 정보를 조회합니다."
    )
    @GetMapping("/{id}")
    public ResponseEntity<SampleResponse> getSampleById(@PathVariable Long id) {
        if (!samples.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(samples.get(id));
    }


    @Operation(
            summary = "샘플 생성",
            description = "새로운 샘플을 생성합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "생성할 샘플 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SampleRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "샘플 생성 요청 예시",
                                            value = "{\n" +
                                                    "  \"name\": \"\",\n" +
                                                    "  \"description\": \"\",\n" +
                                                    "  \"category\": \"\"\n" +
                                                    "}"
                                    )
                            }
                    )
            )
    )
    @PostMapping
    public ResponseEntity<SampleResponse> createSample(@RequestBody SampleRequest request) {
        long id = nextId++;
        SampleResponse response = new SampleResponse(id, request.getName(), request.getDescription(),
                request.getCategory());
        samples.put(id, response);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "샘플 수정",
            description = "기존 샘플의 정보를 수정합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수정할 샘플 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SampleRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "샘플 수정 요청 예시",
                                            value = "{\n" +
                                                    "  \"name\": \"\",\n" +
                                                    "  \"description\": \"\",\n" +
                                                    "  \"category\": \"\"\n" +
                                                    "}"
                                    )
                            }
                    )
            )
    )
    @PutMapping("/{id}")
    public ResponseEntity<SampleResponse> updateSample(@PathVariable Long id, @RequestBody SampleRequest request) {
        if (!samples.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        SampleResponse response = new SampleResponse(id, request.getName(), request.getDescription(),
                request.getCategory());
        samples.put(id, response);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "샘플 삭제",
            description = "특정 ID의 샘플을 시스템에서 삭제합니다."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSample(@PathVariable Long id) {
        if (!samples.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        samples.remove(id);
        return ResponseEntity.noContent().build();
    }
}