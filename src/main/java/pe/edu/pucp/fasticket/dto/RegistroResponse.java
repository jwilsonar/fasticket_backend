package pe.edu.pucp.fasticket.dto;

public class RegistroResponse {
    private String email;
    private String mensaje;
    private boolean exito;

    // Constructor vacío
    public RegistroResponse() {
    }

    // Constructor con parámetros
    public RegistroResponse(String email, String mensaje, boolean exito) {
        this.email = email;
        this.mensaje = mensaje;
        this.exito = exito;
    }

    // Getters y Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public boolean isExito() {
        return exito;
    }

    public void setExito(boolean exito) {
        this.exito = exito;
    }
}