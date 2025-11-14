package pe.edu.pucp.fasticket.dto.tickets;

import lombok.Data;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MisEntradasDTO {

    private Integer idTicket;
    private Integer transferenciasRestantes;
    private Integer idEvento;
    private String nombreEvento;
    private LocalDate fechaEvento;
    private LocalTime horaEvento;
    private String nombreLocal;
    private String nombreZona;
    private String nombreTipoTicket;
    private Double precioPagado;
    private String estado;
    private String nombreAsistente;
    private String apellidoAsistente;
    private String documentoAsistente;

    public MisEntradasDTO(Ticket ticket) {
        this.idTicket = ticket.getIdTicket();
        this.estado = ticket.getEstado().toString();
        this.precioPagado = ticket.getPrecio();
        this.nombreAsistente = ticket.getNombreAsistente();
        this.apellidoAsistente = ticket.getApellidoAsistente();
        this.documentoAsistente = ticket.getDocumentoAsistente();

        if (ticket.getTipoTicket() != null) {
            this.nombreTipoTicket = ticket.getTipoTicket().getNombre();
            if (ticket.getTipoTicket().getZona() != null) {
                this.nombreZona = ticket.getTipoTicket().getZona().getNombre();
            }
        }

        Evento evento = ticket.getEvento();
        if (evento != null) {
            this.idEvento = evento.getIdEvento();
            this.nombreEvento = evento.getNombre();
            this.fechaEvento = evento.getFechaEvento();
            this.horaEvento = evento.getHoraInicio();
            if (evento.getLocal() != null) {
                this.nombreLocal = evento.getLocal().getNombre();
            }
            if (evento.getMaxTransferenciasPermitidas() != null) {
                this.transferenciasRestantes = evento.getMaxTransferenciasPermitidas() - ticket.getContadorTransferencias();
            } else {
                this.transferenciasRestantes = 0;
            }
        }
    }
}