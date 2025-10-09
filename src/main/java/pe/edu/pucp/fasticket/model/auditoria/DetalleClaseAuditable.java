package pe.edu.pucp.fasticket.model.auditoria;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

// crea la clase DetalleClaseAuditable
@Data
@NoArgsConstructor
@Entity
@Table(name = "DetalleClaseAuditable")
public class DetalleClaseAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDetalleClaseAuditable")
    private Integer idDetalleClaseAuditable;

    @Column(name = "usuarioCreacion", nullable = false)
    private Integer usuarioCreacion;

    @Column(name = "fechaCreacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "usuarioActualizacion", nullable = true)
    private Integer usuarioActualizacion;

    @Column(name = "fechaActualizacion", nullable = true)
    private LocalDateTime fechaActualizacion;
}
