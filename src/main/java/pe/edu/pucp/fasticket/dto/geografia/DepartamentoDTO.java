package pe.edu.pucp.fasticket.dto.geografia;

import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.geografia.Departamento;

@Data
@NoArgsConstructor
public class DepartamentoDTO {
    private Integer idDepartamento;
    private String nombre;

    public DepartamentoDTO(Departamento departamento) {
        this.idDepartamento = departamento.getIdDepartamento();
        this.nombre = departamento.getNombre();
    }
}