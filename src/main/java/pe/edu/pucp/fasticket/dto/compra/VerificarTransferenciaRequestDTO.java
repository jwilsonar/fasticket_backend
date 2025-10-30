package pe.edu.pucp.fasticket.dto.compra;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerificarTransferenciaRequestDTO {

    @NotNull(message = "El ID del ticket es obligatorio")
    private Integer idTicket;

    @NotBlank(message = "El email del destinatario es obligatorio")
    @Email(message = "El email no es válido")
    private String emailDestinatario;

    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombreCompletoDestinatario;

    @NotBlank(message = "El número de documento es obligatorio")
    private String numeroDocumentoDestinatario;

    @NotBlank(message = "El teléfono es obligatorio")
    private String telefonoDestinatario;
}