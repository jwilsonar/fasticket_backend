package pe.edu.pucp.fasticket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pe.edu.pucp.fasticket.dto.auditoria.ErrorLogDTO;
import pe.edu.pucp.fasticket.dto.auditoria.ErrorLogDetalleDTO;
import pe.edu.pucp.fasticket.model.auditoria.ErrorLog;
import pe.edu.pucp.fasticket.model.usuario.Administrador;

@Mapper(componentModel = "spring")
public interface ErrorLogMapper {

    // Este mapeo le dice a MapStruct:
    // 1. Copia todos los campos con el mismo nombre (idError, fechaHora, etc.)
    // 2. Para el campo 'nombreAdmin' del DTO, usa el campo 'administrador' de la entidad
    //    y pásalo por el método 'adminToNombre'
    @Mapping(source = "administrador", target = "nombreAdmin", qualifiedByName = "adminToNombre")
    ErrorLogDTO toDTO(ErrorLog errorLog);

    @Mapping(source = "administrador", target = "nombreAdmin", qualifiedByName = "adminToNombre")
    ErrorLogDetalleDTO toDetalleDTO(ErrorLog errorLog);

    // Este es el método helper que convierte el objeto Administrador a un String
    @Named("adminToNombre")
    default String adminToNombre(Administrador admin) {
        if (admin == null) {
            // Si el error no está asociado a un admin (ej. un error del sistema)
            return "Sistema";
        }
        // Asumo que tu entidad Administrador tiene estos getters
        return admin.getNombres() + " " + admin.getApellidos();
    }
}