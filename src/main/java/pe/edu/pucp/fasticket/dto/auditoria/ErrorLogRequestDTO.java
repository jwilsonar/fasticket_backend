package pe.edu.pucp.fasticket.dto.auditoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

// Este DTO representa la solicitud POST que viene del formulario
@Getter
@Setter
public class ErrorLogRequestDTO {

    @NotNull(message = "La fecha y hora no pueden ser nulas")
    private LocalDateTime fechaHora;

    @NotBlank(message = "La severidad no puede estar vacía")
    @Size(max = 50)
    private String severidad;

    @NotBlank(message = "El módulo no puede estar vacío")
    @Size(max = 100)
    private String modulo;

    @NotBlank(message = "El mensaje breve no puede estar vacío")
    @Size(max = 255)
    private String mensajeBreve;

    @NotBlank(message = "El detalle técnico no puede estar vacío")
    private String detalleTecnico;

    @NotBlank(message = "La traza no puede estar vacía")
    private String traza;
}