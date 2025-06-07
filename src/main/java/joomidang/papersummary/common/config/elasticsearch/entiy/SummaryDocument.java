package joomidang.papersummary.common.config.elasticsearch.entiy;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "summary_documents")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class SummaryDocument {
    @Id
    private Long id;

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

    @Field(type = FieldType.Date, format = {DateFormat.date_hour_minute_second, DateFormat.date_time})
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;


    @Field(type = FieldType.Date, format = {DateFormat.date_hour_minute_second, DateFormat.date_time})
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("publishedAt")
    private LocalDateTime publishedAt;

    @Field(type = FieldType.Dense_Vector, dims = 384) // Hugging Face 모델 차원 수
    private float[] embedding;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Builder
    public SummaryDocument(Long id,
                           Long summaryId,
                           String title,
                           String brief,
                           String combinedText,
                           Integer likeCount,
                           Integer viewCount,
                           LocalDateTime createdAt,
                           LocalDateTime publishedAt,
                           float[] embedding,
                           List<String> tags) {
        this.id = id;
        this.summaryId = summaryId;
        this.title = title;
        this.brief = brief;
        this.combinedText = combinedText;
        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt; // 호환성을 위해 동일한 값 설정
        this.embedding = embedding;
        this.tags = tags;
    }
}
