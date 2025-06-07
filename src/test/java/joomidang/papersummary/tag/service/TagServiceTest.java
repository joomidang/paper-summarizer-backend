package joomidang.papersummary.tag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import joomidang.papersummary.member.entity.AuthProvider;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.entity.Role;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.entity.Status;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.tag.entity.SummaryTag;
import joomidang.papersummary.tag.entity.Tag;
import joomidang.papersummary.tag.repository.SummaryTagRepository;
import joomidang.papersummary.tag.repository.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagService 단위 테스트")
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private SummaryTagRepository summaryTagRepository;

    @InjectMocks
    private TagService tagService;

    @Nested
    @DisplayName("attachTagsToSummary 메서드는")
    class AttachTagsToSummaryTest {

        @Test
        @DisplayName("정상적인 태그 목록을 받으면 태그들을 요약본에 연결한다")
        void shouldAttachValidTagsToSummary() {
            // given
            Summary summary = createTestSummary();
            List<String> tagNames = Arrays.asList("Java", "Spring", "JPA");

            Tag javaTag = createTestTag(1L, "java", 5);
            Tag springTag = createTestTag(2L, "spring", 3);
            Tag jpaTag = createTestTag(3L, "jpa", 1);

            given(summaryTagRepository.existsBySummaryAndTag(summary, javaTag)).willReturn(false);
            given(summaryTagRepository.existsBySummaryAndTag(summary, springTag)).willReturn(false);
            given(summaryTagRepository.existsBySummaryAndTag(summary, jpaTag)).willReturn(false);

            given(tagRepository.findByName("java")).willReturn(Optional.of(javaTag));
            given(tagRepository.findByName("spring")).willReturn(Optional.of(springTag));
            given(tagRepository.findByName("jpa")).willReturn(Optional.of(jpaTag));

            // when
            tagService.attachTagsToSummary(summary, tagNames);

            // then
            then(summaryTagRepository).should().deleteBySummary(summary);
            then(summaryTagRepository).should(times(3)).save(any(SummaryTag.class));
            then(tagRepository).should(times(3)).save(any(Tag.class));

            // 사용 횟수 증가 확인
            assertThat(javaTag.getUsageCount()).isEqualTo(6);
            assertThat(springTag.getUsageCount()).isEqualTo(4);
            assertThat(jpaTag.getUsageCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("존재하지 않는 태그는 새로 생성하여 연결한다")
        void shouldCreateNewTagIfNotExists() {
            // given
            Summary summary = createTestSummary();
            List<String> tagNames = Arrays.asList("NewTag");

            Tag newTag = createTestTag(1L, "newtag", 0);

            given(tagRepository.findByName("newtag")).willReturn(Optional.empty());
            given(tagRepository.save(any(Tag.class))).willReturn(newTag);
            given(summaryTagRepository.existsBySummaryAndTag(summary, newTag)).willReturn(false);

            // when
            tagService.attachTagsToSummary(summary, tagNames);

            // then
            then(summaryTagRepository).should().deleteBySummary(summary);
            then(summaryTagRepository).should().save(any(SummaryTag.class)); // 관계 생성
            then(tagRepository).should(times(2)).save(any(Tag.class)); // 1) 새 태그 생성 2) 사용횟수 증가

            // 사용 횟수 증가 확인
            assertThat(newTag.getUsageCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("유효하지 않은 태그명은 무시한다")
        void shouldIgnoreInvalidTagNames() {
            // given
            Summary summary = createTestSummary();
            List<String> tagNames = Arrays.asList("", "  ", "VeryLongTagNameThatExceedsThirtyCharacterLimit",
                    "Invalid@Tag");

            // when
            tagService.attachTagsToSummary(summary, tagNames);

            // then
            then(summaryTagRepository).should().deleteBySummary(summary);
            then(tagRepository).should(never()).findByName(anyString());
            then(summaryTagRepository).should(never()).save(any(SummaryTag.class));
        }

        @Test
        @DisplayName("태그 목록이 null이거나 비어있으면 아무것도 하지 않는다")
        void shouldDoNothingWhenTagListIsNullOrEmpty() {
            // given
            Summary summary = createTestSummary();

            // when - null 케이스
            tagService.attachTagsToSummary(summary, null);

            // then
            then(summaryTagRepository).should(never()).deleteBySummary(any());
            then(tagRepository).should(never()).findByName(anyString());

            // when - empty 케이스
            tagService.attachTagsToSummary(summary, Collections.emptyList());

            // then
            then(summaryTagRepository).should(never()).deleteBySummary(any());
            then(tagRepository).should(never()).findByName(anyString());
        }

        @Test
        @DisplayName("이미 연결된 태그는 중복 연결하지 않는다")
        void shouldNotDuplicateExistingTagConnection() {
            // given
            Summary summary = createTestSummary();
            List<String> tagNames = Arrays.asList("Java");

            Tag javaTag = createTestTag(1L, "java", 5);

            given(tagRepository.findByName("java")).willReturn(Optional.of(javaTag));
            given(summaryTagRepository.existsBySummaryAndTag(summary, javaTag)).willReturn(true);

            // when
            tagService.attachTagsToSummary(summary, tagNames);

            // then
            then(summaryTagRepository).should().deleteBySummary(summary);
            then(summaryTagRepository).should(never()).save(any(SummaryTag.class));
            then(tagRepository).should(never()).save(javaTag); // 사용횟수 증가 안함
        }

        @Test
        @DisplayName("태그명 정규화를 적용하여 연결한다")
        void shouldNormalizeTagNamesBeforeAttaching() {
            // given
            Summary summary = createTestSummary();
            List<String> tagNames = Arrays.asList("  JAVA  ", "Spring Boot", "jpa");

            Tag javaTag = createTestTag(1L, "java", 0);
            Tag springBootTag = createTestTag(2L, "spring boot", 0);
            Tag jpaTag = createTestTag(3L, "jpa", 0);

            given(tagRepository.findByName("java")).willReturn(Optional.of(javaTag));
            given(tagRepository.findByName("spring boot")).willReturn(Optional.of(springBootTag));
            given(tagRepository.findByName("jpa")).willReturn(Optional.of(jpaTag));

            given(summaryTagRepository.existsBySummaryAndTag(any(), any())).willReturn(false);

            // when
            tagService.attachTagsToSummary(summary, tagNames);

            // then
            then(tagRepository).should().findByName("java");
            then(tagRepository).should().findByName("spring boot");
            then(tagRepository).should().findByName("jpa");
        }
    }

    @Nested
    @DisplayName("getTagNamesBySummary 메서드는")
    class GetTagNamesBySummaryTest {

        @Test
        @DisplayName("요약본의 태그 목록을 정상적으로 조회한다")
        void shouldReturnTagNamesForSummary() {
            // given
            Long summaryId = 1L;

            Tag tag1 = createTestTag(1L, "java", 5);
            Tag tag2 = createTestTag(2L, "spring", 3);

            SummaryTag summaryTag1 = createTestSummaryTag(1L, null, tag1);
            SummaryTag summaryTag2 = createTestSummaryTag(2L, null, tag2);

            List<SummaryTag> summaryTags = Arrays.asList(summaryTag1, summaryTag2);

            given(summaryTagRepository.findBySummaryIdWithTag(summaryId)).willReturn(summaryTags);

            // when
            List<String> result = tagService.getTagNamesBySummary(summaryId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly("java", "spring");
            then(summaryTagRepository).should().findBySummaryIdWithTag(summaryId);
        }

        @Test
        @DisplayName("태그가 없는 요약본은 빈 목록을 반환한다")
        void shouldReturnEmptyListWhenNoTagsFound() {
            // given
            Long summaryId = 1L;

            given(summaryTagRepository.findBySummaryIdWithTag(summaryId)).willReturn(Collections.emptyList());

            // when
            List<String> result = tagService.getTagNamesBySummary(summaryId);

            // then
            assertThat(result).isEmpty();
            then(summaryTagRepository).should().findBySummaryIdWithTag(summaryId);
        }
    }

    @Nested
    @DisplayName("decreaseTagUsageForSummary 메서드는")
    class DecreaseTagUsageForSummaryTest {

        @Test
        @DisplayName("요약본 삭제 시 연관된 태그들의 사용 횟수를 감소시킨다")
        void shouldDecreaseTagUsageCountWhenSummaryDeleted() {
            // given
            Summary summary = createTestSummary();

            Tag tag1 = createTestTag(1L, "java", 5);
            Tag tag2 = createTestTag(2L, "spring", 3);

            SummaryTag summaryTag1 = createTestSummaryTag(1L, summary, tag1);
            SummaryTag summaryTag2 = createTestSummaryTag(2L, summary, tag2);

            List<SummaryTag> summaryTags = Arrays.asList(summaryTag1, summaryTag2);

            given(summaryTagRepository.findBySummaryIdWithTag(summary.getId())).willReturn(summaryTags);

            // when
            tagService.decreaseTagUsageForSummary(summary);

            // then
            assertThat(tag1.getUsageCount()).isEqualTo(4);
            assertThat(tag2.getUsageCount()).isEqualTo(2);

            then(tagRepository).should(times(2)).save(any(Tag.class));
            then(summaryTagRepository).should().deleteBySummary(summary);
        }

        @Test
        @DisplayName("사용 횟수가 0인 태그는 0 이하로 감소하지 않는다")
        void shouldNotDecreaseUsageCountBelowZero() {
            // given
            Summary summary = createTestSummary();

            Tag tag = createTestTag(1L, "java", 0); // 이미 0인 태그
            SummaryTag summaryTag = createTestSummaryTag(1L, summary, tag);

            List<SummaryTag> summaryTags = Arrays.asList(summaryTag);

            given(summaryTagRepository.findBySummaryIdWithTag(summary.getId())).willReturn(summaryTags);

            // when
            tagService.decreaseTagUsageForSummary(summary);

            // then
            assertThat(tag.getUsageCount()).isEqualTo(0); // 여전히 0
            then(tagRepository).should().save(tag);
            then(summaryTagRepository).should().deleteBySummary(summary);
        }

        @Test
        @DisplayName("연관된 태그가 없는 요약본은 아무것도 하지 않는다")
        void shouldDoNothingWhenNoTagsAssociated() {
            // given
            Summary summary = createTestSummary();

            given(summaryTagRepository.findBySummaryIdWithTag(summary.getId())).willReturn(Collections.emptyList());

            // when
            tagService.decreaseTagUsageForSummary(summary);

            // then
            then(tagRepository).should(never()).save(any(Tag.class));
            then(summaryTagRepository).should().deleteBySummary(summary);
        }
    }

    @Nested
    @DisplayName("getSummariesByTag 메서드는")
    class GetSummariesByTagTest {

        @Test
        @DisplayName("태그명으로 요약본 목록을 정상적으로 조회한다")
        void shouldReturnSummariesByTagName() {
            // given
            String tagName = "java";
            String normalizedTagName = "java";
            Pageable pageable = PageRequest.of(0, 10);

            Summary summary1 = createTestSummary();
            Summary summary2 = Summary.builder()
                    .id(2L)
                    .title("Another Test Summary")
                    .brief("Another test brief")
                    .s3KeyMd("test/summary2.md")
                    .publishStatus(PublishStatus.PUBLISHED)
                    .member(summary1.getMember())
                    .paper(summary1.getPaper())
                    .build();

            List<Summary> summaries = Arrays.asList(summary1, summary2);
            Page<Summary> summariesPage = new PageImpl<>(summaries, pageable, 2L);

            given(summaryTagRepository.findSummariesByTagName(
                    eq(normalizedTagName), eq(PublishStatus.PUBLISHED), eq(pageable)))
                    .willReturn(summariesPage);

            // when
            Page<Summary> result = tagService.getSummariesByTag(tagName, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).containsExactly(summary1, summary2);
            assertThat(result.getTotalElements()).isEqualTo(2L);

            then(summaryTagRepository).should().findSummariesByTagName(
                    eq(normalizedTagName), eq(PublishStatus.PUBLISHED), eq(pageable));
        }

        @Test
        @DisplayName("태그명을 정규화하여 조회한다")
        void shouldNormalizeTagNameBeforeQuery() {
            // given
            String tagName = "  JAVA  ";
            String normalizedTagName = "java";
            Pageable pageable = PageRequest.of(0, 10);

            List<Summary> emptyList = Collections.emptyList();
            Page<Summary> emptyPage = new PageImpl<>(emptyList, pageable, 0L);

            given(summaryTagRepository.findSummariesByTagName(
                    eq(normalizedTagName), eq(PublishStatus.PUBLISHED), eq(pageable)))
                    .willReturn(emptyPage);

            // when
            Page<Summary> result = tagService.getSummariesByTag(tagName, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();

            then(summaryTagRepository).should().findSummariesByTagName(
                    eq(normalizedTagName), eq(PublishStatus.PUBLISHED), eq(pageable));
        }

        @Test
        @DisplayName("태그에 해당하는 요약본이 없으면 빈 페이지를 반환한다")
        void shouldReturnEmptyPageWhenNoSummariesFound() {
            // given
            String tagName = "nonexistent";
            String normalizedTagName = "nonexistent";
            Pageable pageable = PageRequest.of(0, 10);

            List<Summary> emptyList = Collections.emptyList();
            Page<Summary> emptyPage = new PageImpl<>(emptyList, pageable, 0L);

            given(summaryTagRepository.findSummariesByTagName(
                    eq(normalizedTagName), eq(PublishStatus.PUBLISHED), eq(pageable)))
                    .willReturn(emptyPage);

            // when
            Page<Summary> result = tagService.getSummariesByTag(tagName, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();

            then(summaryTagRepository).should().findSummariesByTagName(
                    eq(normalizedTagName), eq(PublishStatus.PUBLISHED), eq(pageable));
        }
    }

    // 테스트 헬퍼 메서드들
    private Summary createTestSummary() {
        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .name("testUser")
                .authProvider(AuthProvider.LOCAL)
                .providerUid("test-provider-uid")
                .role(Role.USER)
                .build();

        Paper paper = Paper.builder()
                .id(1L)
                .title("Test Paper")
                .filePath("test/path")
                .fileType("application/pdf")
                .fileSize(1000L)
                .status(Status.ANALYZED)
                .member(member)
                .build();

        return Summary.builder()
                .id(1L)
                .title("Test Summary")
                .brief("Test brief")
                .s3KeyMd("test/summary.md")
                .publishStatus(PublishStatus.PUBLISHED)
                .member(member)
                .paper(paper)
                .build();
    }

    private Tag createTestTag(Long id, String name, Integer usageCount) {
        return Tag.builder()
                .id(id)
                .name(name)
                .usageCount(usageCount)
                .build();
    }

    private SummaryTag createTestSummaryTag(Long id, Summary summary, Tag tag) {
        return SummaryTag.builder()
                .id(id)
                .summary(summary)
                .tag(tag)
                .build();
    }
}
