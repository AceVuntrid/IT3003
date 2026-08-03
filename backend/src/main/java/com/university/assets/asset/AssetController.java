package com.university.assets.asset;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.university.assets.asset.dto.AssetDtos.AssetDetail;
import com.university.assets.asset.dto.AssetDtos.AssetFilter;
import com.university.assets.asset.dto.AssetDtos.AssetRequest;
import com.university.assets.asset.dto.AssetDtos.AssetSummary;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.AssetCondition;
import com.university.assets.common.model.Enums.AssetStatus;
import com.university.assets.common.model.Enums.AssetType;
import com.university.assets.common.response.ApiResponse;
import com.university.assets.common.response.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Assets")
public class AssetController {

    public record StatusChangeRequest(AssetStatus status, AssetCondition condition) {}

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ASSET_VIEW')")
    public ApiResponse<PageResponse<AssetSummary>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID facultyId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) AssetType assetType,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) AssetCondition condition,
            @RequestParam(required = false) UUID custodianUserId,
            @RequestParam(required = false) LocalDate purchasedFrom,
            @RequestParam(required = false) LocalDate purchasedTo,
            @RequestParam(required = false) Boolean maintenanceDue,
            @RequestParam(required = false) Boolean includeArchived,
            @RequestParam(required = false) Boolean availableOnly,
            @RequestParam(required = false) Boolean reservable,
            @ParameterObject @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable) {
        AssetFilter filter = new AssetFilter(search, facultyId, departmentId, locationId, categoryId,
                assetType, status, condition, custodianUserId, purchasedFrom, purchasedTo,
                maintenanceDue, includeArchived, availableOnly, reservable);
        return ApiResponse.ok(assetService.list(filter, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSET_VIEW')")
    public ApiResponse<AssetDetail> get(@PathVariable UUID id) {
        return ApiResponse.ok(assetService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ASSET_CREATE')")
    public ApiResponse<AssetDetail> create(@Valid @RequestBody AssetRequest request) {
        return ApiResponse.ok("Asset created successfully", assetService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSET_EDIT')")
    public ApiResponse<AssetDetail> update(@PathVariable UUID id,
                                           @Valid @RequestBody AssetRequest request) {
        return ApiResponse.ok("Asset updated successfully", assetService.update(id, request));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('ASSET_ARCHIVE')")
    public ApiResponse<Void> archive(@PathVariable UUID id) {
        assetService.archive(id);
        return ApiResponse.message("Asset archived successfully");
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('ASSET_ARCHIVE')")
    public ApiResponse<Void> restore(@PathVariable UUID id) {
        assetService.restore(id);
        return ApiResponse.message("Asset restored successfully");
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ASSET_EDIT')")
    public ApiResponse<AssetDetail> changeStatus(@PathVariable UUID id,
                                                 @RequestBody StatusChangeRequest request) {
        return ApiResponse.ok("Asset status updated",
                assetService.changeStatus(id, request.status(), request.condition()));
    }

    @GetMapping(value = "/{id}/qr-code", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasAuthority('ASSET_VIEW')")
    public ResponseEntity<byte[]> qrCode(@PathVariable UUID id) {
        AssetDetail asset = assetService.get(id);
        try {
            String content = asset.qrCode() != null ? asset.qrCode() : "ASSET:" + asset.assetCode();
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 320, 320);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return ResponseEntity.ok()
                    .header("Content-Disposition",
                            "inline; filename=\"" + asset.assetCode() + "-qr.png\"")
                    .body(out.toByteArray());
        } catch (Exception e) {
            throw ApiException.badRequest("Could not generate QR code");
        }
    }
}
