package pe.edu.pucp.fasticket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
@Configuration
public class S3Config {

    @Value("${aws.s3.region:us-east-1}")
    private String awsRegionFromProps;

    @Bean
    public S3Client s3Client() {
        // Leer variables de entorno directamente (más confiable que @Value para variables de entorno)
        String accessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
        String secretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        String sessionToken = System.getenv("AWS_SESSION_TOKEN");
        String region = System.getenv("AWS_S3_REGION");
        
        // Limpiar y validar valores (remover espacios en blanco)
        if (accessKeyId != null) accessKeyId = accessKeyId.trim();
        if (secretAccessKey != null) secretAccessKey = secretAccessKey.trim();
        if (sessionToken != null) sessionToken = sessionToken.trim();
        if (region != null) region = region.trim();
        
        // Si no hay región en variable de entorno, usar la de properties o default
        if (region == null || region.isEmpty()) {
            region = awsRegionFromProps != null && !awsRegionFromProps.isEmpty() ? awsRegionFromProps : "us-east-1";
        }
        
        boolean tieneAccessKey = accessKeyId != null && !accessKeyId.isEmpty();
        boolean tieneSecretKey = secretAccessKey != null && !secretAccessKey.isEmpty();
        
        // Validar session token: debe estar presente, no vacío, no ser "null", y tener longitud mínima válida
        // Los tokens STS válidos suelen tener al menos 100 caracteres
        boolean tieneSessionTokenValido = false;
        if (sessionToken != null && !sessionToken.isEmpty()) {
            String tokenLower = sessionToken.toLowerCase();
            tieneSessionTokenValido = !tokenLower.equals("null") 
                    && !tokenLower.equals("none")
                    && !tokenLower.equals("false")
                    && sessionToken.length() > 20; // Longitud mínima razonable para un token válido
        }
        
        log.info("🔧 Configurando S3Client | region={} | tieneAccessKey={} | tieneSecretKey={} | tieneSessionTokenValido={} | sessionTokenLength={}", 
                region, tieneAccessKey, tieneSecretKey, tieneSessionTokenValido,
                sessionToken != null ? sessionToken.length() : 0);
        
        // Prioridad 1: Variables de entorno con credenciales completas
        if (tieneAccessKey && tieneSecretKey) {
            // Solo usar session token si está presente y es válido
            if (tieneSessionTokenValido) {
                log.info("✅ Usando credenciales temporales (STS) para S3");
                return S3Client.builder()
                        .region(Region.of(region))
                        .credentialsProvider(
                            StaticCredentialsProvider.create(
                                AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken)
                            )
                        )
                        .build();
            } else {
                // Si hay session token pero es inválido, ignorarlo y usar credenciales permanentes
                if (sessionToken != null && !sessionToken.isEmpty()) {
                    log.warn("⚠️ Session token presente pero inválido (longitud: {}), usando credenciales permanentes", sessionToken.length());
                }
                log.info("✅ Usando credenciales permanentes para S3");
                return S3Client.builder()
                        .region(Region.of(region))
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
        log.info("⚠️ No se encontraron credenciales explícitas, usando DefaultCredentialsProvider para S3");
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
