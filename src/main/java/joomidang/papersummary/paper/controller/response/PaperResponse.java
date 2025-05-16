package joomidang.papersummary.paper.controller.response;

import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.entity.Status;
import lombok.Getter;

@Getter
public class PaperResponse {
    private final Long id;
    private final String title;
    private final String fileType;
    private final Status status;

    public PaperResponse(Paper paper) {
        this.id = paper.getId();
        this.title = paper.getTitle();
        this.fileType = paper.getFileType();
        this.status = paper.getStatus();
    }

    public static PaperResponse of(Paper savedPaper) {
        return new PaperResponse(savedPaper);
    }
}