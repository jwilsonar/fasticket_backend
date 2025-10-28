package pe.edu.pucp.fasticket.services.eventos;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.eventos.ActualizarTipoTicketRequestDTO;
import pe.edu.pucp.fasticket.dto.eventos.CrearTipoTicketRequestDTO;
import pe.edu.pucp.fasticket.dto.eventos.TipoTicketDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.mapper.TipoTicketMapper;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.ZonaRepositorio;

@Service
@RequiredArgsConstructor
@Slf4j
public class TipoTicketServicio {

    private final TipoTicketRepositorio tipoTicketRepositorio;
    private final ZonaRepositorio zonaRepositorio;
    private final TipoTicketMapper tipoTicketMapper;

    public List<TipoTicketDTO> listarTodos() {
        log.info("Listando todos los tipos de ticket");
        return tipoTicketRepositorio.findAll()
                .stream()
                .map(tipoTicketMapper::toDTO)
                .toList();
    }

    public List<TipoTicketDTO> listarPorZona(Integer idZona) {
        log.info("Listando tipos de ticket para zona: {}", idZona);
        return tipoTicketRepositorio.findByZonaIdZona(idZona)
                .stream()
                .map(tipoTicketMapper::toDTO)
                .toList();
    }

    public TipoTicketDTO obtenerPorId(Integer id) {
        log.info("Obteniendo tipo de ticket con ID: {}", id);
        TipoTicket tipoTicket = tipoTicketRepositorio.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado con ID: " + id));
        return tipoTicketMapper.toDTO(tipoTicket);
    }

    @Transactional
    public TipoTicketDTO crear(CrearTipoTicketRequestDTO dto) {
        log.info("Creando tipo de ticket: {} para zona: {}", dto.getNombre(), dto.getIdZona());
        
        // Validar que la zona existe
        Zona zona = zonaRepositorio.findById(dto.getIdZona())
                .orElseThrow(() -> new ResourceNotFoundException("Zona no encontrada con ID: " + dto.getIdZona()));
        
        // Validar que la zona esté activa
        if (!zona.getActivo()) {
            throw new BusinessException("No se puede crear tipo de ticket para una zona inactiva");
        }
        
        // Validar que el stock no exceda el aforo de la zona
        if (dto.getStock() > zona.getAforoMax()) {
            throw new BusinessException("El stock no puede exceder el aforo máximo de la zona (" + zona.getAforoMax() + ")");
        }
        
        // Validar que no exista otro tipo de ticket con el mismo nombre en la misma zona
        if (tipoTicketRepositorio.existsByNombreAndZonaIdZona(dto.getNombre(), dto.getIdZona())) {
            throw new BusinessException("Ya existe un tipo de ticket con el nombre '" + dto.getNombre() + "' en esta zona");
        }
        
        // Crear el tipo de ticket
        TipoTicket tipoTicket = new TipoTicket();
        tipoTicket.setZona(zona);
        tipoTicket.setNombre(dto.getNombre());
        tipoTicket.setDescripcion(dto.getDescripcion());
        tipoTicket.setPrecio(dto.getPrecio());
        tipoTicket.setStock(dto.getStock());
        tipoTicket.setCantidadDisponible(dto.getStock());
        tipoTicket.setCantidadVendida(0);
        tipoTicket.setActivo(true);
        tipoTicket.setLimitePorPersona(dto.getLimitePorPersona());
        
        TipoTicket guardado = tipoTicketRepositorio.save(tipoTicket);
        log.info("Tipo de ticket creado exitosamente con ID: {}", guardado.getIdTipoTicket());
        
        return tipoTicketMapper.toDTO(guardado);
    }

    @Transactional
    public TipoTicketDTO actualizar(Integer id, ActualizarTipoTicketRequestDTO dto) {
        log.info("Actualizando tipo de ticket con ID: {}", id);
        
        TipoTicket tipoTicket = tipoTicketRepositorio.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado con ID: " + id));
        
        // Validar que el stock no exceda el aforo de la zona
        if (dto.getStock() > tipoTicket.getZona().getAforoMax()) {
            throw new BusinessException("El stock no puede exceder el aforo máximo de la zona (" + tipoTicket.getZona().getAforoMax() + ")");
        }
        
        // Validar que no exista otro tipo de ticket con el mismo nombre en la misma zona
        if (!tipoTicket.getNombre().equals(dto.getNombre()) && 
            tipoTicketRepositorio.existsByNombreAndZonaIdZona(dto.getNombre(), tipoTicket.getZona().getIdZona())) {
            throw new BusinessException("Ya existe un tipo de ticket con el nombre '" + dto.getNombre() + "' en esta zona");
        }
        
        // Actualizar campos
        tipoTicket.setNombre(dto.getNombre());
        tipoTicket.setDescripcion(dto.getDescripcion());
        tipoTicket.setPrecio(dto.getPrecio());
        tipoTicket.setLimitePorPersona(dto.getLimitePorPersona());
        
        // Actualizar stock y cantidad disponible
        int diferenciaStock = dto.getStock() - tipoTicket.getStock();
        tipoTicket.setStock(dto.getStock());
        tipoTicket.setCantidadDisponible(tipoTicket.getCantidadDisponible() + diferenciaStock);
        
        TipoTicket actualizado = tipoTicketRepositorio.save(tipoTicket);
        log.info("Tipo de ticket actualizado exitosamente");
        
        return tipoTicketMapper.toDTO(actualizado);
    }

    @Transactional
    public void eliminar(Integer id) {
        log.info("Eliminando tipo de ticket con ID: {}", id);
        
        TipoTicket tipoTicket = tipoTicketRepositorio.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado con ID: " + id));
        
        // Validar que no tenga tickets vendidos
        if (tipoTicket.getCantidadVendida() > 0) {
            throw new BusinessException("No se puede eliminar un tipo de ticket que ya tiene tickets vendidos");
        }
        
        tipoTicketRepositorio.delete(tipoTicket);
        log.info("Tipo de ticket eliminado exitosamente");
    }

    @Transactional
    public void desactivar(Integer id) {
        log.info("Desactivando tipo de ticket con ID: {}", id);
        
        TipoTicket tipoTicket = tipoTicketRepositorio.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado con ID: " + id));
        
        tipoTicket.setActivo(false);
        tipoTicketRepositorio.save(tipoTicket);
        log.info("Tipo de ticket desactivado exitosamente");
    }
}
