package com.university.assets.document;

import com.university.assets.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents")
public class DocumentController {

    public record DocumentResponse(UUID id, String entityType, UUID entityId, String documentType,
                                   String originalFilename, String mimeType, long sizeBytes,
                                   Instant uploadedAt) {
        static DocumentResponse from(Document d) {
            return new DocumentResponse(d.getId(), d.getEntityType(), d.getEntityId(),
                    d.getDocumentType(), d.getOriginalFilename(), d.getMimeType(),
                    d.getSizeBytes(), d.getCreatedAt());
        }
    }

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ASSET_VIEW')")
    public ApiResponse<List<DocumentResponse>> list(@RequestParam String entityType,
                                                    @RequestParam UUID entityId) {
        return ApiResponse.ok(documentService.list(entityType, entityId).stream()
                .map(DocumentResponse::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ASSET_EDIT', 'ASSET_CREATE', 'MAINTENANCE_MANAGE', 'CONSUMABLE_EDIT')")
    public ApiResponse<DocumentResponse> upload(@RequestParam String entityType,
                                                @RequestParam UUID entityId,
                                                @RequestParam(required = false) String documentType,
                                                @RequestParam("file") MultipartFile file) {
        Document document = documentService.store(entityType, entityId, documentType, file);
        return ApiResponse.ok("Document uploaded successfully", DocumentResponse.from(document));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('ASSET_VIEW')")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        DocumentService.DownloadableDocument downloadable = documentService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(downloadable.document().getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
                        + downloadable.document().getOriginalFilename().replace("\"", "") + "\"")
                .body(downloadable.content());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSET_EDIT')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        documentService.delete(id);
        return ApiResponse.message("Document deleted");
    }
}
