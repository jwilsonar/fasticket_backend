package pe.edu.pucp.fasticket.dto.compra;
import lombok.Data;

@Data
public class CrearSolicitudTransferenciaDTO {
    private Integer idTicket;
    private String emailReceptor;
    private String nombreCompletoReceptor;
    private String numeroDocumentoReceptor;
    private String telefonoReceptor;
    private String mensaje; // Opcional
}
