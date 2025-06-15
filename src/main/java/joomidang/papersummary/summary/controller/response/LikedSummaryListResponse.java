package joomidang.papersummary.summary.controller.response;

import java.util.List;
import joomidang.papersummary.summary.entity.SummaryLike;
import org.springframework.data.domain.Page;

public record LikedSummaryListResponse(
        ContentWrapper content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public record ContentWrapper(List<LikedSummaryResponse> content) {}

    public static LikedSummaryListResponse from(Page<SummaryLike> summaryLikePage) {
        List<LikedSummaryResponse> summaries = summaryLikePage.getContent().stream()
                .map(LikedSummaryResponse::from)
                .toList();

        return new LikedSummaryListResponse(
                new ContentWrapper(summaries),
                summaryLikePage.getNumber() + 1,
                summaryLikePage.getSize(),
                summaryLikePage.getTotalElements(),
                summaryLikePage.getTotalPages()
        );
    }
}
