package joomidang.papersummary.member.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberInterestResponse {
    private String[] interests;

    public static MemberInterestResponse from(String[] interests){
        return MemberInterestResponse.builder()
                .interests(interests != null ? interests : new String[0])
                .build();
    }
}
