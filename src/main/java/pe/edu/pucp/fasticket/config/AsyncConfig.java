package pe.edu.pucp.fasticket.config;

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuración para procesamiento asíncrono.
 * 
 * Permite que las notificaciones se procesen en hilos separados,
 * garantizando que:
 * 1. No bloquean las transacciones principales
 * 2. Los errores no afectan el flujo de negocio
 * 3. Mejor rendimiento y experiencia de usuario
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Configura el executor para tareas asíncronas.
     * 
     * Parámetros optimizados para:
     * - Core: 5 hilos mínimos
     * - Max: 10 hilos máximos
     * - Queue: 100 tareas en cola
     * - Timeout: 60 segundos para hilos inactivos
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Configuración del pool de hilos
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-notif-");
        
        // Política de rechazo: El caller ejecuta la tarea si la cola está llena
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // Espera a que todas las tareas terminen al hacer shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("✅ Executor asíncrono configurado: core={}, max={}, queue={}", 
                 executor.getCorePoolSize(), 
                 executor.getMaxPoolSize(), 
                 executor.getQueueCapacity());
        
        return executor;
    }

    /**
     * Manejador global de excepciones no capturadas en tareas asíncronas.
     * 
     * CRÍTICO: Este handler garantiza que las excepciones en notificaciones
     * se logueen pero NO afecten la aplicación.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("❌ [ASYNC ERROR] Excepción no capturada en método asíncrono: {}.{}", 
                     method.getDeclaringClass().getSimpleName(),
                     method.getName());
            log.error("❌ [ASYNC ERROR] Mensaje: {}", throwable.getMessage(), throwable);
            log.error("❌ [ASYNC ERROR] Parámetros: {}", java.util.Arrays.toString(params));
            
            // Aquí podrías:
            // - Enviar alertas al equipo de desarrollo
            // - Registrar en una base de datos de errores
            // - Incrementar métricas de errores
            
            // IMPORTANTE: NO re-lanzamos la excepción
        };
    }
}

