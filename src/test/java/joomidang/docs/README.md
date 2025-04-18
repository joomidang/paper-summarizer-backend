# REST Docs Support Guide

Spring REST Docs 기반 API 문서화를 효율적으로 구성하기 위한 유틸리티 클래스 RestDocsSupport의 사용법을 정리합니다.

## 목차

- [개요](#개요)
- [RestDocsSupport 메서드 사용방법](#RestDocsSupport 메서드 사용방법)
- [RestDocsSupport 사용 방법](#restdocssupport-사용-방법)
- [샘플 컨트롤러 문서화 예제](#샘플-컨트롤러-문서화-예제)
- [문서화 스니펫 종류](#문서화-스니펫-종류)
- [문서 생성 및 확인](#문서-생성-및-확인)

## 개요

Spring REST Docs는 테스트 코드를 기반으로 API 문서를 자동으로 생성해주는 도구입니다. 이 프로젝트에서는 REST Docs를 더 쉽게 사용할 수 있도록 `RestDocsSupport` 클래스를
제공합니다.

주요 기능:

- MockMvc 설정 자동화
- 문서 생성 헬퍼 메소드 제공
- URI 디코딩 전처리기 제공
- JSON 변환 유틸리티 제공
---
## RestDocsSupport 메서드 사용방법

### `createdocment(Snippet... snippets)`

**설명**

- REST Docs 문서 생성을 위한 핸들러를 반환해주는 메서드
- `{class-name}/{method-name}` 경로로 문서를 만들어줘서 자동 분류됨
  전처리기 설정(`getDocumentRequest()`)도 함께 적용됨.

**사용 예시**

```java
.andDo(createDocument(
        requestFields(
            fieldWithPath("email").description("이메일"),
            fieldWithPath("password").description("비밀번호")
        ),
        responseFields(
            fieldWithPath("id").description("생성된 사용자 ID")
        )
));
```

### `getDocumentReqeust()`

**설명**

- 요청 전처리기로 URI 디코딩을 적용함.
- 예를 들어 %20이 포함된 URI가 테스트 결과에 그대로 나오는 게 아니라, 사람이 읽기 쉽게 바꿈

**사용 예시**
```markdown
`createDocument()` 안에서 사용됨.
```

### `constraints(String value)`

**설명**

- 필드에 대한 **제약 조건 설명**을 문서화할 때 사용하는 attribute
  문서에서 제약조건: `최소 3자 이상` 같은 텍스트를 추가할 수 있음.

**사용 예시**

```java
fieldWithPath("username")
    .description("사용자 이름")
    .attributes(constraints("최소 3자, 최대 20자"))
```
### `pathVariableExample(lone value)`

**설명**

- `PathVariable`에 대한 예제 값을 문서화할 수 있는 attribute
  문서에 "예: 123" 같은 값이 보이도록 만들 수 있음

**사용 예시**

```java
parameterWithName("userId")
    .description("사용자 ID")
    .attributes(pathVariableExample(101L))
```
### `convertToJson(Object object)`

**설명**

- Java 객체를 Json 무자열로 변환해주는 메서드
  `ObjectMapper`를 내부적으로 사용해서 POST/PUT 요청 본문을 만들 때 사용됨

**사용 예시**

```java
String json = convertToJson(new UserRequest("test@example.com", "1234"));
mockMvc.perform(post("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isCreated());
```

### `initController()`

**설명**

- 추상 메서드로, 각 테스트 클래스에서 테스트 대상 컨트롤러 인스턴스를 리턴해야 함.
  `standaloneSetup()` 방식으로 테스트 환경을 구성하는 데 사용됨.

**사용 예시**

```java
@Override
protected Object initController() {
   return new UserController(mockUserService);
}
```
---
## RestDocsSupport 사용 방법

### 1. RestDocsSupport 상속

API 문서화 테스트 클래스는 `RestDocsSupport`를 상속받아 구현합니다:

```java
public class YourControllerDocumentation extends RestDocsSupport {

    @Override
    protected Object initController() {
        return new YourController();
    }

    // 테스트 메소드들...
}
```

### 2. 컨트롤러 초기화

`initController()` 메소드를 오버라이드하여 테스트할 컨트롤러 인스턴스를 반환합니다.

### 3. 테스트 메소드 작성

각 API 엔드포인트에 대한 테스트 메소드를 작성합니다:

```java

@Test
@DisplayName("API 문서화 테스트")
void testApiEndpoint() throws Exception {
    // given
    // 테스트 데이터 준비

    // when
    ResultActions result = mockMvc.perform(
            get("/api/your-endpoint")
                    .contentType(MediaType.APPLICATION_JSON)
    );

    // then
    result.andExpect(status().isOk())
            .andDo(createDocument(
                    // 스니펫 정의
                    responseFields(
                            fieldWithPath("field1").type(JsonFieldType.STRING)
                                    .description("필드1 설명"),
                            fieldWithPath("field2").type(JsonFieldType.NUMBER)
                                    .description("필드2 설명")
                    )
            ));
}
```
---
## 샘플 컨트롤러 문서화 예제

이 프로젝트에는 `SampleController`와 이를 문서화하는 `SampleControllerDocumentation` 클래스가 포함되어 있습니다. 이 예제는 다음과 같은 다양한 API 문서화 방법을
보여줍니다:

1. **GET 요청 문서화**
    - 목록 조회 API
    - 단일 항목 조회 API (경로 변수 포함)

2. **POST 요청 문서화**
    - 요청 본문 필드 문서화
    - 응답 본문 필드 문서화

3. **PUT 요청 문서화**
    - 경로 변수 문서화
    - 요청 본문 필드 문서화
    - 응답 본문 필드 문서화

4. **DELETE 요청 문서화**
    - 경로 변수 문서화

## 문서화 스니펫 종류

RestDocsSupport를 사용하여 다음과 같은 스니펫을 생성할 수 있습니다:

1. **요청 필드 문서화**
   ```java
   requestFields(
           fieldWithPath("name").type(JsonFieldType.STRING)
                   .description("이름"),
           fieldWithPath("age").type(JsonFieldType.NUMBER)
                   .description("나이")
   )
   ```

2. **응답 필드 문서화**
   ```java
   responseFields(
           fieldWithPath("id").type(JsonFieldType.NUMBER)
                   .description("ID"),
           fieldWithPath("name").type(JsonFieldType.STRING)
                   .description("이름")
   )
   ```

3. **경로 변수 문서화**
   ```java
   pathParameters(
           parameterWithName("id").description("항목 ID")
   )
   ```

4. **쿼리 파라미터 문서화**
   ```java
   queryParameters(
           parameterWithName("page").description("페이지 번호"),
           parameterWithName("size").description("페이지 크기")
   );
   ```
---
## 문서 생성 및 확인

1. 테스트 실행:
   ```
   ./gradlew test
   ```

=> build/generated-snippets 데릭토리에 만들어진 snippet (.adoc 파일)을 보고 src/docs/asciidoc 하위에 폴더만들고 추가합니다.

2. 문서 생성:
   ```
   ./gradlew asciidoctor
   ```

- src/docs/asciidoc에 있는 .adoc 파일들을 읽고 스니펫들을 포함하여 html 파일로 변환합니다.

3. 생성된 문서 확인:
    - `build/docs/asciidoc` html 파일 확인함