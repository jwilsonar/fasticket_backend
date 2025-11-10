package pe.edu.pucp.fasticket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.pucp.fasticket.dto.auditoria.ErrorLogDTO;
import pe.edu.pucp.fasticket.model.auditoria.ErrorLog;

@Mapper(componentModel = "spring")
public interface ErrorLogMapper {

    @Mapping(source = "administrador.nombres", target = "nombreAdmin") // Asume que Administrador tiene 'nombres'
    ErrorLogDTO toDTO(ErrorLog errorLog);
}