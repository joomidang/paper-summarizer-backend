package joomidang.papersummary.member.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import joomidang.papersummary.member.controller.request.ProfileCreateRequest;
import joomidang.papersummary.member.controller.request.UpdateProfileRequest;
import joomidang.papersummary.member.controller.response.MemberSummaryResponse;
import joomidang.papersummary.member.entity.AuthProvider;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.entity.MemberInterest;
import joomidang.papersummary.member.exception.MemberDuplicateException;
import joomidang.papersummary.member.exception.MemberNotFoundException;
import joomidang.papersummary.member.repository.MemberInterestRepository;
import joomidang.papersummary.member.repository.MemberRepository;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원(Member) 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 회원 저장, 수정, 탈퇴, 조회(Provider 기반) 등의 주요 기능을 제공합니다.
 */
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class MemberService {
    /** 회원 엔티티에 대한 JPA 리포지토리 */
    private final MemberRepository memberRepository;
    private final MemberInterestRepository memberInterestRepository;
    private final SummaryRepository summaryRepository;

    /**
     * 회원 정보 저장
     *
     * @param member 저장할 회원 엔티티
     * @return 저장된 회원 객체
     */
    @Transactional
    public Member save(final Member member) {
        log.info("Save user: {}", member);
        return memberRepository.save(member);
    }

    /**
     * 회원 정보 저장 또는 수정(동일 Provider + ProviderUid가 존재하면 update)
     *
     * @param memberDetails 저장 또는 수정할 회원 정보
     * @return 저장된 회원 객체
     */
    @Transactional
    public Member saveOrUpdate(final Member memberDetails) {
        Optional<Member> existingMember = memberRepository.findByAuthProviderAndProviderUid(
                memberDetails.getAuthProvider(),
                memberDetails.getProviderUid()
        );

        if (existingMember.isPresent()) {
            // 기존 회원이면 프로필 및 로그인 시각만 갱신
            Member member = existingMember.get();
            member.updateProfile(
                    memberDetails.getName(),
                    memberDetails.getProfileImage()
            );
            member.updateLastLoginAt();
            log.info("제공자 ID로 기존 사용자 업데이트 중: {}", member.getProviderUid());
            return memberRepository.save(member);
        } else {
            // 없으면 새 회원으로 저장
            log.info("제공자 ID로 새로운 사용자 생성 중: {}", memberDetails.getProviderUid());
            return memberRepository.save(memberDetails);
        }
    }

    /**
     * 회원 탈퇴(논리 삭제)
     *
     * @param providerUid 탈퇴 처리할 회원의 프로바이더(외부 로그인) UID
     */
    @Transactional
    public void delete(final String providerUid) {
        log.info("사용자 탈퇴 처리 시작 : {}", providerUid);
        Member user = memberRepository.findByProviderUid(providerUid)
                .orElseThrow(() -> new MemberNotFoundException("사용자를 찾을 수 없습니다."));
        user.delete();
        memberRepository.save(user);
        log.info("사용자 탈퇴 처리 완료: {}", providerUid);
    }

    /**
     * 인증 제공자와 제공자 UID로 회원 조회
     *
     * @param provider 인증 제공자(LOCAL, KAKAO, GOOGLE, etc)
     * @param providerUid 제공자 별 회원 고유 식별자
     * @return 찾은 회원(Optional)
     */
    public Optional<Member> findByProviderAndProviderUid(AuthProvider provider, final String providerUid) {
        return memberRepository.findByAuthProviderAndProviderUid(provider, providerUid);
    }

    /**
     * 제공자 UID로 회원 단건 조회
     *
     * @param providerUid 제공자 별 회원 고유 식별자
     * @return 찾은 회원 엔티티(없으면 예외 발생)
     * @throws MemberNotFoundException 회원이 없을 때 발생
     */
    public Member findByProviderUid(final String providerUid) {
        return memberRepository.findByProviderUid(providerUid)
                .orElseThrow(() -> new MemberNotFoundException("사용자를 찾을 수 없습니다."));
    }

    /**
     * 회원 프로필 정보 생성
     *
     * @param memberId 프로필을 생성할 회원 ID
     * @param request 생성할 프로필 정보
     * @return 생성된 회원 객체
     * @throws MemberNotFoundException 회원이 없을 때 발생
     */
    @Transactional
    public Member createProfile(final Long memberId, final ProfileCreateRequest request) {
        log.info("프로필 생성 시작: memberId={}, username={}", memberId, request.getUsername());

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("사용자를 찾을 수 없습니다."));

        log.info("사용자 조회 성공: memberId={}, 현재 name={}", memberId, member.getName());

        // 닉네임 중복 검증 - 현재 사용자 ID와 요청된 닉네임으로 중복 체크
        checkUsernameDuplicate(request.getUsername(), memberId);

        // 프로필 생성
        member.createProfile(request.getUsername(), request.getProfileImageUrl());
        log.info("프로필 정보 설정 완료: name={}", member.getName());

        // 초기화
        memberInterestRepository.deleteByMember(member);

        // 새로운 관심분야 추가
        List<MemberInterest> interests = request.getInterests().stream()
                .map(interest -> MemberInterest.of(member, interest))
                .collect(Collectors.toList());

        memberInterestRepository.saveAll(interests);
        log.info("관심분야 저장 완료: count={}", interests.size());

        Member savedMember = memberRepository.save(member);
        log.info("프로필 생성 완료: memberId={}, name={}", savedMember.getId(), savedMember.getName());

        return savedMember;
    }

    private void checkUsernameDuplicate(String username, Long memberId) {
        log.info("닉네임 중복 검사: username={}, memberId={}", username, memberId);

        // 새 회원이거나 닉네임 변경 시 중복 체크
        if (memberId == null) {
            boolean exists = memberRepository.existsByName(username);
            log.info("새 회원 닉네임 중복 여부: {}", exists);
            if (exists) {
                throw new MemberDuplicateException("이미 사용 중인 닉네임입니다.");
            }
        } else {
            // 기존 회원이 닉네임 변경 시 (본인 제외 중복 체크)
            boolean exists = memberRepository.existsByNameAndId(username, memberId);
            log.info("기존 회원 닉네임 중복 여부: {}", exists);
            if (exists) {
                throw new MemberDuplicateException("이미 사용 중인 닉네임입니다.");
            }
        }
    }
    public void validateUsernameDuplicate(String username, Long memberId) {
        checkUsernameDuplicate(username, memberId);
    }

    /**
     * 회원 프로필 정보 수정
     *
     * @param providerUid 인증 제공자가 제공한 인증된 사용자의 고유 식별자
     * @param request 수정할 프로필 정보 (선택적 필드)
     * @return 수정된 회원 객체
     * @throws MemberNotFoundException 회원이 없을 때 발생
     */
    @Transactional
    public Member updateProfile(final String providerUid, final UpdateProfileRequest request) {
        log.info("프로필 수정 시작: providerUid={}", providerUid);

        // 회원 조회
        Member member = findByProviderUid(providerUid);
        log.info("사용자 조회 성공: memberId={}, 현재 name={}", member.getId(), member.getName());

        // 닉네임이 제공된 경우에만 업데이트 및 중복 검증
        if (request.getUsername() != null) {
            checkUsernameDuplicate(request.getUsername(), member.getId());
            log.info("닉네임 중복 검사 통과: username={}", request.getUsername());
        }

        // 프로필 정보 업데이트 (제공된 필드만)
        String newUsername = request.getUsername() != null ? request.getUsername() : member.getName();
        String newProfileImageUrl = request.getProfileImageUrl() != null ? request.getProfileImageUrl() : member.getProfileImage();
        member.updateProfile(newUsername, newProfileImageUrl);
        log.info("프로필 정보 업데이트: username={}, profileImageUrl={}", newUsername, newProfileImageUrl);

        // 관심분야가 제공된 경우에만 업데이트
        if (request.getInterests() != null) {
            // 기존 관심분야 삭제
            memberInterestRepository.deleteByMember(member);
            log.info("기존 관심분야 삭제 완료");

            // 새로운 관심분야 추가
            List<MemberInterest> interests = request.getInterests().stream()
                    .map(interest -> MemberInterest.of(member, interest))
                    .collect(Collectors.toList());

            memberInterestRepository.saveAll(interests);
            log.info("새 관심분야 저장 완료: count={}", interests.size());
        }

        Member savedMember = memberRepository.save(member);
        log.info("프로필 수정 완료: memberId={}, name={}", savedMember.getId(), savedMember.getName());

        return savedMember;
    }

    /**
     * 지정된 회원의 관심분야 목록을 조회
     *
     * @param memberId 관심분야를 조회할 회원의 고유 식별자
     * @return 회원의 관심분야를 나타내는 문자열 배열
     */
    @Transactional
    public String[] getInterests(final Long memberId) {
        log.debug("회원 관심사 조회 시작: memberId={}", memberId);
        List<MemberInterest> memberInterests = memberInterestRepository.findByMemberId(memberId);
        return memberInterests.stream()
                .map(MemberInterest::getInterest)
                .toArray(String[]::new);
    }

    /**
     * 회원이 작성한 요약 목록을 페이지네이션으로 조회
     *
     * @param memberId 요약을 조회할 회원의 고유 식별자
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이지네이션된 회원의 요약 목록
     */
    @Transactional
    public MemberSummaryResponse getSummaries(final Long memberId, int page, int size) {
        log.debug("회원 요약 목록 조회 시작: memberId={}, page={}, size={}", memberId, page, size);

        if (page > 0) page = page - 1;

        // 기본 정렬: 생성일 기준 내림차순 (최신순)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 회원의 요약 목록 조회
        Page<Summary> summaryPage = summaryRepository.findByMemberIdWithStats(memberId, pageable);

        // 각 요약의 통계 정보 초기화 (Lazy Loading 방지)
        summaryPage.getContent().forEach(summary -> {
            if (summary.getSummaryStats() == null) {
                summary.initializeSummaryStats();
            }
        });

        // 응답 객체 생성
        return MemberSummaryResponse.from(summaryPage);
    }

//    @Transactional
//    public MemberSummaryResponse getLikedSummaries(final Long memberId, int page, int size){
//        log.debug("좋아요한 요약 목록 조회: providerUid={}, page={}, size={}", providerUid, page, size);
//        if (page > 0) page = page - 1;
//
//        // 기본 정렬: 생성일 기준 내림차순 (최신순)
//        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
//        Page<Summary> likedSummaryPage = summaryRepository.findSummariesByMemberIdWithlikes(memberId, pageable);
//        return MemberSummaryResponse.from(likedSummaryPage);
//    }

}
