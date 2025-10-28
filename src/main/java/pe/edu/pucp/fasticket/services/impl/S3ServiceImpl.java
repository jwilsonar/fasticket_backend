package pe.edu.pucp.fasticket.services.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.services.S3Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name:test-bucket}")
    private String bucketName;

    @Value("${aws.s3.region:us-east-1}")
    private String awsRegion;

    @Override
    public String uploadFile(MultipartFile file, String folder, Integer entityId) {
        try {
            String fileName = generateUniqueFileName(file.getOriginalFilename(), folder, entityId);
            String key = folder + "/" + entityId + "/" + fileName;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", 
                    bucketName, awsRegion, key);

            log.info("Archivo subido exitosamente: {}", fileUrl);
            return fileUrl;

        } catch (Exception e) {
            log.error("Error al subir archivo a S3: {}", e.getMessage());
            throw new RuntimeException("Error al subir archivo a S3", e);
        }
    }

    @Override
    public List<String> uploadFiles(List<MultipartFile> files, String folder, Integer entityId) {
        List<String> uploadedUrls = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String url = uploadFile(file, folder, entityId);
                uploadedUrls.add(url);
            }
        }
        
        return uploadedUrls;
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            // Extraer la clave del archivo de la URL
            String key = extractKeyFromUrl(fileUrl);
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Archivo eliminado exitosamente: {}", fileUrl);
            return true;

        } catch (Exception e) {
            log.error("Error al eliminar archivo de S3: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String generateUniqueFileName(String originalFilename, String folder, Integer entityId) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return UUID.randomUUID().toString();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        
        // Obtener extensión del archivo
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }

        return String.format("%s_%s_%d%s", timestamp, uuid, entityId, extension);
    }

    private String extractKeyFromUrl(String fileUrl) {
        // URL format: https://bucket-name.s3.region.amazonaws.com/folder/entityId/filename
        String baseUrl = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, awsRegion);
        return fileUrl.replace(baseUrl, "");
    }
}
