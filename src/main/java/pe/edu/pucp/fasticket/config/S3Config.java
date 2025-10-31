package pe.edu.pucp.fasticket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${AWS_S3_REGION:us-east-1}")
    private String awsRegion;

    @Value("${AWS_ACCESS_KEY_ID:}")
    private String accessKeyId;

    @Value("${AWS_SECRET_ACCESS_KEY:}")
    private String secretAccessKey;

    @Value("${AWS_SESSION_TOKEN:}")
    private String sessionToken;

    @Bean
    public S3Client s3Client() {
        // Prioridad 1: Variables de entorno con credenciales completas
        if (!accessKeyId.isEmpty() && !secretAccessKey.isEmpty()) {
            // Si hay session token, significa que son credenciales temporales (STS)
            if (!sessionToken.isEmpty()) {
                return S3Client.builder()
                        .region(Region.of(awsRegion))
                        .credentialsProvider(
                            StaticCredentialsProvider.create(
                                AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken)
                            )
                        )
                        .build();
            } else {
                // Credenciales permanentes
                return S3Client.builder()
                        .region(Region.of(awsRegion))
                        .credentialsProvider(
                            StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                            )
                        )
                        .build();
            }
        } 
        
        // Prioridad 2: DefaultCredentialsProvider (intenta múltiples fuentes)
        // Esto buscará en: variables de entorno, credential files, IAM roles, etc.
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
