package joomidang.papersummary.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.member.controller.request.ProfileCreateRequest;
import joomidang.papersummary.member.controller.response.MemberSuccessCode;
import joomidang.papersummary.member.controller.response.ProfileResponse;
import joomidang.papersummary.member.entity.AuthProvider;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.entity.Role;
import joomidang.papersummary.member.exception.MemberDuplicateException;
import joomidang.papersummary.member.exception.MemberNotFoundException;
import joomidang.papersummary.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private MemberController memberController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private Member testMember;
    private ProfileCreateRequest validRequest;

    @BeforeEach
    void setUp() {
        // MockMvc 설정
        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setControllerAdvice(new MemberExceptionHandler())
                .build();

        // 테스트용 회원 정보 생성
        testMember = Member.builder()
                .id(1L)
                .email("test@example.com")
                .name("newUsername")
                .profileImage("https://example.com/image.jpg")
                .authProvider(AuthProvider.LOCAL)
                .providerUid("test-user-local")
                .role(Role.USER)
                .build();

        // 유효한 프로필 생성 요청
        validRequest = ProfileCreateRequest.builder()
                .id(1L)
                .username("newUsername")
                .profileImageUrl("https://example.com/image.jpg")
                .interests(Arrays.asList("AI", "Machine Learning"))
                .build();
    }

    @Test
    @DisplayName("프로필 생성 API 성공 테스트")
    void createProfileSuccessTest() throws Exception {
        // Given
        given(memberService.createProfile(anyLong(), any(ProfileCreateRequest.class))).willReturn(testMember);

        // When & Then
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(MemberSuccessCode.PROFILE_CREATED.getValue()))
                .andExpect(jsonPath("$.message").value(MemberSuccessCode.PROFILE_CREATED.getMessage()))
                .andExpect(jsonPath("$.data.id").value(testMember.getId()))
                .andExpect(jsonPath("$.data.username").value(testMember.getName()))
                .andExpect(jsonPath("$.data.profileImageUrl").value(testMember.getProfileImage()));
    }

    @Test
    @DisplayName("유효하지 않은 이름으로 프로필 생성 시 400 응답")
    void createProfileInvalidUserNameTest() throws Exception {
        // Given
        ProfileCreateRequest invalidRequest = ProfileCreateRequest.builder()
                .id(1L)
                .username("")  // 빈 문자열은 @NotBlank 조건에 위배됨
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
    @DisplayName("유효하지 않은 관심사으로 프로필 생성 시 400 응답")
    void createProfileInvalidInterestsTest() throws Exception {
        // Given
        ProfileCreateRequest invalidRequest = ProfileCreateRequest.builder()
                .id(1L)
                .username("TestUser")  // 빈 문자열은 @NotBlank 조건에 위배됨
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
    @DisplayName("존재하지 않는 회원으로 프로필 생성 시 404 응답")
    void createProfileNonExistentMemberTest() throws Exception {
        // Given
        doThrow(new MemberNotFoundException("사용자를 찾을 수 없습니다."))
                .when(memberService).createProfile(anyLong(), any(ProfileCreateRequest.class));

        // When & Then
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("중복된 닉네임으로 프로필 생성 시 409 응답")
    void createProfileDuplicateUsernameTest() throws Exception {
        // Given
        doThrow(new MemberDuplicateException("이미 사용 중인 닉네임입니다."))
                .when(memberService).createProfile(anyLong(), any(ProfileCreateRequest.class));

        // When & Then
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEM-0003"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));
    }
}