package pe.edu.pucp.fasticket.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.services.S3Service;
import sibApi.TransactionalEmailsApi;

/**
 * Configuración de test para mockear servicios externos como S3, Email, etc.
 */
@TestConfiguration
@Slf4j
public class TestConfig {

    /**
     * Mock del servicio S3 para evitar llamadas reales a AWS
     */
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

            @Override
            public String uploadFileFromBytes(byte[] fileBytes, String fileName, String contentType, String folder, Integer entityId) {
                // Mock: retorna una URL simulada
                return String.format("https://test-bucket.s3.us-east-1.amazonaws.com/%s/%d/%s", 
                        folder, entityId, fileName);
            }

            @Override
            public byte[] downloadFile(String fileUrl) {
                // Mock: retorna bytes simulados de un PDF válido
                // Cabecera PDF válida: %PDF-1.4 seguido de contenido mínimo
                return "%PDF-1.4\n%%EOF".getBytes();
            }
        };
    }

    /**
     * Mock del JavaMailSender para evitar envío real de correos
     */
    @Bean
    @Primary
    public JavaMailSender mockJavaMailSender() {
        return new JavaMailSenderImpl();
    }

    /**
     * Mock de la API transaccional de Brevo
     */
    @Bean
    @Primary
    public TransactionalEmailsApi mockTransactionalEmailsApi() {
        return Mockito.mock(TransactionalEmailsApi.class);
    }

    /**
     * Mock del servicio de notificaciones por email (Brevo)
     * Retorna siempre true para simular envío exitoso
     */
    @Bean
    @Primary
    public pe.edu.pucp.fasticket.services.notificaciones.EmailService mockNotificacionesEmailService() {
        return new pe.edu.pucp.fasticket.services.notificaciones.EmailService() {
            @Override
            public boolean enviarEmail(String destinatario, String nombreDestinatario, String asunto,
                                      Long templateId, Map<String, Object> parametros) {
                log.info("📧 [TEST MOCK] Email con plantilla simulado: to={}, asunto={}, templateId={}", 
                        destinatario, asunto, templateId);
                return true;
            }

            @Override
            public boolean enviarEmailHtml(String destinatario, String nombreDestinatario,
                                          String asunto, String contenidoHtml) {
                log.info("📧 [TEST MOCK] Email HTML simulado: to={}, asunto={}", destinatario, asunto);
                return true;
            }

            @Override
            public boolean enviarEmailHtmlConAdjuntos(String destinatario, String nombreDestinatario,
                                                     String asunto, String contenidoHtml,
                                                     List<Map<String, Object>> adjuntos) {
                log.info("📧 [TEST MOCK] Email con adjuntos simulado: to={}, asunto={}, adjuntos={}", 
                        destinatario, asunto, adjuntos != null ? adjuntos.size() : 0);
                return true;
            }
        };
    }
}
