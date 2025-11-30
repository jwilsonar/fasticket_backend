package pe.edu.pucp.fasticket.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.model.ConfiguracionGlobal;
import pe.edu.pucp.fasticket.repository.ConfiguracionRepository;

@Configuration
@Profile({"dev","local","prod"}) // no ejecutar en 'test' por defecto
@RequiredArgsConstructor
public class StartupConfigInitializer implements ApplicationRunner {

    private final ConfiguracionRepository repo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // TIEMPO_CARRO_MINUTOS (valor en minutos como String)
        repo.findById("TIEMPO_CARRO_MINUTOS").orElseGet(() -> {
            ConfiguracionGlobal c = new ConfiguracionGlobal();
            c.setKey("TIEMPO_CARRO_MINUTOS");
            c.setValue("15"); // 15 minutos
            c.setValueType("INTEGER");
            c.setDescripcion("Tiempo máximo (min) que un carrito puede quedar inactivo");
            return repo.save(c);
        });

        // LIMITE_PERSONAS_COMPRA (cantidad máxima de personas por compra)
        repo.findById("LIMITE_PERSONAS_COMPRA").orElseGet(() -> {
            ConfiguracionGlobal c = new ConfiguracionGlobal();
            c.setKey("LIMITE_PERSONAS_COMPRA");
            c.setValue("10");
            c.setValueType("INTEGER");
            c.setDescripcion("Máximo de personas por compra");
            return repo.save(c);
        });

        // PUNTOS_POR_MONEDA (valor de puntos por unidad monetaria, como String decimal)
        repo.findById("PUNTOS_POR_MONEDA").orElseGet(() -> {
            ConfiguracionGlobal c = new ConfiguracionGlobal();
            c.setKey("PUNTOS_POR_MONEDA");
            c.setValue("0.5");
            c.setValueType("DOUBLE");
            c.setDescripcion("Puntos otorgados por unidad monetaria");
            return repo.save(c);
        });
    }
}