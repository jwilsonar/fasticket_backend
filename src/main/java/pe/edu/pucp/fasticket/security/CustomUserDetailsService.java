package pe.edu.pucp.fasticket.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pe.edu.pucp.fasticket.model.usuario.Persona;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;

import java.util.Collections;

/**
 * Servicio personalizado para cargar detalles de usuario desde la base de datos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final PersonasRepositorio personasRepositorio;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Persona persona = personasRepositorio.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        return User.builder()
                .username(persona.getEmail())
                .password(persona.getContrasena())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + persona.getRol().name())))
                .accountExpired(false)
                .accountLocked(!persona.getActivo())
                .credentialsExpired(false)
                .disabled(!persona.getActivo())
                .build();
    }
}

