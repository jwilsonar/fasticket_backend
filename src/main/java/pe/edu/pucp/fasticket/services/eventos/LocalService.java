package pe.edu.pucp.fasticket.services.eventos;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.eventos.LocalCreateDTO;
import pe.edu.pucp.fasticket.dto.eventos.LocalResponseDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.mapper.LocalMapper;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.geografia.Distrito;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.geografia.DistritoRepository;

/**
 * Servicio para la gestión de locales.
 * Implementa lógica de negocio y validaciones.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LocalService {

    private final LocalesRepositorio localRepository;
    private final DistritoRepository distritoRepository;
    private final LocalMapper localMapper;

    public List<LocalResponseDTO> listarTodos() {
        return localRepository.findAll().stream()
                .map(localMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<LocalResponseDTO> listarActivos() {
        return localRepository.findByActivoTrue().stream()
                .map(localMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public LocalResponseDTO obtenerPorId(Integer id) {
        Local local = localRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + id));
        return localMapper.toResponseDTO(local);
    }

    @Transactional
    public LocalResponseDTO crear(LocalCreateDTO dto) {
        log.info("Creando nuevo local: {}", dto.getNombre());

        // Validar nombre único
        if (localRepository.existsByNombreIgnoreCase(dto.getNombre())) {
            throw new BusinessException("Ya existe un local con el nombre: " + dto.getNombre());
        }

        // Obtener distrito
        Distrito distrito = null;
        if (dto.getIdDistrito() != null) {
            distrito = distritoRepository.findById(dto.getIdDistrito())
                    .orElseThrow(() -> new ResourceNotFoundException("Distrito no encontrado con ID: " + dto.getIdDistrito()));
        }

        // Crear y guardar
        Local local = localMapper.toEntity(dto, distrito);
        Local localGuardado = localRepository.save(local);

        log.info("Local creado con ID: {}", localGuardado.getIdLocal());
        return localMapper.toResponseDTO(localGuardado);
    }

    @Transactional
    public LocalResponseDTO actualizar(Integer id, LocalCreateDTO dto) {
        log.info("Actualizando local ID: {}", id);

        Local local = localRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + id));

        // Validar nombre único (excepto el actual)
        if (!local.getNombre().equalsIgnoreCase(dto.getNombre()) 
                && localRepository.existsByNombreIgnoreCase(dto.getNombre())) {
            throw new BusinessException("Ya existe otro local con el nombre: " + dto.getNombre());
        }

        // Obtener distrito
        Distrito distrito = null;
        if (dto.getIdDistrito() != null) {
            distrito = distritoRepository.findById(dto.getIdDistrito())
                    .orElseThrow(() -> new ResourceNotFoundException("Distrito no encontrado con ID: " + dto.getIdDistrito()));
        }

        // Actualizar
        localMapper.updateEntity(local, dto, distrito);
        local.setFechaActualizacion(LocalDate.now());
        Local localActualizado = localRepository.save(local);

        log.info("Local actualizado: {}", id);
        return localMapper.toResponseDTO(localActualizado);
    }

    @Transactional
    public void eliminarLogico(Integer id) {
        log.info("Eliminación lógica del local ID: {}", id);

        Local local = localRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + id));

        local.setActivo(false);
        local.setFechaActualizacion(LocalDate.now());
        localRepository.save(local);

        log.info("Local desactivado: {}", id);
    }

    /**
     * RF-005: Buscar y filtrar locales por nombre.
     * 
     * @param nombre Nombre del local (búsqueda parcial)
     * @return Lista de locales que coinciden con el nombre
     */
    public List<LocalResponseDTO> buscarPorNombre(String nombre) {
        log.info("Buscando locales por nombre: {}", nombre);
        return localRepository.findByNombreContainingIgnoreCaseAndActivoTrue(nombre).stream()
                .map(localMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * RF-005: Buscar y filtrar locales por distrito.
     * 
     * @param idDistrito ID del distrito
     * @return Lista de locales en el distrito especificado
     */
    public List<LocalResponseDTO> buscarPorDistrito(Integer idDistrito) {
        log.info("Buscando locales por distrito ID: {}", idDistrito);
        return localRepository.findByDistritoIdDistritoAndActivoTrue(idDistrito).stream()
                .map(localMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * RF-003: Valida que la suma de aforos de las zonas no exceda el aforo total del local.
     * 
     * @param local Local a validar
     * @throws BusinessException si la suma de aforos excede el aforo total
     */
    public void validarAforosZonas(Local local) {
        Integer sumaAforos = local.getSumaAforosZonas();
        if (sumaAforos > local.getAforoTotal()) {
            throw new BusinessException(
                String.format("La suma de aforos de las zonas (%d) excede el aforo total del local (%d)", 
                    sumaAforos, local.getAforoTotal())
            );
        }
    }
}

