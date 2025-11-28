package pe.edu.pucp.fasticket.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.model.ConfiguracionGlobal;
import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.compra.TransferenciaEntrada;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.ConfiguracionRepository;
import sendinblue.ApiClient;
import sendinblue.ApiException;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final TransactionalEmailsApi transactionalEmailsApi;
    private final JavaMailSender mailSender;
    private final ConfiguracionRepository configuracionRepository;
	// Implementación Brevo (Strategy) para enviar si está habilitado
	private final pe.edu.pucp.fasticket.services.notificaciones.EmailService notificacionesEmailService;

    @Value("${brevo.enabled:false}")
    private boolean brevoEnabled;
    @Value("${brevo.api-key:}")
    private String brevoApiKey;
    @Value("${spring.mail.username:noreply@fasticket.com}")
    private String senderEmail;
    private final String BASE_URL = "http://localhost:8080";
    /**
     * Envía un correo de bienvenida a un nuevo cliente.
     * (Debe ser llamado desde el servicio de registro de usuarios)
     */
    public void enviarCorreoBienvenida(Cliente cliente) {
        String asunto = obtenerValorConfig("EMAIL_REGISTRO_ASUNTO", "¡Bienvenido a FastTicket!");
        String cuerpo = obtenerValorConfig("EMAIL_REGISTRO_CUERPO", "Hola ${nombreUsuario}, gracias por registrarte.");

        // Reemplazar placeholders
        cuerpo = cuerpo.replace("${nombreUsuario}", cliente.getNombres());

        enviarEmail(cliente.getEmail(), asunto, cuerpo, true);
    }

    /**
     * Envía la confirmación de compra.
     * (Debe ser llamado desde OrdenServicio, en 'confirmarPagoOrden')
     */
    @Async
    public void enviarCorreoConfirmacionCompra(OrdenCompra orden) {
        log.info(">>> [EmailService] Preparando correo con enlaces para Orden {}", orden.getIdOrdenCompra());

        try {
            String eventoNombre = "Evento Fasticket";
            int totalTickets = 0;

            if (orden.getItems() != null && !orden.getItems().isEmpty()) {
                eventoNombre = orden.getItems().get(0).getTipoTicket().getEvento().getNombre();
                for (ItemCarrito item : orden.getItems()) {
                    if (item.getTickets() != null) {
                        totalTickets += item.getTickets().size();
                    }
                }
            }

            String asunto = "✅ Confirmación de compra - " + eventoNombre;
            String codigo = orden.getCodigoSeguimiento();

            String linkTickets;
            String linkComprobante;

            if (codigo != null && !codigo.isEmpty()) {
                linkTickets = String.format("%s/api/v1/public/ordenes/tickets/%s", BASE_URL, codigo);
                linkComprobante = String.format("%s/api/v1/public/ordenes/comprobante/%s", BASE_URL, codigo);
            } else {
                linkTickets = String.format("%s/api/v1/ordenes/%d/tickets/descargar", BASE_URL, orden.getIdOrdenCompra());
                linkComprobante = String.format("%s/api/v1/ordenes/%d/comprobante", BASE_URL, orden.getIdOrdenCompra());
            }

            String cuerpoHtml = String.format(
                    "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #eee; padding: 20px; border-radius: 8px;'>" +
                            "<h2 style='color: #007bff; text-align: center;'>¡Hola %s!</h2>" +
                            "<p style='text-align: center; font-size: 16px;'>Tu compra para el evento <strong>%s</strong> fue exitosa.</p>" +
                            "<p style='text-align: center;'>Tienes <strong>%d ticket(s)</strong> listos para descargar.</p>" +
                            "<div style='margin: 35px 0; text-align: center;'>" +
                            "  <a href='%s' style='background-color: #28a745; color: white; padding: 14px 25px; text-decoration: none; border-radius: 50px; font-weight: bold; margin: 10px; display: inline-block; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>📥 Descargar Mis Tickets (ZIP)</a>" +
                            "  <br><br>" +
                            "  <a href='%s' style='background-color: #007bff; color: white; padding: 12px 20px; text-decoration: none; border-radius: 5px; font-size: 14px; margin: 10px; display: inline-block;'>📄 Descargar Boleta de Pago</a>" +
                            "</div>" +
                            "<p style='color: #666; font-size: 13px; text-align: center; margin-top: 30px;'>Estos enlaces son seguros y únicos para tu compra.</p>" +
                            "<hr style='border:none;border-top:1px solid #eee;margin:20px 0;'>" +
                            "<p style='color: #999; font-size: 12px; text-align: center;'>Fasticket - Sistema de venta de entradas</p>" +
                            "</div>",
                    orden.getCliente().getNombres(),
                    eventoNombre,
                    totalTickets,
                    linkTickets,
                    linkComprobante
            );

            enviarEmailViaApi(orden.getCliente().getEmail(), asunto, cuerpoHtml);

        } catch (Exception e) {
            log.error(">>> ERROR CRÍTICO ENVIANDO CORREO: ", e);
        }
    }

    private void enviarEmailViaApi(String para, String asunto, String contenidoHtml) {
        try {
            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();

            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail(senderEmail);
            sender.setName("Fasticket");
            sendSmtpEmail.setSender(sender);

            List<SendSmtpEmailTo> toList = new ArrayList<>();
            SendSmtpEmailTo to = new SendSmtpEmailTo();
            to.setEmail(para);
            toList.add(to);
            sendSmtpEmail.setTo(toList);

            sendSmtpEmail.setSubject(asunto);
            sendSmtpEmail.setHtmlContent(contenidoHtml);

            transactionalEmailsApi.sendTransacEmail(sendSmtpEmail);

            log.info(">>> ¡CORREO ENVIADO! Destino: {}", para);

        } catch (Exception e) {
            log.error("Fallo al contactar API de Brevo: {}", e.getMessage());
            if (e instanceof ApiException) {
                log.error("Respuesta API: {}", ((ApiException) e).getResponseBody());
            }
            throw new RuntimeException(e);
        }
    }

    // Helper para configuración
    private String obtenerValorConfig(String key, String def) {
        return configuracionRepository.findById(key).map(ConfiguracionGlobal::getValue).orElse(def);
    }


    /**
     * Envía correos de notificación de transferencia (RF-045).
     * Uno al emisor y otro al receptor.
     */
    public void enviarCorreoTransferencia(Cliente emisor, Cliente receptor, Ticket ticket) {
        String eventoNombre = ticket.getEvento().getNombre();

        // --- Correo para el EMISOR ---
        String asuntoEmisor = "Has transferido tu entrada para " + eventoNombre;
        String cuerpoEmisor = String.format(
                "Hola %s, has transferido exitosamente tu entrada (Ticket ID: %d) para %s a %s.",
                emisor.getNombres(),
                ticket.getIdTicket(),
                eventoNombre,
                receptor.getEmail()
        );
        enviarEmail(emisor.getEmail(), asuntoEmisor, cuerpoEmisor, false); // false = texto plano

        // --- Correo para el RECEPTOR ---
        String asuntoReceptor = "¡Has recibido una entrada para " + eventoNombre + "!";
        String cuerpoReceptor = String.format(
                "Hola %s, %s (%s) te ha transferido su entrada para el evento %s. " +
                        "El ticket (ID: %d) ahora está en tu cuenta. ¡Que lo disfrutes!",
                receptor.getNombres(),
                emisor.getNombres(),
                emisor.getEmail(),
                eventoNombre,
                ticket.getIdTicket()
        );
        enviarEmail(receptor.getEmail(), asuntoReceptor, cuerpoReceptor, false);
    }

    /**
     * Envía un correo de cancelación de evento a una lista de usuarios.
     * (Debe ser llamado desde EventoService, en 'cancelarEvento')
     */
    public void enviarCorreoCancelacionEvento(Evento evento, List<String> emailsAfectados) {
        String asunto = obtenerValorConfig("EMAIL_CANCELACION_ASUNTO", "Cancelación de evento: ${eventoNombre}");
        String cuerpo = obtenerValorConfig("EMAIL_CANCELACION_CUERPO", "Lamentamos informarte que el evento ha sido cancelado.");

        // Reemplazar placeholders
        asunto = asunto.replace("${eventoNombre}", evento.getNombre());
        cuerpo = cuerpo.replace("${eventoNombre}", evento.getNombre());

        // Convertir la lista de emails a un array para el 'setTo'
        String[] emailsArray = emailsAfectados.toArray(new String[0]);

        if (emailsArray.length > 0) {
            // Usamos bcc (Copia Oculta) para que los usuarios no vean los correos de los demás
            enviarEmailBcc(emailsArray, asunto, cuerpo, true);
        }
    }

    // --- Métodos Privados Helper ---

    /**
     * Motor principal para enviar correos (soporta HTML).
     * Intenta Brevo primero, si falla usa SMTP directo.
     */
    private void enviarEmail(String para, String asunto, String cuerpo, boolean esHtml) {
		if (brevoEnabled && brevoApiKey != null && !brevoApiKey.isBlank()) {
			String nombre = para; // si no tenemos nombre, usamos el email
			String contenidoHtml = esHtml ? cuerpo : "<pre>" + escapeHtml(cuerpo) + "</pre>";
			log.debug("Delegando envío a Brevo (legacy->Brevo) | to={} | asunto={} | html={}", para, asunto, esHtml);
			boolean ok = notificacionesEmailService.enviarEmailHtml(para, nombre, asunto, contenidoHtml);
			if (ok) {
				log.info("✅ Correo enviado exitosamente vía Brevo a: {}", para);
				return;
			} else {
				log.warn("⚠️ Fallo el envío vía Brevo (delegado), intentando SMTP directo. to={} | asunto={}", para, asunto);
				// Continuar con SMTP directo como fallback
			}
		}
		
		// Si Brevo no está habilitado o falló, usar SMTP directo
        try {
            log.info("📧 Enviando correo vía SMTP directo a: {}", para);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail != null ? senderEmail : "no-reply@fasticket.com");
            helper.setTo(para);
            helper.setSubject(asunto);
            helper.setText(cuerpo, esHtml); // true para que interprete el HTML

            mailSender.send(message);
            log.info("✅ Correo enviado exitosamente vía SMTP directo a: {}", para);

        } catch (Exception e) {
            log.error("❌ Error crítico al enviar correo vía SMTP directo a {}: {}", para, e.getMessage(), e);
            throw new RuntimeException("No se pudo enviar el correo: " + e.getMessage(), e);
        }
    }

    /**
     * NUEVO Motor de envío para múltiples destinatarios en Copia Oculta (BCC)
     */
    private void enviarEmailBcc(String[] paraBcc, String asunto, String cuerpo, boolean esHtml) {
		if (brevoEnabled) {
			log.debug("Delegando envío BCC a Brevo (legacy->Brevo) | destinatarios={}", paraBcc.length);
			for (String to : paraBcc) {
				enviarEmail(to, asunto, cuerpo, esHtml);
			}
			return;
		}
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("no-reply@fasticket.com");
            helper.setBcc(paraBcc); // Usamos Bcc en lugar de To
            helper.setSubject(asunto);
            helper.setText(cuerpo, esHtml);

            mailSender.send(message);
            log.info("Correo de cancelación enviado exitosamente a {} destinatarios.", paraBcc.length);

        } catch (Exception e) {
            log.error("Error al enviar correo masivo de cancelación. Causa: {}", e.getMessage());
        }
    }

    /**
     * Obtiene un valor de la tabla de configuración.
     */


    /**
     * Enviar correo para resetear contraseña
     * Si Brevo está habilitado pero falla, intenta SMTP directamente
     */
    public void enviarCorreoResetContrasena(String email, String asunto, String cuerpo) {
        log.info("📧 EmailService.enviarCorreoResetContrasena() llamado | brevoEnabled={} | email={}", 
                brevoEnabled, email != null ? email : "null");
        
        // Si Brevo está habilitado, intentar primero con Brevo
        if (brevoEnabled && brevoApiKey != null && !brevoApiKey.isBlank()) {
            log.info("🔄 Intentando envío vía Brevo (fallback desde EmailService)");
            boolean ok = notificacionesEmailService.enviarEmailHtml(email, email, asunto, cuerpo);
            if (ok) {
                log.info("✅ Email enviado exitosamente vía Brevo (fallback)");
                return;
            } else {
                log.warn("⚠️ Brevo falló en fallback, intentando SMTP directo");
            }
        }
        
        // Si Brevo no está habilitado o falló, usar SMTP directamente
        log.info("📧 Enviando correo vía SMTP directo (sin Brevo)");
        enviarEmailDirecto(email, asunto, cuerpo, true);
    }
    
    /**
     * Envía email directamente vía SMTP, sin pasar por Brevo
     */
    private void enviarEmailDirecto(String para, String asunto, String cuerpo, boolean esHtml) {
        try {
            log.info("📤 Preparando envío SMTP directo a: {}", para);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail != null ? senderEmail : "no-reply@fasticket.com");
            helper.setTo(para);
            helper.setSubject(asunto);
            helper.setText(cuerpo, esHtml);

            mailSender.send(message);
            log.info("✅ Correo enviado exitosamente vía SMTP directo a: {}", para);

        } catch (Exception e) {
            log.error("❌ Error crítico al enviar correo vía SMTP directo a {}: {}", para, e.getMessage(), e);
            throw new RuntimeException("No se pudo enviar el correo vía SMTP: " + e.getMessage(), e);
        }
    }

	private String escapeHtml(String text) {
		if (text == null) return "";
		return text
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}
}