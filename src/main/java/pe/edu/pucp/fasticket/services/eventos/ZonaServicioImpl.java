package pe.edu.pucp.fasticket.services.eventos;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.ZonaRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ZonaServicioImpl implements ZonaServicio {

    private final ZonaRepository zonaRepository;
    private final LocalesRepositorio localRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Zona> listarTodas() {
        log.info("Listando todas las zonas");
        return zonaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Zona> buscarPorId(Integer id) {
        log.info("Buscando zona con ID: {}", id);
        return zonaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Zona> buscarPorLocal(Integer idLocal) {
        log.info("Buscando zonas del local con ID: {}", idLocal);
        return zonaRepository.findByLocal_IdLocal(idLocal);
    }

    @Override
    public Zona crear(Zona zona, Integer idLocal) {
        log.info("Creando nueva zona: {}", zona.getNombre());
        log.info("ID del local a asignar: {}", idLocal);
        
        // Cargar el local desde la base de datos
        if (idLocal != null) {
            log.info("Buscando local con ID: {}", idLocal);
            Local local = localRepository.findById(idLocal)
                    .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + idLocal));
            zona.setLocal(local);
            log.info("Local asignado: {}", local);
        } else {
            log.warn("El ID del local es null");
        }
        
        Zona zonaGuardada = zonaRepository.save(zona);
        log.info("Zona guardada - ID: {}, Local: {}", zonaGuardada.getIdZona(), zonaGuardada.getLocal());
        return zonaGuardada;
    }

    @Override
    public Zona actualizar(Zona zona, Integer idLocal) {
        log.info("Actualizando zona con ID: {}", zona.getIdZona());
        log.info("ID del local a asignar: {}", idLocal);
        
        // Cargar el local desde la base de datos
        if (idLocal != null) {
            log.info("Buscando local con ID: {}", idLocal);
            Local local = localRepository.findById(idLocal)
                    .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + idLocal));
            zona.setLocal(local);
            log.info("Local asignado: {}", local);
        } else {
            log.warn("El ID del local es null");
        }
        
        return zonaRepository.save(zona);
    }

    @Override
    public void eliminar(Integer id) {
        log.info("Eliminando zona con ID: {}", id);
        if (!zonaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Zona no encontrada con ID: " + id);
        }
        zonaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Zona> buscarActivas() {
        log.info("Buscando zonas activas");
        return zonaRepository.findByActivoTrue();
    }

    @Override
    public Zona actualizarImagenUrl(Integer id, String imagenUrl) {
        log.info("Actualizando URL de imagen para zona ID: {}", id);

        Zona zona = zonaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zona no encontrada con ID: " + id));

        zona.setImagenUrl(imagenUrl);
        zona.setFechaActualizacion(LocalDate.now());
        Zona zonaActualizada = zonaRepository.save(zona);

        log.info("URL de imagen actualizada para zona ID: {}", id);
        return zonaActualizada;
    }
}
