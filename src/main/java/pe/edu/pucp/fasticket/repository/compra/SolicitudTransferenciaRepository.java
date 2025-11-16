package pe.edu.pucp.fasticket.repository.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.fasticket.model.compra.EstadoSolicitud;
import pe.edu.pucp.fasticket.model.compra.SolicitudTransferencia;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SolicitudTransferenciaRepository extends JpaRepository<SolicitudTransferencia, Integer> {

    List<SolicitudTransferencia> findByReceptor_IdPersonaAndEstadoAndActivoTrue(
            Integer idReceptor, EstadoSolicitud estado);

    List<SolicitudTransferencia> findByEmisor_IdPersonaAndActivoTrueOrderByFechaSolicitudDesc(
            Integer idEmisor);

    List<SolicitudTransferencia> findByTicket_IdTicketAndEstadoAndActivoTrue(
            Integer idTicket, EstadoSolicitud estado);

    Optional<SolicitudTransferencia> findByTicket_IdTicketAndReceptor_IdPersonaAndEstadoAndActivoTrue(
            Integer idTicket, Integer idReceptor, EstadoSolicitud estado);

    List<SolicitudTransferencia> findByEstadoAndFechaExpiracionBeforeAndActivoTrue(
            EstadoSolicitud estado, LocalDateTime fecha);
}