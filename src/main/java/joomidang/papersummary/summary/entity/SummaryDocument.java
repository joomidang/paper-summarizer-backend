package joomidang.papersummary.summary.entity;

import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "summary_documents")
@Getter
@Setter
@NoArgsConstructor
public class SummaryDocument {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private Long summaryId;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String brief;

    @Field(type = FieldType.Text)
    private String combinedText;

    @Field(type = FieldType.Integer)
    private Integer likeCount;

    @Field(type = FieldType.Integer)
    private Integer viewCount;

    @Field(type = FieldType.Date, format = {DateFormat.date_hour_minute_second, DateFormat.date})
    private LocalDateTime createdAt;

    @Field(type = FieldType.Dense_Vector, dims = 384) // Hugging Face 모델 차원 수
    private float[] embedding;

    @Builder
    public SummaryDocument(String id,
                           Long summaryId,
                           String title,
                           String brief,
                           String combinedText,
                           Integer likeCount,
                           Integer viewCount,
                           LocalDateTime createdAt,
                           float[] embedding) {
        this.id = id;
        this.summaryId = summaryId;
        this.title = title;
        this.brief = brief;
        this.combinedText = combinedText;
        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.embedding = embedding;
    }
}
