package pe.edu.pucp.fasticket.dto.tickets;

import lombok.Data;
import lombok.NoArgsConstructor;
// import pe.edu.pucp.fasticket.model.eventos.TipoEstadoTiket; // <--- LÍNEA ELIMINADA

@Data
@NoArgsConstructor
public class TicketDTO {

    private Integer idTicket;
    private String nombre;
    private Double precio;
    private Integer stock;

    // --- CORRECCIÓN AQUÍ ---
    // Cambiamos el tipo de Enum a String
    private String estado;
    // --- FIN DE LA CORRECCIÓN ---

    private String nombreZona;
}