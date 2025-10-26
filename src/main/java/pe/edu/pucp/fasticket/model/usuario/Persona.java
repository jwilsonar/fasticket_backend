package pe.edu.pucp.fasticket.model.usuario;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.geografia.Distrito;

@Data
@NoArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "Persona")
public class Persona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPersona")
    private Integer idPersona;
    
    @Column(name = "tipoDocumento", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoDocumento;
    
    @Column(name = "docIdentidad", nullable = false, unique = true, length = 20)
    private String docIdentidad;
    
    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;
    
    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;
    
    @Column(name = "telefono", length = 20)
    private String telefono;
    
    @Column(name = "email", unique = true, length = 150)
    private String email;
    
    @Column(name = "fechaNacimiento")
    private LocalDate fechaNacimiento;
    
    @Column(name = "direccion", length = 200)
    private String direccion;

    /**
     * Calcula la edad actual de la persona.
     * Usado para validar RF-072: restricción de edad mínima en eventos.
     * 
     * @return Edad en años, o null si no tiene fecha de nacimiento
     */
    public Integer calcularEdad() {
        if (fechaNacimiento == null) {
            return null;
        }
        return java.time.Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }
    
    @Column(name = "contrasenia", nullable = false, length = 200)
    private String contrasena;
    
    @Column(name = "rol", nullable = false)
    @Enumerated(EnumType.STRING)
    private Rol rol;
    
    @Column(name = "activo")
    private Boolean activo = true;
    
    @JsonProperty("usuario_creacion")
    @Column(name = "usuarioCreacion")
    private Integer usuarioCreacion;
    
    @JsonProperty("fecha_creacion")
    @Column(name = "fechaCreacion")
    private LocalDate fechaCreacion;
    
    @JsonProperty("usuario_actualizacion")
    @Column(name = "usuarioActualizacion")
    private Integer usuarioActualizacion;
    
    @JsonProperty("fecha_actualizacion")
    @Column(name = "fechaActualizacion")
    private LocalDate fechaActualizacion;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idDistrito")
    private Distrito distrito;
    
    // Setter explícito para rol (usado en constructores de subclases)
    public void setRol(Rol rol) {
        this.rol = rol;
    }
}
