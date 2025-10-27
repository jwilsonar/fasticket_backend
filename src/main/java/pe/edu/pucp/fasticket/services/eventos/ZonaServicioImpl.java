package pe.edu.pucp.fasticket.services.eventos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.repository.eventos.ZonaRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ZonaServicioImpl implements ZonaServicio {

    private final ZonaRepository zonaRepository;

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
    public Zona crear(Zona zona) {
        log.info("Creando nueva zona: {}", zona.getNombre());
        return zonaRepository.save(zona);
    }

    @Override
    public Zona actualizar(Zona zona) {
        log.info("Actualizando zona con ID: {}", zona.getIdZona());
        return zonaRepository.save(zona);
    }

    @Override
    public void eliminar(Integer id) {
        log.info("Eliminando zona con ID: {}", id);
        zonaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Zona> buscarActivas() {
        log.info("Buscando zonas activas");
        return zonaRepository.findByActivoTrue();
    }
}
