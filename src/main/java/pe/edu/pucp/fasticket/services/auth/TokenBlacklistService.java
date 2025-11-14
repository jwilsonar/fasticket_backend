package pe.edu.pucp.fasticket.services.auth;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    
    // Usaremos un ConcurrentHashMap como solución simple inicial
    private final Map<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();
    
    public void blacklistToken(String token) {
        // Guardar el token con marca de tiempo de invalidación
        blacklistedTokens.put(token, Instant.now());
    }
    
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }
    
    // Opcional: Limpiar tokens expirados periódicamente
    @Scheduled(fixedRate = 3600000) // Cada hora
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        blacklistedTokens.entrySet().removeIf(entry -> 
            now.minus(Duration.ofHours(24)).isAfter(entry.getValue())
        );
    }
}