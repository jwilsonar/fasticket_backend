package pe.edu.pucp.fasticket.services;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {
    
    /**
     * Sube un archivo a S3 y retorna la URL pública
     * @param file Archivo a subir
     * @param folder Carpeta donde guardar el archivo (ej: "eventos", "locales", "zonas")
     * @param entityId ID de la entidad asociada
     * @return URL pública del archivo subido
     */
    String uploadFile(MultipartFile file, String folder, Integer entityId);
    
    /**
     * Sube múltiples archivos a S3
     * @param files Lista de archivos a subir
     * @param folder Carpeta donde guardar los archivos
     * @param entityId ID de la entidad asociada
     * @return Lista de URLs públicas de los archivos subidos
     */
    List<String> uploadFiles(List<MultipartFile> files, String folder, Integer entityId);
    
    /**
     * Sube un archivo desde bytes a S3 y retorna la URL pública
     * @param fileBytes Bytes del archivo a subir
     * @param fileName Nombre del archivo
     * @param contentType Tipo de contenido (ej: "application/pdf")
     * @param folder Carpeta donde guardar el archivo
     * @param entityId ID de la entidad asociada
     * @return URL pública del archivo subido
     */
    String uploadFileFromBytes(byte[] fileBytes, String fileName, String contentType, String folder, Integer entityId);
    
    /**
     * Descarga un archivo desde S3 usando su URL
     * @param fileUrl URL del archivo en S3
     * @return Bytes del archivo descargado
     */
    byte[] downloadFile(String fileUrl);
    
    /**
     * Elimina un archivo de S3
     * @param fileUrl URL del archivo a eliminar
     * @return true si se eliminó correctamente
     */
    boolean deleteFile(String fileUrl);
    
    /**
     * Genera un nombre único para el archivo
     * @param originalFilename Nombre original del archivo
     * @param folder Carpeta donde se guardará
     * @param entityId ID de la entidad
     * @return Nombre único para el archivo
     */
    String generateUniqueFileName(String originalFilename, String folder, Integer entityId);
}
