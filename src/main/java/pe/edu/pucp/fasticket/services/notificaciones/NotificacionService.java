package pe.edu.pucp.fasticket.services.notificaciones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.notificaciones.TipoNotificacion;
import pe.edu.pucp.fasticket.model.notificaciones.TipoPlantilla;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;
import pe.edu.pucp.fasticket.services.pago.PagoServicio;

/**
 * Servicio de notificaciones que orquesta el envío de diferentes tipos de emails.
 * 
 * Implementa los requerimientos:
 * - RF-048: Verificación de cuenta
 * - RF-049: Confirmación de compra
 * - RF-050: Recordatorios de evento (ToDO: implementar con scheduler)
 * - RF-052: Recuperación de contraseña
 * - RF-086: Envío de tickets
 * 
 * Este servicio actúa como una fachada (Patrón Facade) que simplifica
 * la interacción con el sistema de emails.
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionService {

    private final EmailService emailService;
    private final PlantillaService plantillaService;
    private final NotificationManager notificationManager;
    private final PersonasRepositorio personasRepo;
    private final PagoServicio pagoServicio;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * RF-048: Envía email de verificación de cuenta y notificación in-app.
     */
    public void enviarEmailVerificacion(String email, String nombreCompleto, String token) {
        log.info("Enviando email de verificación a: {}", email);

        String asunto = "Verifica tu cuenta en Fasticket";
        String linkVerificacion = frontendUrl + "/verificar-cuenta?token=" + token;

        Map<String, Object> params = new HashMap<>();
        params.put("nombre", nombreCompleto);
        params.put("linkVerificacion", linkVerificacion);

        try {
            String html = null;
            var plantilla = plantillaService.obtenerActiva(pe.edu.pucp.fasticket.model.notificaciones.TipoPlantilla.VERIFICAR_CUENTA);
            if (plantilla != null) {
                asunto = plantilla.getAsunto();
                html = plantillaService.render(plantilla.getHtml(), params);
            } else {
                html = String.format("<h2>Hola %s</h2><p>Verifica tu cuenta haciendo clic "
                    + "<a href=\"%s\">aquí</a>.</p><p>Si no funciona, copia y pega el enlace: %s</p>",
                    nombreCompleto, linkVerificacion, linkVerificacion);
            }

            boolean enviado = emailService.enviarEmailHtml(email, nombreCompleto, asunto, html);

            // Notificación in-app usando el manager (resolver personaId por email)
            NotificationRequest req = NotificationRequest.builder()
                .email(email)
                .nombre(nombreCompleto)
                .notiTipo(TipoNotificacion.VERIFICACION_CUENTA)
                .plantilla(TipoPlantilla.VERIFICAR_CUENTA)
                .params(params)
                .titulo("Verifica tu cuenta")
                .mensaje("Hemos enviado un correo con el enlace de verificación.")
                .build();
            notificationManager.notifyAllChannels(req);

            if (enviado) {
                log.info("Email de verificación enviado exitosamente");
            } else {
                log.warn("No se pudo enviar email de verificación, pero el registro continuó");
            }
        } catch (Exception e) {
            log.error("Error al enviar email de verificación (no crítico): {}", e.getMessage());
            // NO propagamos la excepción - el registro debe completarse
        }
    }

    /**
     * RF-049, RF-086: Envía confirmación de compra con detalles y comprobante PDF adjunto.
     */
    public void enviarConfirmacionCompra(OrdenCompra orden, String emailCliente, String nombreCliente) {
        log.info("Enviando confirmación de compra a: {}", emailCliente);

        String asunto = "Confirmación de Compra - Fasticket #" + orden.getIdOrdenCompra();

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("nombre", nombreCliente);
            params.put("idOrden", orden.getIdOrdenCompra());
            params.put("total", orden.getTotal());
            var plantilla = plantillaService.obtenerActiva(pe.edu.pucp.fasticket.model.notificaciones.TipoPlantilla.CONFIRMACION_COMPRA);
            String html = plantilla != null
                ? plantillaService.render(plantilla.getHtml(), params)
                : String.format("<h2>Hola %s</h2><p>Tu compra fue confirmada.</p>"
                    + "<p>Número de Orden: #%d</p><p>Total: S/ %.2f</p>"
                    + "<p>Adjunto encontrarás el comprobante de pago en formato PDF.</p>",
                    nombreCliente, orden.getIdOrdenCompra(), orden.getTotal());
            if (plantilla != null) asunto = plantilla.getAsunto();

            // Generar PDF del comprobante y adjuntarlo
            List<Map<String, Object>> adjuntos = new ArrayList<>();
            try {
                byte[] pdfComprobante = pagoServicio.generarComprobantePdf(orden);
                Map<String, Object> adjunto = new HashMap<>();
                adjunto.put("nombre", "Comprobante_ORD-" + orden.getIdOrdenCompra() + ".pdf");
                adjunto.put("contenido", pdfComprobante);
                adjuntos.add(adjunto);
                log.info("PDF del comprobante generado exitosamente para orden ID: {}", orden.getIdOrdenCompra());
            } catch (Exception e) {
                log.warn("No se pudo generar el PDF del comprobante para orden ID: {}. Error: {}", 
                    orden.getIdOrdenCompra(), e.getMessage());
                // Continuamos sin adjunto si falla la generación del PDF
            }

            boolean enviado;
            if (!adjuntos.isEmpty()) {
                enviado = emailService.enviarEmailHtmlConAdjuntos(emailCliente, nombreCliente, asunto, html, adjuntos);
            } else {
                enviado = emailService.enviarEmailHtml(emailCliente, nombreCliente, asunto, html);
            }

            // Notificación in-app vía NotificationManager
            NotificationRequest reqCompra = NotificationRequest.builder()
                .email(emailCliente)
                .nombre(nombreCliente)
                .notiTipo(TipoNotificacion.CONFIRMACION_COMPRA)
                .plantilla(TipoPlantilla.CONFIRMACION_COMPRA)
                .params(params)
                .titulo("Compra confirmada")
                .mensaje("Tu compra #" + orden.getIdOrdenCompra() + " ha sido confirmada.")
                .build();
            notificationManager.notifyAllChannels(reqCompra);

            if (enviado) {
                log.info("Confirmación de compra enviada exitosamente con comprobante adjunto");
            } else {
                log.warn("No se pudo enviar confirmación, pero la compra se completó");
            }
        } catch (Exception e) {
            log.error("Error al enviar confirmación (no crítico): {}", e.getMessage());
        }
    }

    /**
     * RF-016: Envía notificación de evento cancelado.
     */
    public void enviarNotificacionEventoCancelado(Evento evento, String emailCliente, 
                                                  String nombreCliente, String motivo) {
        log.info("Enviando notificación de evento cancelado a: {}", emailCliente);

        String asunto = "Evento Cancelado - " + evento.getNombre();

        try {
            String html = String.format("<h2>Hola %s</h2><p>Lamentamos informarte que el evento "
                + "<strong>%s</strong> ha sido cancelado.</p><p><strong>Motivo:</strong> %s</p>"
                + "<p>Se procesará el reembolso en los próximos días hábiles.</p>",
                nombreCliente, evento.getNombre(), motivo);
            boolean enviado = emailService.enviarEmailHtml(emailCliente, nombreCliente, asunto, html);

            // Notificación in-app vía NotificationManager
            NotificationRequest reqCancel = NotificationRequest.builder()
                .email(emailCliente)
                .nombre(nombreCliente)
                .notiTipo(TipoNotificacion.SISTEMA)
                .titulo("Evento cancelado")
                .mensaje("El evento " + evento.getNombre() + " ha sido cancelado. Motivo: " + motivo)
                .build();
            notificationManager.notifyAllChannels(reqCancel);

            if (enviado) {
                log.info("Notificación de cancelación enviada");
            }
        } catch (Exception e) {
            log.error("Error al enviar notificación de cancelación: {}", e.getMessage());
        }
    }

    /**
     * RF-052: Envía email de recuperación de contraseña.
     */
    public void enviarRecuperacionContrasena(String email, String nombreCompleto, String token) {
        log.info("📨 Enviando recuperación de contraseña a: {}", email);

        String asunto = "Recuperación de Contraseña - Fasticket";
        String linkRecuperacion = frontendUrl + "/recuperar-contrasena?token=" + token;

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("nombre", nombreCompleto);
            params.put("linkRecuperacion", linkRecuperacion);
            var plantilla = plantillaService.obtenerActiva(pe.edu.pucp.fasticket.model.notificaciones.TipoPlantilla.CONFIRMACION_RECUPERACION_CONTRASENA);
            String html = plantilla != null
                ? plantillaService.render(plantilla.getHtml(), params)
                : String.format("<h2>Hola %s</h2><p>Para recuperar tu contraseña ingresa "
                    + "<a href=\"%s\">aquí</a>.</p><p>Enlace: %s</p>",
                    nombreCompleto, linkRecuperacion, linkRecuperacion);
            if (plantilla != null) asunto = plantilla.getAsunto();

            boolean enviado = emailService.enviarEmailHtml(email, nombreCompleto, asunto, html);

            // Notificación in-app vía NotificationManager
            NotificationRequest reqRec = NotificationRequest.builder()
                .email(email)
                .nombre(nombreCompleto)
                .notiTipo(TipoNotificacion.RECUPERACION_CONTRASENA)
                .plantilla(TipoPlantilla.CONFIRMACION_RECUPERACION_CONTRASENA)
                .params(params)
                .titulo("Recuperación de contraseña")
                .mensaje("Hemos enviado un enlace de recuperación a tu correo.")
                .build();
            notificationManager.notifyAllChannels(reqRec);

            if (enviado) {
                log.info("Email de recuperación enviado exitosamente");
            }
        } catch (Exception e) {
            log.error("Error al enviar recuperación de contraseña: {}", e.getMessage());
        }
    }

    /**
     * RF-089: Envía notificación de compra anulada.
     */
    public void enviarNotificacionCompraAnulada(OrdenCompra orden, String emailCliente,
                                               String nombreCliente, String motivo) {
        log.info("📨 Enviando notificación de compra anulada a: {}", emailCliente);

        String asunto = "Compra Anulada - Fasticket #" + orden.getIdOrdenCompra();

        try {
            String html = String.format("<h2>Hola %s</h2><p>Tu compra con número de orden <strong>#%d</strong> "
                + "ha sido anulada.</p><p><strong>Motivo:</strong> %s</p><p>Se procesará el reembolso "
                + "correspondiente en los próximos días hábiles.</p>",
                nombreCliente, orden.getIdOrdenCompra(), motivo);
            boolean enviado = emailService.enviarEmailHtml(emailCliente, nombreCliente, asunto, html);

            // Notificación in-app vía NotificationManager
            NotificationRequest reqAnul = NotificationRequest.builder()
                .email(emailCliente)
                .nombre(nombreCliente)
                .notiTipo(TipoNotificacion.SISTEMA)
                .titulo("Compra anulada")
                .mensaje("Tu compra #" + orden.getIdOrdenCompra() + " ha sido anulada. Motivo: " + motivo)
                .build();
            notificationManager.notifyAllChannels(reqAnul);

            if (enviado) {
                log.info("Notificación de anulación enviada");
            }
        } catch (Exception e) {
            log.error("Error al enviar notificación de anulación: {}", e.getMessage());
        }
    }

    /**
     * Nuevo: Notifica cambio de contraseña (post cambio exitoso).
     */
    public void enviarNotificacionCambioContrasena(String email, String nombreCompleto) {
        log.info("📨 Enviando notificación de cambio de contraseña a: {}", email);
        String asunto = "Tu contraseña ha sido actualizada";
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("nombre", nombreCompleto);
            var plantilla = plantillaService.obtenerActiva(
                pe.edu.pucp.fasticket.model.notificaciones.TipoPlantilla.CAMBIO_CONTRASENA
            );
            String html = plantilla != null
                ? plantillaService.render(plantilla.getHtml(), params)
                : String.format("<h2>Hola %s</h2><p>Tu contraseña fue cambiada correctamente.</p>", nombreCompleto);
            if (plantilla != null) asunto = plantilla.getAsunto();

            emailService.enviarEmailHtml(email, nombreCompleto, asunto, html);
        } catch (Exception e) {
            log.error("Error al enviar notificación de cambio de contraseña: {}", e.getMessage());
        }
    }

    /**
     * Nuevo: Recordatorio de evento 48h antes.
     */
    public void enviarRecordatorioEvento48h(Evento evento, String emailCliente, String nombreCliente) {
        log.info("📨 Enviando recordatorio (48h) de evento a: {}", emailCliente);
        String asunto = "Recordatorio: tu evento inicia pronto";
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("nombre", nombreCliente);
            params.put("eventoNombre", evento.getNombre());
            var plantilla = plantillaService.obtenerActiva(
                pe.edu.pucp.fasticket.model.notificaciones.TipoPlantilla.RECORDATORIO_EVENTO_48H
            );
            String html = plantilla != null
                ? plantillaService.render(plantilla.getHtml(), params)
                : String.format("<h2>Hola %s</h2><p>Te recordamos que el evento %s inicia en 48 horas.</p>",
                    nombreCliente, evento.getNombre());
            if (plantilla != null) asunto = plantilla.getAsunto();

            emailService.enviarEmailHtml(emailCliente, nombreCliente, asunto, html);

            // Notificación in-app vía NotificationManager
            NotificationRequest reqRec48 = NotificationRequest.builder()
                .email(emailCliente)
                .nombre(nombreCliente)
                .notiTipo(TipoNotificacion.RECORDATORIO_EVENTO)
                .plantilla(TipoPlantilla.RECORDATORIO_EVENTO_48H)
                .params(params)
                .titulo("Recordatorio de evento")
                .mensaje("Faltan 48 horas para el evento " + evento.getNombre())
                .build();
            notificationManager.notifyAllChannels(reqRec48);
        } catch (Exception e) {
            log.error("Error al enviar recordatorio de evento: {}", e.getMessage());
        }
    }

    /**
     * Nuevo: Transferencia exitosa.
     */
    public void enviarTransferenciaExitosa(String emailDestinatario, String nombreDestinatario,
                                           String eventoNombre, Long ticketId) {
        log.info("📨 Enviando notificación de transferencia exitosa a: {}", emailDestinatario);
        String asunto = "Transferencia realizada";
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("nombre", nombreDestinatario);
            params.put("eventoNombre", eventoNombre);
            params.put("ticketId", ticketId);
            var plantilla = plantillaService.obtenerActiva(
                pe.edu.pucp.fasticket.model.notificaciones.TipoPlantilla.TRANSFERENCIA_OK
            );
            String html = plantilla != null
                ? plantillaService.render(plantilla.getHtml(), params)
                : String.format("<h2>Hola %s</h2><p>La transferencia del ticket %d para el evento %s fue exitosa.</p>",
                    nombreDestinatario, ticketId, eventoNombre);
            if (plantilla != null) asunto = plantilla.getAsunto();

            emailService.enviarEmailHtml(emailDestinatario, nombreDestinatario, asunto, html);

            // Notificación in-app vía NotificationManager
            NotificationRequest reqOk = NotificationRequest.builder()
                .email(emailDestinatario)
                .nombre(nombreDestinatario)
                .notiTipo(TipoNotificacion.TRANSFERENCIA_OK)
                .plantilla(TipoPlantilla.TRANSFERENCIA_OK)
                .params(params)
                .titulo("Transferencia realizada")
                .mensaje("Tu ticket " + ticketId + " fue transferido para el evento " + eventoNombre)
                .build();
            notificationManager.notifyAllChannels(reqOk);
        } catch (Exception e) {
            log.error("Error al enviar notificación de transferencia exitosa: {}", e.getMessage());
        }
    }

    /**
     * Nuevo: Transferencia fallida.
     */
    public void enviarTransferenciaFallida(String emailDestinatario, String nombreDestinatario,
                                           String eventoNombre, Long ticketId, String motivo) {
        log.info("📨 Enviando notificación de transferencia fallida a: {}", emailDestinatario);
        String asunto = "Transferencia no realizada";
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("nombre", nombreDestinatario);
            params.put("eventoNombre", eventoNombre);
            params.put("ticketId", ticketId);
            params.put("motivo", motivo);
            var plantilla = plantillaService.obtenerActiva(
                pe.edu.pucp.fasticket.model.notificaciones.TipoPlantilla.TRANSFERENCIA_FALLIDA
            );
            String html = plantilla != null
                ? plantillaService.render(plantilla.getHtml(), params)
                : String.format("<h2>Hola %s</h2><p>No se pudo realizar la transferencia del ticket %d para el evento %s. Motivo: %s.</p>",
                    nombreDestinatario, ticketId, eventoNombre, motivo);
            if (plantilla != null) asunto = plantilla.getAsunto();

            emailService.enviarEmailHtml(emailDestinatario, nombreDestinatario, asunto, html);

            // Notificación in-app vía NotificationManager
            NotificationRequest reqFail = NotificationRequest.builder()
                .email(emailDestinatario)
                .nombre(nombreDestinatario)
                .notiTipo(TipoNotificacion.TRANSFERENCIA_FALLIDA)
                .plantilla(TipoPlantilla.TRANSFERENCIA_FALLIDA)
                .params(params)
                .titulo("Transferencia no realizada")
                .mensaje("No se pudo transferir el ticket " + ticketId + " para el evento " + eventoNombre + ". Motivo: " + motivo)
                .build();
            notificationManager.notifyAllChannels(reqFail);
        } catch (Exception e) {
            log.error("Error al enviar notificación de transferencia fallida: {}", e.getMessage());
        }
    }

    // Métodos de construcción HTML eliminados por uso de PlantillaService con fallbacks simples.
}

