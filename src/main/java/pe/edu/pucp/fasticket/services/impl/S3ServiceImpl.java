package pe.edu.pucp.fasticket.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name:${AWS_S3_BUCKET_NAME:test-bucket}}")
    private String bucketName;

    @Value("${aws.s3.region:${AWS_S3_REGION:us-east-1}}")
    private String awsRegion;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        // Si el bucket name viene como test-bucket (default), intentar leer de variable de entorno
        if (bucketName == null || bucketName.isEmpty() || bucketName.equals("test-bucket")) {
            String envBucket = System.getenv("AWS_S3_BUCKET_NAME");
            if (envBucket != null && !envBucket.isEmpty()) {
                bucketName = envBucket;
                log.info("📦 Bucket name leído desde variable de entorno: {}", bucketName);
            }
        }
        
        // Si la región no está configurada, intentar leer de variable de entorno
        if (awsRegion == null || awsRegion.isEmpty()) {
            String envRegion = System.getenv("AWS_S3_REGION");
            if (envRegion != null && !envRegion.isEmpty()) {
                awsRegion = envRegion;
            } else {
                awsRegion = "us-east-1";
            }
        }
        
        log.info("🔧 S3ServiceImpl inicializado | bucket={} | region={}", bucketName, awsRegion);
    }

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

            String fileUrl = generateS3Url(bucketName, awsRegion, key);

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
    public String uploadFileFromBytes(byte[] fileBytes, String fileName, String contentType, String folder, Integer entityId) {
        try {
            // Validar que tenemos el bucket name
            if (bucketName == null || bucketName.isEmpty() || bucketName.equals("test-bucket")) {
                log.error("❌ Bucket name no configurado o es el valor por defecto: {}", bucketName);
                throw new RuntimeException("Bucket name de S3 no está configurado. Verifica la propiedad aws.s3.bucket-name");
            }
            
            // Validar que tenemos la región
            if (awsRegion == null || awsRegion.isEmpty()) {
                log.error("❌ Región de S3 no configurada");
                throw new RuntimeException("Región de S3 no está configurada. Verifica la propiedad aws.s3.region");
            }
            
            String key = folder + "/" + entityId + "/" + fileName;
            
            log.info("📤 Subiendo archivo a S3 | bucket={} | region={} | key={} | size={} bytes | contentType={}", 
                    bucketName, awsRegion, key, fileBytes.length, contentType);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) fileBytes.length)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));

            String fileUrl = generateS3Url(bucketName, awsRegion, key);

            log.info("✅ Archivo subido exitosamente desde bytes: {} ({} bytes)", fileUrl, fileBytes.length);
            return fileUrl;

        } catch (Exception e) {
            log.error("❌ Error al subir archivo desde bytes a S3 | bucket={} | region={} | error={}", 
                    bucketName, awsRegion, e.getMessage(), e);
            throw new RuntimeException("Error al subir archivo a S3: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] downloadFile(String fileUrl) {
        try {
            // Extraer la clave del archivo de la URL
            String key = extractKeyFromUrl(fileUrl);
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest);
            
            // Leer el contenido del stream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = response.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            response.close();
            
            byte[] fileBytes = baos.toByteArray();
            baos.close();
            
            log.info("Archivo descargado exitosamente desde S3: {} ({} bytes)", fileUrl, fileBytes.length);
            return fileBytes;

        } catch (IOException e) {
            log.error("Error al descargar archivo de S3: {}", e.getMessage(), e);
            throw new RuntimeException("Error al descargar archivo de S3", e);
        }
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

    /**
     * Genera la URL de S3 correcta según la región
     * Para us-east-1 usa formato: https://bucket.s3.amazonaws.com/key
     * Para otras regiones: https://bucket.s3.region.amazonaws.com/key
     */
    private String generateS3Url(String bucket, String region, String key) {
        if (region == null || region.isEmpty() || "us-east-1".equals(region)) {
            // us-east-1 no incluye la región en la URL
            return String.format("https://%s.s3.amazonaws.com/%s", bucket, key);
        } else {
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
        }
    }

    private String extractKeyFromUrl(String fileUrl) {
        // URL format puede ser:
        // https://bucket-name.s3.amazonaws.com/key (us-east-1)
        // https://bucket-name.s3.region.amazonaws.com/key (otras regiones)
        String baseUrl1 = String.format("https://%s.s3.amazonaws.com/", bucketName);
        String baseUrl2 = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, awsRegion);
        
        if (fileUrl.startsWith(baseUrl1)) {
            return fileUrl.replace(baseUrl1, "");
        } else if (fileUrl.startsWith(baseUrl2)) {
            return fileUrl.replace(baseUrl2, "");
        }
        // Fallback: intentar extraer desde cualquier formato de URL S3
        if (fileUrl.contains(".s3.")) {
            int index = fileUrl.indexOf(".s3.");
            int slashIndex = fileUrl.indexOf("/", index + 4);
            if (slashIndex > 0) {
                return fileUrl.substring(slashIndex + 1);
            }
        }
        return fileUrl;
    }
}
