package joomidang.papersummary.s3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * 로컬 환경에서 테스트를 위한 가짜 S3 설정
 * <p>
 * 실제 AWS 자격 증명 없이도 테스트할 수 있도록 가짜 S3Client를 제공한다.
 */
@Configuration
@Profile("local")
public class FakeS3Config {

    /**
     * 로컬 테스트용 가짜 S3Client 빈
     * <p>
     * 실제 AWS 자격 증명 없이도 테스트할 수 있도록 가짜 자격 증명을 사용한다.
     * <p>
     * @Primary 어노테이션을 사용하여 기본 S3Config의 s3Client 빈보다 우선 적용되도록 한다.
     */
    @Bean
    @Primary
    public S3Client s3Client() {
        // 가짜 자격 증명으로 S3Client 생성
        // 실제로는 이 클라이언트가 사용되지 않지만, 스프링 컨텍스트 로딩을 위해 필요함
        return S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("fake-access-key", "fake-secret-key")
                        )
                ).build();
    }
}