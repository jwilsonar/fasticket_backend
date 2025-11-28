package pe.edu.pucp.fasticket.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.model.usuario.Rol;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.security.JwtUtil;


import java.util.Map;

@Tag(
        name = "OAuth",
        description = "API para obtener datos de Google. " +
                "Única función facilitada por Google"
)
@RestController
@RequestMapping("/api/v1/oauth")
@RequiredArgsConstructor
@Slf4j
public class OAuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ClienteRepository clienteRepository;

    @GetMapping("/user-info")
    public Map<String, Object> userInfo(@AuthenticationPrincipal OAuth2User principal) {
        return principal.getAttributes();
    }
    @GetMapping("/success")
    public ResponseEntity<?> handleOAuth2Success(OAuth2AuthenticationToken authentication) {
        // Obtener info del usuario de Google
        Map<String, Object> attributes = authentication.getPrincipal().getAttributes();
        String email = (String) attributes.get("email");
        String nombre = (String) attributes.get("name");
        String googleId = (String) attributes.get("sub");

        // Buscar o crear usuario en tu BD
        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseGet(() -> {
                    Cliente nuevoCliente = new Cliente();
                    nuevoCliente.setEmail(email);
                    nuevoCliente.setNombres(nombre);
                    nuevoCliente.setRol(Rol.CLIENTE);
                    // ... otros campos
                    return clienteRepository.save(nuevoCliente);
                });

        // Generar JWT
        String token = jwtUtil.generateToken(cliente.getEmail(), cliente.getRol().toString());

        // Redirigir al frontend con el token
        return ResponseEntity.ok(Map.of(
                "token", token,
                "usuario", cliente.getNombres(),
                "email", cliente.getEmail()
        ));
    }
}
