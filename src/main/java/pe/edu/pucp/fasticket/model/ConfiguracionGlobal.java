package pe.edu.pucp.fasticket.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "configuracion_global")
@Data
@NoArgsConstructor
public class ConfiguracionGlobal {

    @Id
    @Column(name = "configKey", length = 50, nullable = false, updatable = false)
    private String key;

    @Column(name = "configValue", columnDefinition = "TEXT")
    private String value;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "valueType", length = 20)
    private String valueType;

    @Column(name = "createdAt", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helpers de parseo
    public Integer getValueAsInteger() {
        if (this.value == null) return null;
        try { return Integer.valueOf(this.value.trim()); } catch (Exception e) { return null; }
    }

    public Double getValueAsDouble() {
        if (this.value == null) return null;
        try { return Double.valueOf(this.value.trim()); } catch (Exception e) { return null; }
    }

    public Integer getValueAsMinutes() {
        return getValueAsInteger();
    }

    // Convenience setters que actualizan valueType
    public void setValue(String v) {
        this.value = v == null ? null : v;
        this.valueType = v == null ? null : "STRING";
    }

    public void setValue(Double v) {
        this.value = v == null ? null : v.toString();
        this.valueType = v == null ? null : "DOUBLE";
    }

    // Lombok generará setValue(String) por defecto; dejamos que use valueType "STRING" al asignar desde service.
}