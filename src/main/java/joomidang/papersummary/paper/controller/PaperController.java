package joomidang.papersummary.paper.controller;

import static joomidang.papersummary.paper.controller.response.PaperSuccessCode.UPLOAD_SUCCESS;

import joomidang.papersummary.auth.resolver.Authenticated;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.paper.controller.response.PaperResponse;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.service.PaperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class PaperController {

    private final PaperService paperService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PaperResponse>> uploadPaper(
            @Authenticated String providerUid,
            @RequestPart("file") MultipartFile file) {

        log.info("논문 업로드 요청: {}", file.getOriginalFilename());

        Paper savedPaper = paperService.uploadPaper(file, providerUid);

        return ResponseEntity.ok()
                .body(ApiResponse.successWithData(UPLOAD_SUCCESS, PaperResponse.of(savedPaper)));
    }
}
