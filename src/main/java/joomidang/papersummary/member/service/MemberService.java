package joomidang.papersummary.member.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import joomidang.papersummary.member.controller.request.ProfileCreateRequest;
import joomidang.papersummary.member.entity.AuthProvider;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.entity.MemberInterest;
import joomidang.papersummary.member.exception.MemberDuplicateException;
import joomidang.papersummary.member.exception.MemberNotFoundException;
import joomidang.papersummary.member.repository.MemberInterestRepository;
import joomidang.papersummary.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            log.info("기존 회원 닉네임 중복 여부(본인 제외): {}", exists);
            if (exists) {
                throw new MemberDuplicateException("이미 사용 중인 닉네임입니다.");
            }
        }
    }
}
