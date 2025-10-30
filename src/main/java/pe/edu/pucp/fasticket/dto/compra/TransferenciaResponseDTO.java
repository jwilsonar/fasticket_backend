package pe.edu.pucp.fasticket.dto.compra;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TransferenciaResponseDTO {
    private Integer idHistorial;
    private Integer idTicket;
    private String nombreEmisor;
    private String emailEmisor;
    private String nombreReceptor;
    private String emailReceptor;
    private LocalDateTime fechaTransferencia;
}
