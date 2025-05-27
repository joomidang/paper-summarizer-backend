package joomidang.papersummary.summary.controller.response;

import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public record PopularSummaryListResponse(
        List<PopularSummaryResponse> summaries,
        int currentPage,
        int totalPages,
        long totalElements,
        boolean hasNext,
        boolean hasPrevious
) {
    public static PopularSummaryListResponse from(Page<PopularSummaryResponse> summariesPage) {
        return new PopularSummaryListResponse(
                summariesPage.getContent(),
                summariesPage.getNumber(),
                summariesPage.getTotalPages(),
                summariesPage.getTotalElements(),
                summariesPage.hasNext(),
                summariesPage.hasPrevious()
        );
    }

    public static PopularSummaryListResponse empty(Pageable pageable) {
        Page<PopularSummaryResponse> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                pageable,
                0
        );
        return PopularSummaryListResponse.from(emptyPage);
    }
}
