package pe.edu.pucp.fasticket.controllers.notificaciones;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pe.edu.pucp.fasticket.services.notificaciones.EmailService;

@RestController
@RequestMapping("/notificacion")
@RequiredArgsConstructor
public class NotificacionController {

    private final EmailService emailService;

    @GetMapping("/test")
    public String test() {
        // Enviar usando HTML directo
        emailService.enviarEmailHtml(
            "destino@example.com",
            "Usuario Destino",
            "Hola desde Brevo (HTML)",
            "<h1>Notificación enviada correctamente</h1>"
        );

        // Enviar usando plantilla (ejemplo con templateId=5 y parámetros)
        emailService.enviarEmail(
            "destino@example.com",
            "Usuario Destino",
            "Hola desde Brevo (Plantilla)",
            5L,
            Map.of("nombre", "Wilson", "codigo", "123456")
        );

        return "OK";
    }
}


