package joomidang.papersummary.member.controller.request;

import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 프로필 수정 요청 DTO
 * 모든 필드는 선택적(optional)이며, 제공된 필드만 업데이트됩니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 60, message = "사용자 이름은 60자 이하여야 합니다.")
    private String username;
    
    private String profileImageUrl;
    
    private List<String> interests;
}