package pe.edu.pucp.fasticket.listeners;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.events.ClienteRegistradoEvent;
import pe.edu.pucp.fasticket.events.CompraAnuladaEvent;
import pe.edu.pucp.fasticket.events.CompraConfirmadaEvent;
import pe.edu.pucp.fasticket.events.EventoCanceladoEvent;
import pe.edu.pucp.fasticket.events.RecuperacionContrasenaEvent;
import pe.edu.pucp.fasticket.services.notificaciones.NotificacionService;

/**
 * Listener que implementa el Patrón Observer para escuchar eventos de dominio.
 * 
 * Este componente se suscribe a los eventos publicados en la aplicación y
 * desencadena las notificaciones correspondientes de forma ASÍNCRONA.
 * 
 * VENTAJAS DE ESTE PATRÓN:
 * 1. Desacoplamiento: Los servicios de negocio no conocen el sistema de notificaciones
 * 2. Robustez: Los errores en notificaciones NO afectan el flujo principal
 * 3. Escalabilidad: Procesamiento asíncrono no bloquea las transacciones
 * 4. Extensibilidad: Fácil añadir nuevos listeners sin modificar código existente
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacionListener {

    private final NotificacionService notificacionService;

    /**
     * RF-048: Escucha eventos de registro de cliente.
     * 
     * @Async: Se ejecuta en un hilo separado, no bloquea el registro del usuario.
     * Si falla el envío del email, el usuario ya está registrado.
     */
    @EventListener
    @Async("taskExecutor")
    public void handleClienteRegistrado(ClienteRegistradoEvent event) {
        log.info("🔔 [OBSERVER] Evento recibido: ClienteRegistradoEvent para {}", event.getEmail());
        
        try {
            notificacionService.enviarEmailVerificacion(
                event.getEmail(),
                event.getNombreCompleto(),
                event.getTokenVerificacion()
            );
        } catch (Exception e) {
            // CRÍTICO: Capturamos cualquier excepción para evitar que rompa el flujo
            log.error("❌ Error en listener de cliente registrado (no crítico): {}", e.getMessage(), e);
        }
    }

    /**
     * RF-049, RF-086: Escucha eventos de compra confirmada.
     * 
     * Envía confirmación con detalles de la compra y los tickets.
     */
    @EventListener
    @Async("taskExecutor")
    public void handleCompraConfirmada(CompraConfirmadaEvent event) {
        log.info("🔔 [OBSERVER] Evento recibido: CompraConfirmadaEvent para orden #{}", 
                 event.getOrden().getIdOrdenCompra());
        
        try {
            String nombreCliente = event.getOrden().getCliente().getNombres() + " " + 
                                  event.getOrden().getCliente().getApellidos();
            
            notificacionService.enviarConfirmacionCompra(
                event.getOrden(),
                event.getEmailCliente(),
                nombreCliente
            );
        } catch (Exception e) {
            log.error("❌ Error en listener de compra confirmada (no crítico): {}", e.getMessage(), e);
        }
    }

    /**
     * RF-016: Escucha eventos de cancelación de evento.
     * 
     * Notifica a TODOS los clientes que compraron tickets para ese evento.
     */
    @EventListener
    @Async("taskExecutor")
    public void handleEventoCancelado(EventoCanceladoEvent event) {
        log.info("🔔 [OBSERVER] Evento recibido: EventoCanceladoEvent - {}", 
                 event.getEvento().getNombre());
        
        try {
            // Enviar email a cada cliente afectado
            for (String email : event.getEmailsAfectados()) {
                try {
                    // Aquí idealmente obtendríamos el nombre del cliente
                    notificacionService.enviarNotificacionEventoCancelado(
                        event.getEvento(),
                        email,
                        "Cliente", // TODO: Obtener nombre real del cliente
                        event.getMotivoCancelacion()
                    );
                } catch (Exception e) {
                    log.error("❌ Error al enviar notificación a {}: {}", email, e.getMessage());
                    // Continuamos con el siguiente email
                }
            }
            
            log.info("✅ Notificaciones de cancelación enviadas a {} clientes", 
                     event.getEmailsAfectados().size());
        } catch (Exception e) {
            log.error("❌ Error en listener de evento cancelado: {}", e.getMessage(), e);
        }
    }

    /**
     * RF-052: Escucha eventos de recuperación de contraseña.
     */
    @EventListener
    @Async("taskExecutor")
    public void handleRecuperacionContrasena(RecuperacionContrasenaEvent event) {
        log.info("🔔 [OBSERVER] Evento recibido: RecuperacionContrasenaEvent para {}", 
                 event.getEmail());
        
        try {
            notificacionService.enviarRecuperacionContrasena(
                event.getEmail(),
                event.getNombreCompleto(),
                event.getTokenRecuperacion()
            );
        } catch (Exception e) {
            log.error("❌ Error en listener de recuperación de contraseña: {}", e.getMessage(), e);
        }
    }

    /**
     * RF-089: Escucha eventos de compra anulada.
     */
    @EventListener
    @Async("taskExecutor")
    public void handleCompraAnulada(CompraAnuladaEvent event) {
        log.info("🔔 [OBSERVER] Evento recibido: CompraAnuladaEvent para orden #{}", 
                 event.getOrden().getIdOrdenCompra());
        
        try {
            String nombreCliente = event.getOrden().getCliente().getNombres() + " " + 
                                  event.getOrden().getCliente().getApellidos();
            
            notificacionService.enviarNotificacionCompraAnulada(
                event.getOrden(),
                event.getEmailCliente(),
                nombreCliente,
                event.getMotivoAnulacion()
            );
        } catch (Exception e) {
            log.error("❌ Error en listener de compra anulada: {}", e.getMessage(), e);
        }
    }
}

