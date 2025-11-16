package pe.edu.pucp.fasticket.dto.compra;

import lombok.Data;
import pe.edu.pucp.fasticket.model.compra.EstadoSolicitud;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SolicitudTransferenciaDTO {
    private Integer idSolicitud;
    private Integer idTicket;
    private String codigoTicket;
    private String nombreEvento;
    private LocalDate fechaEvento;

    private Integer idEmisor;
    private String nombreEmisor;
    private String emailEmisor;

    private Integer idReceptor;
    private String nombreReceptor;
    private String emailReceptor;

    private EstadoSolicitud estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaRespuesta;
    private LocalDateTime fechaExpiracion;
    private Long horasRestantes;

    private Integer transferenciasRestantes;
    private Boolean puedeTransferir;
}