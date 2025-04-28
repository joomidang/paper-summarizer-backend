package joomidang.papersummary.users.service;

import java.util.Optional;
import joomidang.papersummary.users.entity.AuthProvider;
import joomidang.papersummary.users.entity.Member;
import joomidang.papersummary.users.exception.MemberNotFoundException;
import joomidang.papersummary.users.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class MemberService {
    private final MemberRepository memberRepository;

    @Transactional
    public Member save(final Member member) {
        log.info("Save user: {}", member);
        return memberRepository.save(member);
    }

    @Transactional
    public Member saveOrUpdate(final Member memberDetails) {
        Optional<Member> existingMember = memberRepository.findByAuthProviderAndProviderUid(
                memberDetails.getAuthProvider(),
                memberDetails.getProviderUid()
        );

        if (existingMember.isPresent()) {
            Member member = existingMember.get();
            member.updateProfile(
                    memberDetails.getName(),
                    memberDetails.getEmail(),
                    memberDetails.getProfileImage()
            );
            member.updateLastLoginAt();
            log.info("제공자 ID로 기존 사용자 업데이트 중: {}", member.getProviderUid());
            return memberRepository.save(member);
        } else {
            log.info("제공자 ID로 새로운 사용자 생성 중: {}", memberDetails.getProviderUid());
            return memberRepository.save(memberDetails);
        }
    }


    @Transactional
    public void delete(final String providerUid) {
        log.info("사용자 탈퇴 처리 시작 : {}", providerUid);
        Member user = memberRepository.findByProviderUid(providerUid)
                .orElseThrow(() -> new MemberNotFoundException("사용자를 찾을 수 없습니다."));
        user.delete();
        memberRepository.save(user);
        log.info("사용자 탈퇴 처리 완료: {}", providerUid);
    }

    public Optional<Member> findByProviderAndProviderUid(AuthProvider provider, final String providerUid) {
        return memberRepository.findByAuthProviderAndProviderUid(provider, providerUid);
    }

    public Member findByProviderUid(final String providerUid) {
        return memberRepository.findByProviderUid(providerUid)
                .orElseThrow(() -> new MemberNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
