
package joomidang.papersummary.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import joomidang.papersummary.auth.security.JwtTokenProvider;
import joomidang.papersummary.member.controller.request.ProfileCreateRequest;
import joomidang.papersummary.member.controller.response.MemberSuccessCode;
import joomidang.papersummary.member.entity.AuthProvider;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.entity.Role;
import joomidang.papersummary.member.exception.MemberDuplicateException;
import joomidang.papersummary.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
    private Authentication authentication;

    @InjectMocks
    private MemberController memberController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private Member testMember;
    private ProfileCreateRequest validRequest;
    private final String TEST_USER_ID = "test-user-local";

    @BeforeEach
    void setUp() {
        // MockMvc 설정
        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setControllerAdvice(new MemberExceptionHandler())
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
        when(authentication.getName()).thenReturn(TEST_USER_ID);
        when(tokenProvider.getUserId(anyString())).thenReturn(TEST_USER_ID);
        when(memberService.findByProviderUid(TEST_USER_ID)).thenReturn(testMember);
        when(memberService.createProfile(anyLong(), any(ProfileCreateRequest.class))).thenReturn(testMember);

        // When & Then
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer fake-token")
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
        when(authentication.getName()).thenReturn(TEST_USER_ID);
        when(tokenProvider.getUserId(anyString())).thenReturn(TEST_USER_ID);
        when(memberService.findByProviderUid(TEST_USER_ID)).thenReturn(testMember);
        doThrow(new MemberDuplicateException("이미 사용 중인 닉네임입니다."))
                .when(memberService).createProfile(anyLong(), any(ProfileCreateRequest.class));

        // When & Then
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer fake-token")
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
                        .param("providerUid", providerUid)
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

}