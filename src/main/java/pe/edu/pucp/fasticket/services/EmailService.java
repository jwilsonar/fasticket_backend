package pe.edu.pucp.fasticket.services;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import pe.edu.pucp.fasticket.model.ConfiguracionGlobal;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.ConfiguracionRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final ConfiguracionRepository configuracionRepository;

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
    public void enviarCorreoConfirmacionCompra(OrdenCompra orden) {
        String eventoNombre = orden.getItems().get(0).getTipoTicket().getEvento().getNombre();

        String asunto = obtenerValorConfig("EMAIL_COMPRA_ASUNTO", "Confirmación de tu compra");
        String cuerpo = obtenerValorConfig("EMAIL_COMPRA_CUERPO", "¡Tu compra fue exitosa!");

        // Reemplazar placeholders
        asunto = asunto.replace("${eventoNombre}", eventoNombre);
        cuerpo = cuerpo.replace("${nombreUsuario}", orden.getCliente().getNombres());
        // Aquí deberías generar el QR o adjuntar los tickets
        cuerpo = cuerpo.replace("${codigoQr}", "[Contenido del Ticket/QR]");

        enviarEmail(orden.getCliente().getEmail(), asunto, cuerpo, true);
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
     */
    private void enviarEmail(String para, String asunto, String cuerpo, boolean esHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("no-reply@fasticket.com"); // Puedes ponerlo en properties
            helper.setTo(para);
            helper.setSubject(asunto);
            helper.setText(cuerpo, esHtml); // true para que interprete el HTML

            mailSender.send(message);
            log.info("Correo enviado exitosamente a: {}", para);

        } catch (Exception e) {
            log.error("Error al enviar correo a: {}. Causa: {}", para, e.getMessage());
            // No lanzamos excepción para no detener el flujo principal
        }
    }

    /**
     * NUEVO Motor de envío para múltiples destinatarios en Copia Oculta (BCC)
     */
    private void enviarEmailBcc(String[] paraBcc, String asunto, String cuerpo, boolean esHtml) {
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
    private String obtenerValorConfig(String key, String valorPorDefecto) {
        return configuracionRepository.findById(key)
                .map(ConfiguracionGlobal::getValue)
                .orElse(valorPorDefecto);
    }
}