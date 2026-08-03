package com.university.assets.document;

import com.university.assets.common.exception.ApiException;
import com.university.assets.config.AppProperties;
import com.university.assets.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "jpg", "jpeg", "png", "docx", "xlsx");

    private final DocumentRepository repository;
    private final Path storageRoot;

    public DocumentService(DocumentRepository repository, AppProperties properties) {
        this.repository = repository;
        this.storageRoot = Path.of(properties.storage().path()).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<Document> list(String entityType, UUID entityId) {
        return repository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    @Transactional
    public Document store(String entityType, UUID entityId, String documentType, MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String extension = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw ApiException.badRequest(
                    "File type not allowed. Allowed formats: PDF, JPG, JPEG, PNG, DOCX, XLSX");
        }
        // Stored file name is generated; the original name is kept only as metadata.
        String storageKey = entityType.toLowerCase() + "/" + entityId + "/"
                + UUID.randomUUID() + "." + extension;
        try {
            Path target = storageRoot.resolve(storageKey).normalize();
            if (!target.startsWith(storageRoot)) {
                throw ApiException.badRequest("Invalid file path");
            }
            Files.createDirectories(target.getParent());
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not store uploaded file", e);
        }
        Document document = new Document();
        document.setEntityType(entityType);
        document.setEntityId(entityId);
        document.setDocumentType(documentType == null ? "OTHER" : documentType);
        document.setOriginalFilename(original);
        document.setStorageKey(storageKey);
        document.setMimeType(file.getContentType() == null
                ? "application/octet-stream" : file.getContentType());
        document.setSizeBytes(file.getSize());
        document.setUploadedBy(CurrentUser.id());
        return repository.save(document);
    }

    @Transactional(readOnly = true)
    public DownloadableDocument download(UUID id) {
        Document document = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Document"));
        Path path = storageRoot.resolve(document.getStorageKey()).normalize();
        if (!path.startsWith(storageRoot) || !Files.exists(path)) {
            throw ApiException.notFound("Document file");
        }
        try {
            return new DownloadableDocument(document, Files.readAllBytes(path));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read stored file", e);
        }
    }

    @Transactional
    public void delete(UUID id) {
        Document document = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Document"));
        Path path = storageRoot.resolve(document.getStorageKey()).normalize();
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Metadata removal still proceeds; orphan files are harmless locally.
        }
        repository.delete(document);
    }

    public record DownloadableDocument(Document document, byte[] content) {}
}
