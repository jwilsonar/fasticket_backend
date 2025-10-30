package pe.edu.pucp.fasticket.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import pe.edu.pucp.fasticket.model.usuario.Persona;

import java.util.Collection;

public class UserDetailsImpl extends User {
    private final Integer idPersona;

    public UserDetailsImpl(Persona persona, Collection<? extends GrantedAuthority> authorities) {
        super(
                persona.getEmail(),
                persona.getContrasena(),
                persona.getActivo(),
                true,
                true,
                persona.getActivo(),
                authorities
        );
        this.idPersona = persona.getIdPersona();
    }

    public Integer getIdPersona() {
        return idPersona;
    }
}
