
package joomidang.papersummary.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import joomidang.papersummary.auth.resolver.Authenticated;
import joomidang.papersummary.auth.security.JwtTokenProvider;
import joomidang.papersummary.member.controller.request.ProfileCreateRequest;
import joomidang.papersummary.member.controller.response.MemberSuccessCode;
import joomidang.papersummary.member.entity.AuthProvider;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.entity.Role;
import joomidang.papersummary.member.exception.MemberDuplicateException;
import joomidang.papersummary.member.service.MemberService;
import joomidang.papersummary.summary.controller.response.AuthorResponse;
import joomidang.papersummary.summary.controller.response.LikedSummaryListResponse;
import joomidang.papersummary.summary.controller.response.LikedSummaryResponse;
import joomidang.papersummary.summary.service.SummaryLikeService;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MemberService memberService;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private SummaryLikeService summaryLikeService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private MemberController memberController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private Member testMember;
    private ProfileCreateRequest validRequest;
    private final String TEST_USER_ID = "test-user-local";
    private final String TEST_TOKEN = "Bearer fake-token";
    private final int PAGE = 0;
    private final int SIZE = 10;

    /**
     * 테스트용 인증 어노테이션 리졸버 X-AUTH-ID 헤더에서 사용자 ID를 추출하여 @Authenticated 어노테이션이 붙은 파라미터에 주입
     */
    static class TestAuthenticatedArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(Authenticated.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            return webRequest.getHeader("X-AUTH-ID");
        }
    }

    @BeforeEach
    void setUp() {
        // MockMvc 설정
        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setControllerAdvice(new MemberExceptionHandler())
                .setCustomArgumentResolvers(new TestAuthenticatedArgumentResolver())
                .build();

        // 테스트용 회원 정보 생성
        testMember = Member.builder()
                .id(4L)
                .email("test@example.com")
                .name("Username")
                .profileImage("https://example.com/image.jpg")
                .authProvider(AuthProvider.LOCAL)
                .providerUid(TEST_USER_ID)
                .role(Role.USER)
                .build();

        // 유효한 프로필 생성 요청
        validRequest = ProfileCreateRequest.builder()
//                .id(4L)
                .username("newUsername")
                .profileImageUrl("https://example.com/image.jpg")
                .interests(Arrays.asList("AI", "Machine Learning"))
                .build();
    }

    @Test
    @DisplayName("프로필 생성 API 성공 테스트")
    void createProfileSuccessTest() throws Exception {
        // Given
        when(memberService.findByProviderUid(TEST_USER_ID)).thenReturn(testMember);
        when(memberService.createProfile(anyLong(), any(ProfileCreateRequest.class))).thenReturn(testMember);

        // When & Then
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer fake-token")
                        .header("X-AUTH-ID", TEST_USER_ID)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(MemberSuccessCode.PROFILE_CREATED.getValue()))
                .andExpect(jsonPath("$.message").value(MemberSuccessCode.PROFILE_CREATED.getMessage()))
                .andExpect(jsonPath("$.data.id").value(testMember.getId()))
                .andExpect(jsonPath("$.data.username").value(testMember.getName()))
                .andExpect(jsonPath("$.data.profileImageUrl").value(testMember.getProfileImage()));
    }

    @Test
    @DisplayName("빈 이름으로 프로필 생성 시 400 응답")
    void createProfileInvalidUserNameTest() throws Exception {
        // Given
        ProfileCreateRequest invalidRequest = ProfileCreateRequest.builder()
//                .id(1L)
                .username("")
                .profileImageUrl("https://example.com/image.jpg")
                .interests(Arrays.asList("AI", "ML"))
                .build();

        // When & Then
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEM-0002"));
    }

    @Test
    @DisplayName("빈 관심사으로 프로필 생성 시 400 응답")
    void createProfileInvalidInterestsTest() throws Exception {
        // Given
        ProfileCreateRequest invalidRequest = ProfileCreateRequest.builder()
//                .id(1L)
                .username("TestUser")
                .profileImageUrl("https://example.com/image.jpg")
                .interests(Collections.emptyList())  // 빈 리스트는 @NotEmpty 조건에 위배됨
                .build();

        // When & Then
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEM-0002"));
    }

    @Test
    @DisplayName("중복된 닉네임으로 프로필 생성 시 409 응답")
    void createProfileDuplicateUsernameTest() throws Exception {
        // Given
        when(memberService.findByProviderUid(TEST_USER_ID)).thenReturn(testMember);
        doThrow(new MemberDuplicateException("이미 사용 중인 닉네임입니다."))
                .when(memberService).createProfile(anyLong(), any(ProfileCreateRequest.class));

        // When & Then
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer fake-token")
                        .header("X-AUTH-ID", TEST_USER_ID)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .principal(authentication))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEM-0003"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));
    }

    @Test
    @DisplayName("사용자 관심사 조회 API 성공 테스트")
    void getInterestsSuccessTest() throws Exception {
        // given
        String providerUid = "test-user-local";
        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .name("testUser")
                .authProvider(AuthProvider.LOCAL)
                .providerUid(providerUid)
                .role(Role.USER)
                .build();

        String[] interests = {"AI", "Machine Learning", "Data Science"};

        when(memberService.findByProviderUid(providerUid)).thenReturn(member);
        when(memberService.getInterests(1L)).thenReturn(interests);

        // when & then
        mockMvc.perform(get("/api/users/me/interests")
                        .header("X-AUTH-ID", providerUid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEM-0002"))
                .andExpect(jsonPath("$.message").value("유저 정보 조회"))
                .andExpect(jsonPath("$.data.interests").isArray())
                .andExpect(jsonPath("$.data.interests.length()").value(3))
                .andExpect(jsonPath("$.data.interests[0]").value("AI"))
                .andExpect(jsonPath("$.data.interests[1]").value("Machine Learning"))
                .andExpect(jsonPath("$.data.interests[2]").value("Data Science"));
    }

    @Test
    @DisplayName("사용자 댓글 목록 조회 API 성공 테스트")
    void getCommentsSuccessTest() throws Exception {
        when(memberService.findByProviderUid(any())).thenReturn(testMember);

        mockMvc.perform(get("/api/users/me/comments")
                        .param("page", String.valueOf(PAGE))
                        .param("size", String.valueOf(SIZE))
                        .header("Authorization", TEST_TOKEN)
                        .header("X-AUTH-ID", TEST_USER_ID)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(MemberSuccessCode.MEMBER_COMMENTS.getValue()))
                .andExpect(jsonPath("$.message").value(MemberSuccessCode.MEMBER_COMMENTS.getMessage()));
    }

    @Test
    @DisplayName("사용자 좋아요한 요약 목록 조회 API 성공 테스트")
    void getLikedSummariesSuccessTest() throws Exception {
        // Given
        List<LikedSummaryResponse> summaries = new ArrayList<>();

        // Create author responses
        AuthorResponse author1 = AuthorResponse.builder()
                .id(42L)
                .username("작성자1")
                .profileImageUrl("https://example.com/profiles/42.png")
                .build();

        AuthorResponse author2 = AuthorResponse.builder()
                .id(55L)
                .username("작성자2")
                .profileImageUrl("https://example.com/profiles/55.png")
                .build();

        // Create tags
        List<String> tags = Arrays.asList("GPT", "ML");

        // Create summary responses
        summaries.add(new LikedSummaryResponse(
                1L, "첫 번째 요약본", author1,
                LocalDateTime.now(), LocalDateTime.now(), 5, tags
        ));
        summaries.add(new LikedSummaryResponse(
                2L, "두 번째 요약본", author2,
                LocalDateTime.now(), LocalDateTime.now(), 8, tags
        ));

        LikedSummaryListResponse mockResponse = new LikedSummaryListResponse(
                new LikedSummaryListResponse.ContentWrapper(summaries), 
                1, 10, 2L, 1);

        when(summaryLikeService.getLikedSummaries(any(), any(Pageable.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/users/me/likes")
                        .param("page", "1")
                        .param("size", "10")
                        .header("Authorization", TEST_TOKEN)
                        .header("X-AUTH-ID", TEST_USER_ID)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(MemberSuccessCode.MEMBER_LIKED_SUMMARIES.getValue()))
                .andExpect(jsonPath("$.message").value(MemberSuccessCode.MEMBER_LIKED_SUMMARIES.getMessage()))
                .andExpect(jsonPath("$.data.content.content").isArray())
                .andExpect(jsonPath("$.data.content.content.length()").value(2))
                .andExpect(jsonPath("$.data.content.content[0].summaryId").value(1))
                .andExpect(jsonPath("$.data.content.content[0].title").value("첫 번째 요약본"))
                .andExpect(jsonPath("$.data.content.content[0].author.id").value(42))
                .andExpect(jsonPath("$.data.content.content[0].author.username").value("작성자1"))
                .andExpect(jsonPath("$.data.content.content[1].summaryId").value(2))
                .andExpect(jsonPath("$.data.content.content[1].title").value("두 번째 요약본"))
                .andExpect(jsonPath("$.data.content.content[1].author.id").value(55))
                .andExpect(jsonPath("$.data.content.content[1].author.username").value("작성자2"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @DisplayName("사용자 좋아요한 요약 목록 페이징 테스트")
    void getLikedSummariesPagingTest() throws Exception {
        // Given
        // 좋아요한 요약 목록 응답 생성 (페이지 2, 총 25개 항목)
        List<LikedSummaryResponse> summaries = new ArrayList<>();

        // Create tags
        List<String> tags = Arrays.asList("GPT", "ML");

        for (int i = 11; i <= 20; i++) {
            // Create author response for each summary
            AuthorResponse author = AuthorResponse.builder()
                    .id((long) i)
                    .username("작성자 " + i)
                    .profileImageUrl("https://example.com/profiles/" + i + ".png")
                    .build();

            summaries.add(new LikedSummaryResponse(
                    (long) i, "요약본 " + i, author,
                    LocalDateTime.now(), LocalDateTime.now(), 5, tags
            ));
        }

        LikedSummaryListResponse mockResponse = new LikedSummaryListResponse(
                new LikedSummaryListResponse.ContentWrapper(summaries), 
                2, 10, 25L, 3
        );

        when(summaryLikeService.getLikedSummaries(any(), any(Pageable.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/users/me/likes")
                        .param("page", "2")
                        .param("size", "10")
                        .header("Authorization", TEST_TOKEN)
                        .header("X-AUTH-ID", TEST_USER_ID)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(MemberSuccessCode.MEMBER_LIKED_SUMMARIES.getValue()))
                .andExpect(jsonPath("$.message").value(MemberSuccessCode.MEMBER_LIKED_SUMMARIES.getMessage()))
                .andExpect(jsonPath("$.data.content.content").isArray())
                .andExpect(jsonPath("$.data.content.content.length()").value(10))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.totalElements").value(25))
                .andExpect(jsonPath("$.data.size").value(10));
    }
}
