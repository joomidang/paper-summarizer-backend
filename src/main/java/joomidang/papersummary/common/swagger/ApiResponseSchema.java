package joomidang.papersummary.common.swagger;

import io.swagger.v3.oas.annotations.media.Schema;
import joomidang.papersummary.common.controller.response.SuccessCode;

public class ApiResponseSchema<T> {

    @Schema(description = "응답 코드", example = "")
    private String code;

    @Schema(description = "응답 메시지", example = "")
    private String message;

    @Schema(description = "응답 데이터")
    private T data;

}