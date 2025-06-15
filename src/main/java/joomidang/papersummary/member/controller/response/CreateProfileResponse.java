package joomidang.papersummary.member.controller.response;

import java.util.List;
import java.util.stream.Collectors;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.entity.MemberInterest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProfileResponse {
    
    private Long id;
    private String username;
    private String profileImageUrl;
    private List<String> interests;
    
    public static CreateProfileResponse from(Member member) {
        List<String> interestList = member.getInterests().stream()
                .map(MemberInterest::getInterest)
                .collect(Collectors.toList());
        
        return CreateProfileResponse.builder()
                .id(member.getId())
                .username(member.getName())
                .profileImageUrl(member.getProfileImage())
                .interests(interestList)
                .build();
    }
}