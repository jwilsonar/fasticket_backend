package pe.edu.pucp.fasticket.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "configuracion_global")
@Data
@NoArgsConstructor
public class ConfiguracionGlobal {

    /**
     * La clave única de la configuración.
     * Ej: "SISTEMA_MONEDA", "SISTEMA_ZONA_HORARIA", "POLITICA_LIMITE_COMPRA"
     */
    @Id
    @Column(name = "config_key", length = 50, nullable = false, updatable = false)
    private String key;

    /**
     * El valor de la configuración.
     * Ej: "PEN", "America/Lima", "10"
     */
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String value;

    /**
     * Descripción de para qué sirve esta clave (solo para el admin)
     */
    @Column(name = "descripcion", length = 255)
    private String descripcion;
}