package pe.edu.pucp.fasticket.dto.compra;

import lombok.Data;
import java.time.LocalDate;

@Data
public class VerificarTransferenciaResponseDTO {

    private String nombreEvento;
    private LocalDate fechaEvento;
    private String nombreDestinatario;
    private String emailDestinatario;
    private Integer transferenciasRestantes;
    private Integer horasCooldown;
}
