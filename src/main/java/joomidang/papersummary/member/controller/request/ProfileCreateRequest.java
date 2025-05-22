package joomidang.papersummary.member.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCreateRequest {
    
    private Long id;
    
    @NotBlank(message = "사용자 이름은 필수입니다.")
    @Size(max = 60, message = "사용자 이름은 60자 이하여야 합니다.")
    private String username;
    
    private String profileImageUrl;
    
    @NotEmpty(message = "관심분야는 최소 1개 이상 입력해야 합니다.")
    private List<String> interests;
}