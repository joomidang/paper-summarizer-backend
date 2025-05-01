package joomidang.papersummary.paper.controller.request;

import java.util.List;

public record ParsingResultRequest(
        String title,
        String markdownUrl,
        List<String> figure,
        List<String> table
) {
}
