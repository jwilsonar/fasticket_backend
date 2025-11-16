-- Tabla de notificaciones in-app por usuario
CREATE TABLE IF NOT EXISTS notificaciones_usuario (
    id BIGSERIAL PRIMARY KEY,
    persona_id INTEGER NOT NULL,
    tipo VARCHAR(64) NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    mensaje TEXT NOT NULL,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    creada_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    leida_en TIMESTAMP WITH TIME ZONE NULL,
    metadata_json TEXT NULL
);
CREATE INDEX IF NOT EXISTS idx_notif_usuario_persona ON notificaciones_usuario (persona_id);
CREATE INDEX IF NOT EXISTS idx_notif_usuario_leida ON notificaciones_usuario (leida);
CREATE INDEX IF NOT EXISTS idx_notif_usuario_creada ON notificaciones_usuario (creada_en DESC);

-- Tabla de preferencias de notificaciones in-app
CREATE TABLE IF NOT EXISTS preferencias_notificacion (
    persona_id INTEGER PRIMARY KEY,
    habilitado BOOLEAN NOT NULL DEFAULT TRUE
);


