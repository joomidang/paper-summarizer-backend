package joomidang.papersummary.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import joomidang.papersummary.member.controller.request.ProfileCreateRequest;
import joomidang.papersummary.member.controller.request.UpdateProfileRequest;
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
        when(memberRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);
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
        doReturn(true).when(memberRepository).existsByNameAndIdNot("DuplicateUsername", memberId);

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
        when(memberRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);
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

    @Test
    @DisplayName("사용자 관심사 조회 - 성공")
    void getInterests_Success() {
        // given
        Long memberId = 1L;

        List<MemberInterest> memberInterests = Arrays.asList(
                MemberInterest.of(testMember, "AI"),
                MemberInterest.of(testMember, "Machine Learning"),
                MemberInterest.of(testMember, "Data Science")
        );

        given(memberInterestRepository.findByMemberId(memberId)).willReturn(memberInterests);

        // when
        String[] result = memberService.getInterests(memberId);

        // then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("AI", "Machine Learning", "Data Science");
    }

    @Test
    @DisplayName("프로필 수정 - 닉네임은 그대로 두고 다른 정보만 수정 성공")
    void updateProfile_KeepUsernameUpdateOtherInfo() {
        // Given
        String providerUid = "test-user-local";
        String originalUsername = "TestUser";
        String originalProfileImage = "https://example.com/image.jpg";
        String newProfileImage = "https://example.com/new-image.jpg";
        List<String> newInterests = Arrays.asList("Data Science", "NLP");

        // 닉네임은 null로 설정하여 변경하지 않음
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .username(originalUsername)
                .profileImageUrl(newProfileImage)
                .interests(newInterests)
                .build();

        // 회원 조회 모의 설정
        when(memberRepository.findByProviderUid(providerUid)).thenReturn(Optional.of(testMember));
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // 관심분야 저장 모의 설정
        List<MemberInterest> savedInterests = Arrays.asList(
                MemberInterest.of(testMember, "Data Science"),
                MemberInterest.of(testMember, "NLP")
        );
        when(memberInterestRepository.saveAll(any())).thenReturn(savedInterests);

        // When
        Member result = memberService.updateProfile(providerUid, request);

        // Then
        // 닉네임은 변경되지 않았는지 확인
        assertThat(result.getName()).isEqualTo(originalUsername);
        // 프로필 이미지는 변경되었는지 확인
        assertThat(result.getProfileImage()).isEqualTo(newProfileImage);

        // 메서드 호출 검증
        verify(memberRepository, times(1)).findByProviderUid(providerUid);
        verify(memberInterestRepository, times(1)).deleteByMember(testMember);
        verify(memberInterestRepository, times(1)).saveAll(any());
        verify(memberRepository, times(1)).save(testMember);
    }
}
