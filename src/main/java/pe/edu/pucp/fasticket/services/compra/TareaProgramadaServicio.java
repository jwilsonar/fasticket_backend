package pe.edu.pucp.fasticket.services.compra;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.model.compra.EstadoCompra;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.EstadoTicket;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TareaProgramadaServicio {

    private final OrdenCompraRepositorio ordenCompraRepositorio;
    public TareaProgramadaServicio(OrdenCompraRepositorio ordenCompraRepositorio) {
        this.ordenCompraRepositorio = ordenCompraRepositorio;
    }
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void liberarOrdenesExpiradas() {
        List<OrdenCompra> ordenesExpiradas = ordenCompraRepositorio.findByEstadoAndFechaExpiracionBefore(EstadoCompra.PENDIENTE, LocalDateTime.now());
        for (OrdenCompra orden : ordenesExpiradas) {
            orden.setEstado(EstadoCompra.RECHAZADO);
            orden.getItems().forEach(item -> {
                item.getTickets().forEach(ticket -> {
                    ticket.setEstado(EstadoTicket.DISPONIBLE);
                    ticket.setItemCarrito(null);
                });
            });
            ordenCompraRepositorio.save(orden);
            System.out.println("Orden " + orden.getIdOrdenCompra() + " expirada y tickets liberados.");
        }
    }
}