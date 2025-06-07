package joomidang.papersummary.tag.controller;

import java.util.List;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.tag.controller.response.TagResponse;
import joomidang.papersummary.tag.controller.response.TagSuccessCode;
import joomidang.papersummary.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getTags(
            @RequestParam(required = false, defaultValue = "5") int size) {
        log.info("인기 태그 목록 조회 요청: size={}", size);
        List<TagResponse> tags = tagService.getPopularTags(size);
        log.info("인기 태그 목록 조회 완료: ");
        return ResponseEntity.ok(ApiResponse.successWithData(TagSuccessCode.TAG_SUCCESS_CODE, tags));
    }
}

