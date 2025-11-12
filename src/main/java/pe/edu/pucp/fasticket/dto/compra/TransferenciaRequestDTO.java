package pe.edu.pucp.fasticket.dto.compra;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TransferenciaRequestDTO {

    @NotBlank(message = "El email del receptor es obligatorio")
    @Email(message = "El email del receptor no es válido")
    private String emailReceptor;

    // No necesitamos el ID del ticket en el body,
    // lo pondremos en la URL (ej: /api/v1/tickets/{idTicket}/transferir)
}