package pe.edu.pucp.fasticket.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ValidacionResponseDTO {

    private boolean valido;
    private String mensaje;

    // Info para mostrar al staff
    private String nombreEvento;
    private String tipoTicket;
    private String nombreAsistente;
    private String documentoAsistente;

    // Constructor para respuesta exitosa
    public ValidacionResponseDTO(String mensaje, String evento, String ticket, String asistente, String doc) {
        this.valido = true;
        this.mensaje = mensaje;
        this.nombreEvento = evento;
        this.tipoTicket = ticket;
        this.nombreAsistente = asistente;
        this.documentoAsistente = doc;
    }

    // Constructor para respuesta fallida
    public ValidacionResponseDTO(String mensaje) {
        this.valido = false;
        this.mensaje = mensaje;
    }
}