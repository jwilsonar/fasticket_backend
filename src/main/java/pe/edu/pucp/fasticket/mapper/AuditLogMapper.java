package pe.edu.pucp.fasticket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pe.edu.pucp.fasticket.dto.auditoria.AuditLogDTO;
import pe.edu.pucp.fasticket.model.auditoria.AuditLog;
import pe.edu.pucp.fasticket.model.usuario.Administrador;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    // Mapea el administrador a su email
    @Mapping(source = "administrador", target = "adminEmail", qualifiedByName = "adminToEmail")
    AuditLogDTO toDTO(AuditLog auditLog);

    // Método helper para obtener el email
    @Named("adminToEmail")
    default String adminToEmail(Administrador admin) {
        if (admin == null) {
            return "Sistema"; // O "N/A"
        }
        return admin.getEmail();
    }
}