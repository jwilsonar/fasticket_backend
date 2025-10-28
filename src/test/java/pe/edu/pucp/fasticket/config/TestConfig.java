package pe.edu.pucp.fasticket.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.pucp.fasticket.services.S3Service;

/**
 * Configuración de test para mockear servicios externos como S3
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public S3Service mockS3Service() {
        return new S3Service() {
            @Override
            public String uploadFile(MultipartFile file, String folder, Integer entityId) {
                // Mock: retorna una URL simulada
                return String.format("https://test-bucket.s3.us-east-1.amazonaws.com/%s/%d/mock-file.jpg", 
                        folder, entityId);
            }

            @Override
            public List<String> uploadFiles(List<MultipartFile> files, String folder, Integer entityId) {
                // Mock: retorna URLs simuladas para cada archivo
                List<String> urls = new ArrayList<>();
                for (int i = 0; i < files.size(); i++) {
                    urls.add(String.format("https://test-bucket.s3.us-east-1.amazonaws.com/%s/%d/mock-file-%d.jpg", 
                            folder, entityId, i));
                }
                return urls;
            }

            @Override
            public boolean deleteFile(String fileUrl) {
                // Mock: simula eliminación exitosa
                return true;
            }

            @Override
            public String generateUniqueFileName(String originalFilename, String folder, Integer entityId) {
                // Mock: genera un nombre único simulado
                return String.format("mock-file-%d-%s", entityId, originalFilename);
            }
        };
    }
}
