package joomidang.papersummary.summary.controller.response;

import java.util.List;
import joomidang.papersummary.summary.entity.SummaryLike;
import org.springframework.data.domain.Page;

public record LikedSummaryListResponse(List<LikedSummaryResponse> summaries,
                                       int currentPage,
                                       int totalPages,
                                       long totalElements,
                                       boolean hasNext,
                                       boolean hasPrevious) {
    public static LikedSummaryListResponse from(Page<SummaryLike> summaryLikePage) {
        List<LikedSummaryResponse> summaries = summaryLikePage.getContent().stream()
                .map(LikedSummaryResponse::from)
                .toList();

        return new LikedSummaryListResponse(
                summaries,
                summaryLikePage.getNumber(),
                summaryLikePage.getTotalPages(),
                summaryLikePage.getTotalElements(),
                summaryLikePage.hasNext(),
                summaryLikePage.hasPrevious()
        );
    }
}
