package pe.edu.pucp.fasticket.dto;

import pe.edu.pucp.fasticket.model.usuario.Persona;

public class LoginResponse {
    private boolean ok;
    private String mensaje;
    private Persona persona;

    // Constructor vacío
    public LoginResponse() {
    }

    // Constructor con parámetros
    public LoginResponse(boolean ok, String mensaje, Persona persona) {
        this.ok = ok;
        this.mensaje = mensaje;
        this.persona = persona;
    }

    // Getters y Setters
    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Persona getPersona() {
        return persona;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }
}