package pe.edu.pucp.fasticket.dto;

import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;
import pe.edu.pucp.fasticket.model.usuario.Rol;

import java.time.LocalDate;

public class RegistroRequest {
    private String docIdentidad;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String email;
    private String direccion;
    private String contrasena;
    private LocalDate fechaNacimiento;
    private TipoDocumento tipoDocumento;
    private Rol rol;

    // Constructor vacío
    public RegistroRequest() {
    }

    // Constructor con parámetros principales
    public RegistroRequest(String docIdentidad, String nombres, String apellidos, String telefono, 
                          String email, String direccion, String contrasena, LocalDate fechaNacimiento, 
                          TipoDocumento tipoDocumento, Rol rol) {
        this.docIdentidad = docIdentidad;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
        this.contrasena = contrasena;
        this.fechaNacimiento = fechaNacimiento;
        this.tipoDocumento = tipoDocumento;
        this.rol = rol;
    }

    // Getters y Setters
    public String getDocIdentidad() {
        return docIdentidad;
    }

    public void setDocIdentidad(String docIdentidad) {
        this.docIdentidad = docIdentidad;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }
}