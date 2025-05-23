package joomidang.papersummary.member.service;

import joomidang.papersummary.member.controller.request.ProfileCreateRequest;
import joomidang.papersummary.member.entity.AuthProvider;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.entity.MemberInterest;
import joomidang.papersummary.member.entity.Role;
import joomidang.papersummary.member.exception.MemberDuplicateException;
import joomidang.papersummary.member.exception.MemberNotFoundException;
import joomidang.papersummary.member.repository.MemberInterestRepository;
import joomidang.papersummary.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberInterestRepository memberInterestRepository;

    @InjectMocks
    private MemberService memberService;

    private Member testMember;
    private ProfileCreateRequest validRequest;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 정보 생성
        testMember = Member.builder()
//                .id(1L)
                .email("test@example.com")
                .name("TestUser")
                .profileImage("https://example.com/image.jpg")
                .authProvider(AuthProvider.LOCAL)
                .providerUid("test-user-local")
                .role(Role.USER)
                .build();

        // 유효한 프로필 생성 요청
        validRequest = ProfileCreateRequest.builder()
//                .id(1L)
                .username("NewUsername")
                .profileImageUrl("https://example.com/new.jpg")
                .interests(Arrays.asList("AI", "Machine Learning"))
                .build();
    }

    @Test
    @DisplayName("프로필 생성 성공 테스트")
    void createProfileSuccess() {
        // Given
        when(memberRepository.findById(anyLong())).thenReturn(Optional.of(testMember));
        when(memberRepository.existsByNameAndId(anyString(), anyLong())).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // When
        Member result = memberService.createProfile(1L, validRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("NewUsername");
        assertThat(result.getProfileImage()).isEqualTo("https://example.com/new.jpg");

        // 메서드 호출 검증
        verify(memberRepository, times(1)).findById(1L);
        verify(memberInterestRepository, times(1)).deleteByMember(testMember);
        verify(memberInterestRepository, times(1)).saveAll(any());
        verify(memberRepository, times(1)).save(testMember);
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 프로필 생성 시 예외 발생")
    void createProfileNonExistentMember() {
        // Given
        when(memberRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> memberService.createProfile(1L, validRequest))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("새 회원 중복 닉네임 검증 실패 테스트")
    void checkNewUsernameDuplicateTest() {
        // Given
        doReturn(true).when(memberRepository).existsByName("DuplicateUsername");

        // When & Then
        assertThatThrownBy(() -> memberService.validateUsernameDuplicate("DuplicateUsername", null))
                .isInstanceOf(MemberDuplicateException.class)
                .hasMessage("이미 사용 중인 닉네임입니다.");
    }

    @Test
    @DisplayName("기존 회원 중복 닉네임 검증 실패 테스트")
    void checkExistingUsernameDuplicateTest() {
        // Given
        Long memberId = 1L;
        doReturn(true).when(memberRepository).existsByNameAndId("DuplicateUsername", memberId);

        // When & Then
        assertThatThrownBy(() -> memberService.validateUsernameDuplicate("DuplicateUsername", memberId))
                .isInstanceOf(MemberDuplicateException.class)
                .hasMessage("이미 사용 중인 닉네임입니다.");
    }


    @Test
    @DisplayName("관심분야 저장 테스트")
    void saveInterests() {
        // Given
        when(memberRepository.findById(anyLong())).thenReturn(Optional.of(testMember));
        when(memberRepository.existsByNameAndId(anyString(), anyLong())).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // 관심분야 저장 모의 객체 설정
        List<MemberInterest> savedInterests = Arrays.asList(
                MemberInterest.of(testMember, "AI"),
                MemberInterest.of(testMember, "ML")
        );
        when(memberInterestRepository.saveAll(any())).thenReturn(savedInterests);

        // When
        Member result = memberService.createProfile(1L, validRequest);

        // Then
        verify(memberInterestRepository, times(1)).deleteByMember(testMember);
        verify(memberInterestRepository, times(1)).saveAll(any());
    }
}