package joomidang.papersummary.paper.controller.response;

import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.entity.Status;
import lombok.Getter;

@Getter
public class PaperResponse {
    private final Long id;
    private final String title;
    private final String filePath;
    private final String fileType;
    private final Long fileSize;
    private final Status status;

    public PaperResponse(Paper paper) {
        this.id = paper.getId();
        this.title = paper.getTitle();
        this.filePath = paper.getFilePath();
        this.fileType = paper.getFileType();
        this.fileSize = paper.getFileSize();
        this.status = paper.getStatus();
    }

    public static PaperResponse of(Paper savedPaper) {
        return new PaperResponse(savedPaper);
    }
}