package pe.edu.pucp.fasticket.services.notificaciones;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.Evento;

/**
 * Servicio de notificaciones que orquesta el envío de diferentes tipos de emails.
 * 
 * Implementa los requerimientos:
 * - RF-048: Verificación de cuenta
 * - RF-049: Confirmación de compra
 * - RF-050: Recordatorios de evento (TODO: implementar con scheduler)
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

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * RF-048: Envía email de verificación de cuenta.
     */
    public void enviarEmailVerificacion(String email, String nombreCompleto, String token) {
        log.info("Enviando email de verificación a: {}", email);

        String asunto = "Verifica tu cuenta en Fasticket";
        String linkVerificacion = frontendUrl + "/verificar-cuenta?token=" + token;

        Map<String, Object> params = new HashMap<>();
        params.put("nombre", nombreCompleto);
        params.put("linkVerificacion", linkVerificacion);

        try {
            boolean enviado = emailService.enviarEmailHtml(
                email,
                nombreCompleto,
                asunto,
                construirHtmlVerificacion(nombreCompleto, linkVerificacion)
            );

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
     * RF-049, RF-086: Envía confirmación de compra con detalles.
     */
    public void enviarConfirmacionCompra(OrdenCompra orden, String emailCliente, String nombreCliente) {
        log.info("Enviando confirmación de compra a: {}", emailCliente);

        String asunto = "Confirmación de Compra - Fasticket #" + orden.getIdOrdenCompra();

        try {
            boolean enviado = emailService.enviarEmailHtml(
                emailCliente,
                nombreCliente,
                asunto,
                construirHtmlConfirmacionCompra(orden, nombreCliente)
            );

            if (enviado) {
                log.info("Confirmación de compra enviada exitosamente");
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
            boolean enviado = emailService.enviarEmailHtml(
                emailCliente,
                nombreCliente,
                asunto,
                construirHtmlEventoCancelado(evento, nombreCliente, motivo)
            );

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
            boolean enviado = emailService.enviarEmailHtml(
                email,
                nombreCompleto,
                asunto,
                construirHtmlRecuperacionContrasena(nombreCompleto, linkRecuperacion)
            );

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
            boolean enviado = emailService.enviarEmailHtml(
                emailCliente,
                nombreCliente,
                asunto,
                construirHtmlCompraAnulada(orden, nombreCliente, motivo)
            );

            if (enviado) {
                log.info("Notificación de anulación enviada");
            }
        } catch (Exception e) {
            log.error("Error al enviar notificación de anulación: {}", e.getMessage());
        }
    }

    // ===== TEMPLATES HTML =====

    private String construirHtmlVerificacion(String nombre, String link) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; }
                    .button { display: inline-block; padding: 15px 30px; background: #667eea; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Bienvenido a Fasticket</h1>
                    </div>
                    <div class="content">
                        <h2>¡Hola %s!</h2>
                        <p>Gracias por registrarte en Fasticket. Para activar tu cuenta, por favor verifica tu dirección de correo electrónico.</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Verificar mi Cuenta</a>
                        </p>
                        <p><small>Si el botón no funciona, copia y pega este enlace en tu navegador:<br>%s</small></p>
                        <p>Este enlace expirará en 24 horas.</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Fasticket. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """, nombre, link, link);
    }

    private String construirHtmlConfirmacionCompra(OrdenCompra orden, String nombre) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String fecha = orden.getFechaOrden() != null ? orden.getFechaOrden().format(formatter) : "";

        StringBuilder itemsHtml = new StringBuilder();
        orden.getItems().forEach(item -> {
            itemsHtml.append(String.format("""
                <tr>
                    <td>%s</td>
                    <td style="text-align: center;">%d</td>
                    <td style="text-align: right;">S/ %.2f</td>
                </tr>
                """, 
                item.getTipoTicket().getNombre(),
                item.getCantidad(),
                item.getPrecioFinal()
            ));
        });

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #28a745; color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; }
                    table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                    th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
                    th { background: #f0f0f0; }
                    .total { font-size: 18px; font-weight: bold; color: #28a745; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>¡Compra Confirmada!</h1>
                    </div>
                    <div class="content">
                        <h2>¡Hola %s!</h2>
                        <p>Tu compra ha sido confirmada exitosamente.</p>
                        <p><strong>Número de Orden:</strong> #%d</p>
                        <p><strong>Fecha:</strong> %s</p>
                        
                        <h3>Detalle de tu Compra:</h3>
                        <table>
                            <thead>
                                <tr>
                                    <th>Ticket</th>
                                    <th style="text-align: center;">Cantidad</th>
                                    <th style="text-align: right;">Precio</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                            <tfoot>
                                <tr>
                                    <td colspan="2"><strong>TOTAL:</strong></td>
                                    <td class="total" style="text-align: right;">S/ %.2f</td>
                                </tr>
                            </tfoot>
                        </table>
                        
                        <p>📱 Tus tickets están disponibles en tu cuenta. Recuerda llevarlos el día del evento.</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Fasticket. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            nombre, 
            orden.getIdOrdenCompra(), 
            fecha, 
            itemsHtml.toString(), 
            orden.getTotal()
        );
    }

    private String construirHtmlEventoCancelado(Evento evento, String nombre, String motivo) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #dc3545; color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; }
                    .alert { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Evento Cancelado</h1>
                    </div>
                    <div class="content">
                        <h2>Hola %s,</h2>
                        <p>Lamentamos informarte que el siguiente evento ha sido cancelado:</p>
                        
                        <div class="alert">
                            <h3>%s</h3>
                            <p><strong>Motivo:</strong> %s</p>
                        </div>
                        
                        <p>Se procesará automáticamente el reembolso de tu compra en los próximos 5-7 días hábiles.</p>
                        <p>Disculpa las molestias ocasionadas. Esperamos verte pronto en otros eventos.</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Fasticket. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """, nombre, evento.getNombre(), motivo);
    }

    private String construirHtmlRecuperacionContrasena(String nombre, String link) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #6c757d; color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; }
                    .button { display: inline-block; padding: 15px 30px; background: #6c757d; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .warning { background: #f8d7da; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Recuperación de Contraseña</h1>
                    </div>
                    <div class="content">
                        <h2>Hola %s,</h2>
                        <p>Hemos recibido una solicitud para restablecer tu contraseña. Haz clic en el botón de abajo para continuar:</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Restablecer Contraseña</a>
                        </p>
                        <div class="warning">
                            <p><strong>Importante:</strong></p>
                            <ul>
                                <li>Este enlace expirará en 1 hora</li>
                                <li>Si no solicitaste este cambio, ignora este correo</li>
                            </ul>
                        </div>
                        <p><small>Enlace: %s</small></p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Fasticket. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """, nombre, link, link);
    }

    private String construirHtmlCompraAnulada(OrdenCompra orden, String nombre, String motivo) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #ffc107; color: #333; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; }
                    .alert { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Compra Anulada</h1>
                    </div>
                    <div class="content">
                        <h2>Hola %s,</h2>
                        <p>Tu compra con número de orden <strong>#%d</strong> ha sido anulada.</p>
                        
                        <div class="alert">
                            <p><strong>Motivo:</strong> %s</p>
                        </div>
                        
                        <p>Se procesará el reembolso correspondiente en los próximos días hábiles.</p>
                        <p>Si tienes alguna duda, contáctanos a soporte@fasticket.com</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Fasticket. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """, nombre, orden.getIdOrdenCompra(), motivo);
    }
}

