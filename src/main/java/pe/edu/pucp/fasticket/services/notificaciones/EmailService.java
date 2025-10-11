package pe.edu.pucp.fasticket.services.notificaciones;

import java.util.Map;

/**
 * Interfaz para el servicio de envío de emails.
 * Patrón Strategy: Permite cambiar la implementación del proveedor de email
 * (Brevo, SendGrid, AWS SES, etc.) sin afectar el resto del código.
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
public interface EmailService {
    
    /**
     * Envía un email con plantilla.
     * 
     * @param destinatario Email del destinatario
     * @param nombreDestinatario Nombre del destinatario
     * @param asunto Asunto del email
     * @param templateId ID de la plantilla (en Brevo)
     * @param parametros Parámetros para reemplazar en la plantilla
     * @return true si se envió exitosamente, false en caso contrario
     */
    boolean enviarEmail(String destinatario, String nombreDestinatario, String asunto, 
                       Long templateId, Map<String, Object> parametros);
    
    /**
     * Envía un email simple con HTML.
     * 
     * @param destinatario Email del destinatario
     * @param nombreDestinatario Nombre del destinatario
     * @param asunto Asunto del email
     * @param contenidoHtml Contenido HTML del email
     * @return true si se envió exitosamente, false en caso contrario
     */
    boolean enviarEmailHtml(String destinatario, String nombreDestinatario, 
                           String asunto, String contenidoHtml);
}

