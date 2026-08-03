package com.university.assets.asset.specification;

import com.university.assets.asset.Asset;
import com.university.assets.asset.dto.AssetDtos.AssetFilter;
import com.university.assets.common.model.Enums.AssetStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class AssetSpecifications {

    private AssetSpecifications() {}

    public static Specification<Asset> withFilter(AssetFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!Boolean.TRUE.equals(filter.includeArchived())) {
                predicates.add(cb.isNull(root.get("archivedAt")));
            }
            if (filter.search() != null && !filter.search().isBlank()) {
                String like = "%" + filter.search().toLowerCase().trim() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("assetCode")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("serialNumber"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("barcode"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("qrCode"), "")), like)));
            }
            if (filter.facultyId() != null) {
                predicates.add(cb.equal(root.get("faculty").get("id"), filter.facultyId()));
            }
            if (filter.departmentId() != null) {
                predicates.add(cb.equal(root.get("department").get("id"), filter.departmentId()));
            }
            if (filter.locationId() != null) {
                predicates.add(cb.equal(root.get("location").get("id"), filter.locationId()));
            }
            if (filter.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.categoryId()));
            }
            if (filter.assetType() != null) {
                predicates.add(cb.equal(root.get("assetType"), filter.assetType()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.condition() != null) {
                predicates.add(cb.equal(root.get("condition"), filter.condition()));
            }
            if (filter.custodianUserId() != null) {
                predicates.add(cb.equal(root.get("custodian").get("id"), filter.custodianUserId()));
            }
            if (filter.purchasedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("purchaseDate"), filter.purchasedFrom()));
            }
            if (filter.purchasedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("purchaseDate"), filter.purchasedTo()));
            }
            if (Boolean.TRUE.equals(filter.maintenanceDue())) {
                predicates.add(cb.and(
                        cb.isNotNull(root.get("nextServiceDate")),
                        cb.lessThanOrEqualTo(root.get("nextServiceDate"), LocalDate.now().plusDays(30))));
            }
            if (Boolean.TRUE.equals(filter.availableOnly())) {
                // Mirrors the check-out guard: something left to issue and not in a blocked state.
                predicates.add(cb.greaterThan(root.get("availableQuantity"), 0));
                predicates.add(cb.not(root.get("status").in(
                        AssetStatus.UNDER_MAINTENANCE, AssetStatus.LOST, AssetStatus.DISPOSED)));
            }
            if (filter.reservable() != null) {
                predicates.add(cb.equal(root.get("reservable"), filter.reservable()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
