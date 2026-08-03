package com.university.assets.reservation;

import com.university.assets.asset.Asset;
import com.university.assets.consumable.ConsumableItem;
import com.university.assets.department.Department;
import com.university.assets.faculty.Faculty;
import com.university.assets.location.Location;
import com.university.assets.role.Role;
import com.university.assets.user.User;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves whether a user is the approving authority for a reservation, based on
 * the custodianship of the reserved asset, venue or consumable item. Ownership
 * resolves to exactly one authority, in this order:
 * <ol>
 *   <li>Department-owned (department set) — the department's admin: a DEPT_ADMIN
 *       (or LAB_MANAGER) whose own department matches.</li>
 *   <li>Faculty-owned (no department, faculty set) — the faculty dean: a
 *       FACULTY_DEAN (or FACULTY_ADMIN) whose own faculty matches.</li>
 *   <li>Unowned/miscellaneous (both null) — a caretaker: the asset's custodian,
 *       or the responsible user of the item's location or ANY ancestor
 *       location (walking the parent chain).</li>
 * </ol>
 * Consumable items resolve through the same chain (department → faculty →
 * location responsibleUser); they have no custodian of their own.
 * SUPER_ADMIN and ASSET_ADMIN are global approvers and bypass scoping.
 */
@Service
public class ApprovalScopeService {

    static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    static final String ROLE_ASSET_ADMIN = "ASSET_ADMIN";
    static final String ROLE_DEPT_ADMIN = "DEPT_ADMIN";
    static final String ROLE_LAB_MANAGER = "LAB_MANAGER";
    static final String ROLE_FACULTY_DEAN = "FACULTY_DEAN";
    static final String ROLE_FACULTY_ADMIN = "FACULTY_ADMIN";
    static final String ROLE_CARETAKER = "CARETAKER";
    static final String ROLE_FINANCE_OFFICER = "FINANCE_OFFICER";

    /** Safety cap when walking the location parent chain. */
    private static final int MAX_LOCATION_DEPTH = 25;

    public boolean canApprove(User approver, Reservation reservation) {
        if (approver == null || reservation == null) {
            return false;
        }
        Set<String> roleNames = approver.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        if (roleNames.contains(ROLE_SUPER_ADMIN) || roleNames.contains(ROLE_ASSET_ADMIN)) {
            return true;
        }
        if (reservation.getAsset() != null) {
            return canApproveForAsset(approver, roleNames, reservation.getAsset());
        }
        if (reservation.getLocation() != null) {
            return canApproveForLocation(approver, roleNames, reservation.getLocation());
        }
        if (reservation.getConsumableItem() != null) {
            return canApproveForConsumable(approver, roleNames, reservation.getConsumableItem());
        }
        return false;
    }

    private boolean canApproveForAsset(User approver, Set<String> roleNames, Asset asset) {
        if (asset.getDepartment() != null) {
            return isDepartmentAdmin(approver, roleNames, asset.getDepartment());
        }
        if (asset.getFaculty() != null) {
            return isFacultyDean(approver, roleNames, asset.getFaculty());
        }
        if (asset.getCustodian() != null && sameUser(asset.getCustodian(), approver)) {
            return true;
        }
        return isResponsibleForLocationOrAncestor(approver, asset.getLocation());
    }

    private boolean canApproveForLocation(User approver, Set<String> roleNames, Location location) {
        if (location.getDepartment() != null) {
            return isDepartmentAdmin(approver, roleNames, location.getDepartment());
        }
        if (location.getFaculty() != null) {
            return isFacultyDean(approver, roleNames, location.getFaculty());
        }
        return isResponsibleForLocationOrAncestor(approver, location);
    }

    private boolean canApproveForConsumable(User approver, Set<String> roleNames, ConsumableItem item) {
        if (item.getDepartment() != null) {
            return isDepartmentAdmin(approver, roleNames, item.getDepartment());
        }
        if (item.getFaculty() != null) {
            return isFacultyDean(approver, roleNames, item.getFaculty());
        }
        return isResponsibleForLocationOrAncestor(approver, item.getLocation());
    }

    private boolean isDepartmentAdmin(User approver, Set<String> roleNames, Department department) {
        return (roleNames.contains(ROLE_DEPT_ADMIN) || roleNames.contains(ROLE_LAB_MANAGER))
                && approver.getDepartment() != null
                && approver.getDepartment().getId().equals(department.getId());
    }

    private boolean isFacultyDean(User approver, Set<String> roleNames, Faculty faculty) {
        return (roleNames.contains(ROLE_FACULTY_DEAN) || roleNames.contains(ROLE_FACULTY_ADMIN))
                && approver.getFaculty() != null
                && approver.getFaculty().getId().equals(faculty.getId());
    }

    private boolean isResponsibleForLocationOrAncestor(User approver, Location location) {
        Location current = location;
        int depth = 0;
        while (current != null && depth++ < MAX_LOCATION_DEPTH) {
            if (current.getResponsibleUser() != null && sameUser(current.getResponsibleUser(), approver)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private boolean sameUser(User a, User b) {
        return a.getId() != null && a.getId().equals(b.getId());
    }
}
