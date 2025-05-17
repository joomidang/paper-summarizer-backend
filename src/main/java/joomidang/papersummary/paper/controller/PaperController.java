package joomidang.papersummary.paper.controller;

import static joomidang.papersummary.paper.controller.response.PaperSuccessCode.UPLOAD_SUCCESS;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import joomidang.papersummary.auth.resolver.Authenticated;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.paper.controller.response.PaperResponse;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.service.PaperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Parent;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/papers")
@Tag(name = "Paper", description = "논문 분석 관련 API")
public class PaperController {

    private final PaperService paperService;

    // swagger 설정
    @Operation(summary = "논문 업로드", description = "논문 분석을 위한 파일 업로드")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "논문 업로드성공",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = PaperResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "업로드 성공",
                                            summary = "업로드 성공",
                                            value = "" +
                                                    "{\n" +
                                                    "  \"code\": \"PAS-0001\",\n" +
                                                    "  \"message\": \"논문이 성공적으로 업로드되었습니다.\",\n" +
                                                    "  \"data\": {\n" +
                                                    "    \"id\": 1,\n" +
                                                    "    \"title\": null,\n" +
                                                    "    \"filePath\": \"https://paper-dev-test-magic-pdf-output.s3.bucket.com/papers/88e808db-5f51-4aef-a413-339ae1840597_sample3.pdf\",\n" +
                                                    "    \"fileType\": \"application/pdf\",\n" +
                                                    "    \"fileSize\": 366982,\n" +
                                                    "    \"status\": \"PENDING\"\n" +
                                                    "  }\n" +
                                                    "}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "지원되지 않는 파일 형식",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "파일 형식 오류",
                                            summary = "지원되지 않는 파일 형식",
                                            value = "{\n" +
                                                    "  \"code\": \"PAE-0002\",\n" +
                                                    "  \"message\": \"지원하지 않는 파일 형식입니다. PDF 파일만 업로드 가능합니다.\",\n" +
                                                    "  \"data\": null\n" +
                                                    "}"
                                    )
                            }
                    )
            )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PaperResponse>> uploadPaper(
            @Authenticated @Parameter(hidden = true) String providerUid,
            @Parameter(
                    description = "파일 업로드",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart("file") MultipartFile file) {

        log.info("논문 업로드 요청 시작: fileName={}, fileSize={}, providerUid={}", 
                file.getOriginalFilename(), file.getSize(), providerUid);
        log.debug("논문 업로드 컨트롤러 진입: contentType={}", file.getContentType());

        try {
            log.debug("논문 업로드 서비스 호출 시작");
            Paper savedPaper = paperService.uploadPaper(file, providerUid);
            log.info("논문 업로드 완료: paperId={}, title={}", savedPaper.getId(), savedPaper.getTitle());

            return ResponseEntity.ok()
                    .body(ApiResponse.successWithData(UPLOAD_SUCCESS, PaperResponse.of(savedPaper)));
        } catch (Exception e) {
            log.error("논문 업로드 처리 실패: fileName={}, 오류={}", 
                    file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }
}
