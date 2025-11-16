package pe.edu.pucp.fasticket.dto.compra;

import lombok.Data;

import java.util.List;

@Data
public class ListaSolicitudesDTO {
    private List<SolicitudTransferenciaDTO> solicitudesPendientes;
    private List<SolicitudTransferenciaDTO> solicitudesHistorial;
    private Integer totalPendientes;
}