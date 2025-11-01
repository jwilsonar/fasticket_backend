package pe.edu.pucp.fasticket.services.usuario;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.usuario.ClientePerfilEditDTO;
import pe.edu.pucp.fasticket.dto.usuario.ClientePerfilUpdateDTO;
import pe.edu.pucp.fasticket.dto.usuario.ClientePerfilResponseDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoMembresia;
import pe.edu.pucp.fasticket.model.geografia.Distrito;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.geografia.DistritoRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;

/**
 * Servicio para gestión de clientes.
 * Implementa RF-030, RF-032, RF-060, RF-091.
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PersonasRepositorio personasRepositorio;
    private final DistritoRepository distritoRepositorio;

    /**
     * RF-030: Obtiene el perfil del cliente por email.
     * 
     * @param email Email del cliente
     * @return Perfil del cliente
     */
    public ClientePerfilResponseDTO obtenerPerfilPorEmail(String email) {
        log.info("Obteniendo perfil del cliente con email: {}", email);
        Cliente cliente = (Cliente) personasRepositorio.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + email));
        return convertirAPerfilDTO(cliente);
    }


    /**
     * RF-030: Obtiene el perfil del cliente por ID.
     * 
     * @param id ID del cliente
     * @return Perfil del cliente
     */
    public ClientePerfilResponseDTO obtenerPerfilPorId(Integer id) {
        log.info("Obteniendo perfil del cliente con ID: {}", id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));
        return convertirAPerfilDTO(cliente);
    }

    /**
     * RF-060: Actualiza el perfil del cliente.
     * 
     * @param email Email del cliente autenticado
     * @param dto Datos a actualizar
     * @return Perfil actualizado
     */
    @Transactional
    public ClientePerfilResponseDTO actualizarPerfil(String email, ClientePerfilUpdateDTO dto) {
        log.info("Actualizando perfil del cliente: {}", email);
        
        Cliente cliente = (Cliente) personasRepositorio.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + email));

        // Actualizar campos si vienen en el DTO
        if (dto.getNombres() != null && !dto.getNombres().isBlank()) {
            cliente.setNombres(dto.getNombres());
        }
        if (dto.getApellidos() != null && !dto.getApellidos().isBlank()) {
            cliente.setApellidos(dto.getApellidos());
        }
        if (dto.getTelefono() != null && !dto.getTelefono().isBlank()) {
            cliente.setTelefono(dto.getTelefono());
        }
        if (dto.getDireccion() != null && !dto.getDireccion().isBlank()) {
            cliente.setDireccion(dto.getDireccion());
        }
        
        // Validar que el nuevo email no esté en uso por otro cliente
        if (dto.getEmail() != null && !dto.getEmail().equals(cliente.getEmail())) {
            if (personasRepositorio.existsByEmail(dto.getEmail())) {
                throw new BusinessException("El email ya está registrado por otro usuario");
            }
            cliente.setEmail(dto.getEmail());
        }

        cliente.setFechaActualizacion(java.time.LocalDate.now());
        Cliente clienteActualizado = clienteRepository.save(cliente);

        log.info("Perfil actualizado exitosamente para: {}", email);
        return convertirAPerfilDTO(clienteActualizado);
    }

    /**
     * RF-060: Actualiza el perfil del cliente(pero para el Administrador).
     *
     * @param id ID del cliente autenticado
     * @param dto Datos a actualizar
     * @return Perfil actualizado
     */
    @Transactional
    public ClientePerfilResponseDTO editarPerfil(Integer id, ClientePerfilEditDTO dto) {
        log.info("Actualizando perfil del cliente de ID: {}", id);

        Cliente cliente = (Cliente) personasRepositorio.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        // Actualizar campos si vienen en el DTO
        if (dto.getNombres() != null && !dto.getNombres().isBlank()) {
            cliente.setNombres(dto.getNombres());
        }
        if (dto.getApellidos() != null && !dto.getApellidos().isBlank()) {
            cliente.setApellidos(dto.getApellidos());
        }
        if (dto.getTelefono() != null && !dto.getTelefono().isBlank()) {
            cliente.setTelefono(dto.getTelefono());
        }
        if (dto.getDireccion() != null && !dto.getDireccion().isBlank()) {
            cliente.setDireccion(dto.getDireccion());
        }
        if (dto.getDocIdentidad() != null && !dto.getDocIdentidad().isBlank()) {
            cliente.setDocIdentidad(dto.getDocIdentidad());
        }

        if (dto.getIdDistrito() != null){
            Distrito distrito = distritoRepositorio.findById(dto.getIdDistrito())
                    .orElseThrow(() -> new ResourceNotFoundException("Distrito no encontrado con ID: " + dto.getIdDistrito()));
            cliente.setDistrito(distrito);
        }

        // Validar que el nuevo email no esté en uso por otro cliente
        if (dto.getEmail() != null && !dto.getEmail().equals(cliente.getEmail())) {
            if (personasRepositorio.existsByEmail(dto.getEmail())) {
                throw new BusinessException("El email ya está registrado por otro usuario");
            }
            cliente.setEmail(dto.getEmail());
        }

        cliente.setFechaActualizacion(java.time.LocalDate.now());
        Cliente clienteActualizado = clienteRepository.save(cliente);

        log.info("Perfil actualizado exitosamente para: {}", id);
        return convertirAPerfilDTO(clienteActualizado);
    }

    /**
     * RF-032, RF-091: Obtiene el historial de compras del cliente por email.
     * 
     * @param email Email del cliente
     * @return Lista de órdenes de compra
     */
    public List<OrdenCompra> obtenerHistorialCompras(String email) {
        log.info("Obteniendo historial de compras para: {}", email);
        Cliente cliente = (Cliente) personasRepositorio.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + email));
        return cliente.getOrdenesCompra();
    }

    /**
     * RF-032, RF-091: Obtiene el historial de compras del cliente por ID.
     * 
     * @param id ID del cliente
     * @return Lista de órdenes de compra
     */
    public List<OrdenCompra> obtenerHistorialComprasPorId(Integer id) {
        log.info("Obteniendo historial de compras para cliente ID: {}", id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));
        return cliente.getOrdenesCompra();
    }

    /**
     * RF-030: Obtiene los perfiles de clientes por Nivel.
     * 
     * @param nivel Nivel del cliente
     * @return Lista de perfiles de clientes
     */
    public List<ClientePerfilResponseDTO> obtenerPerfilesPorNivel(TipoMembresia nivel) {
        log.info("Obteniendo perfiles de clientes con nivel: {}", nivel);
        List<Cliente> clientes = clienteRepository.findByNivel(nivel);
        return (clientes == null)
            ? Collections.emptyList()
            : clientes.stream()
                    .map(this::convertirAPerfilDTO)
                    .collect(Collectors.toList());
    }

    /**
     * Obtiene una lista de todos los cliente
     * */

    public List<ClientePerfilResponseDTO> listarTodos() {
        log.info("Obteniendo perfiles de todos los clientes");
        List<Cliente> clientes = clienteRepository.findAll();
        return clientes.stream().map(this::convertirAPerfilDTO).collect(Collectors.toList());
    }

    /**
     * Convierte una entidad Cliente a ClientePerfilDTO.
     * 
     * @param cliente Entidad cliente
     * @return DTO con información del perfil
     */
    private ClientePerfilResponseDTO convertirAPerfilDTO(Cliente cliente) {
        ClientePerfilResponseDTO dto = new ClientePerfilResponseDTO();
        dto.setIdCliente(cliente.getIdPersona());
        dto.setTipoDocumento(cliente.getTipoDocumento());
        dto.setDocIdentidad(cliente.getDocIdentidad());
        dto.setNombres(cliente.getNombres());
        dto.setApellidos(cliente.getApellidos());
        dto.setTelefono(cliente.getTelefono());
        dto.setEmail(cliente.getEmail());
        dto.setFechaNacimiento(cliente.getFechaNacimiento());
        dto.setDireccion(cliente.getDireccion());
        dto.setPuntosAcumulados(cliente.getPuntosAcumulados());
        dto.setNivel(cliente.getNivel());
        dto.setEdad(cliente.calcularEdad());
        dto.setFechaCreacion(cliente.getFechaCreacion());
        return dto;
    }
}

