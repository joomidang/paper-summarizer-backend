package joomidang.papersummary.users.service;

import joomidang.papersummary.users.entity.Member;
import joomidang.papersummary.users.entity.AuthProvider;
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
        return memberRepository.findByProviderUid(member.getProviderUid())
                .orElseGet(() -> memberRepository.save(member));
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

    public Member exists(AuthProvider provider, final String providerUid) {
        return memberRepository.findByAuthProviderAndProviderUid(provider, providerUid)
                .orElseThrow(() -> new MemberNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
