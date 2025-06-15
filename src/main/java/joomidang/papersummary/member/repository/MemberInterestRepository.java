package joomidang.papersummary.member.repository;

import java.util.List;

import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.entity.MemberInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberInterestRepository extends JpaRepository<MemberInterest, Long> {
    List<MemberInterest> findByMember(Member member);
    List<MemberInterest> findByMemberId(Long memberId);
    void deleteByMember(Member member);
}