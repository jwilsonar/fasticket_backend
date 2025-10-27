// RUTA: pe.edu.pucp.fasticket.services.zonas.ZonaService.java
// (Asegúrate de que el nombre del archivo y la clase sean "ZonaService")

package pe.edu.pucp.fasticket.services.zonas;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.zonas.ZonaDTO; // El DTO que YA tenías
import pe.edu.pucp.fasticket.dto.zonas.ZonaCreateDTO; // El DTO que creamos
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.mapper.ZonaMapper; // Crearemos este mapper
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.ZonasRepositorio; // Tu repo existente

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Reemplaza @Autowired
@Slf4j
@Transactional(readOnly = true)
public class ZonaService { // <-- Renombrado de ZonaServicio a ZonaService

    private final ZonasRepositorio repo_zonas;
    private final EventosRepositorio eventoRepository; // Nueva dependencia
    private final ZonaMapper zonaMapper; // Nueva dependencia (Mapper)

    /**
     * PASO 2 del Wizard: Agrega una Zona a un Evento en BORRADOR.
     * Este es el método que faltaba y que el EventoController necesita.
     */
    @Transactional
    public ZonaDTO agregarZonaAEvento(Integer idEvento, ZonaCreateDTO zonaDTO) {
        log.info("Agregando zona '{}' al evento ID: {}", zonaDTO.getNombre(), idEvento);

        // 1. Buscar el evento
        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + idEvento));

        // 2. Validar que el evento esté en BORRADOR
        if (evento.getEstadoEvento() != EstadoEvento.BORRADOR) {
            throw new BusinessException("Solo se pueden agregar zonas a eventos en estado BORRADOR");
        }

        // 3. Obtener el local del evento
        Local local = evento.getLocal();
        if (local == null) {
            throw new BusinessException("El evento ID: " + idEvento + " no tiene un local asociado. No se pueden agregar zonas.");
        }

        // 4. Lógica de Aforo (RF-003): Validar que el aforo de las zonas no supere el aforo total del local
        // Nota: Asumimos que la relación de Local a Zonas está mapeada
        Integer aforoZonasActual = local.getZonas().stream()
                .mapToInt(Zona::getAforoMax)
                .sum();

        if ((aforoZonasActual + zonaDTO.getAforoMax()) > local.getAforoTotal()) {
            throw new BusinessException("Se ha superado el aforo máximo del local (" + local.getAforoTotal() + "). Aforo actual de zonas: " + aforoZonasActual);
        }

        // 5. Mapear y guardar la Zona
        Zona nuevaZona = zonaMapper.toEntity(zonaDTO); // DTO -> Entity
        nuevaZona.setLocal(local); // Asignar la zona al local del evento

        Zona zonaGuardada = repo_zonas.save(nuevaZona);

        log.info("Zona creada con ID: {}", zonaGuardada.getIdZona());
        return zonaMapper.toDTO(zonaGuardada); // Entity -> DTO
    }


    // --- MÉTODOS ANTIGUOS (Actualizados para usar DTOs) ---

    public List<ZonaDTO> ListarZonas(){
        return repo_zonas.findAll().stream()
                .map(zonaMapper::toDTO) // Devuelve DTO, no Entidad
                .collect(Collectors.toList());
    }

    public ZonaDTO BuscarId(Integer id){
        Zona zona = repo_zonas.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zona no encontrada con ID: " + id));
        return zonaMapper.toDTO(zona); // Devuelve DTO, no Entidad
    }

    // (El método Guardar(Zona zona) no es seguro, es mejor usar DTOs, 
    // pero lo dejamos comentado por ahora)
    /*
    @Transactional
    public Zona Guardar(Zona zona){
        return (Zona) repo_zonas.save(zona);
    }
    */

    @Transactional
    public void Eliminar(Integer id){
        repo_zonas.deleteById(id);
    }
}