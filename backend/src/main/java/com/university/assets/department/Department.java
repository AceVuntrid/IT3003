package com.university.assets.department;

import com.university.assets.common.model.BaseEntity;
import com.university.assets.faculty.Faculty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "departments")
public class Department extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean active = true;

    /** Compulsory maintenance interval for the department's assets (null = disabled). */
    @Column(name = "maintenance_interval_days")
    private Integer maintenanceIntervalDays;

    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Integer getMaintenanceIntervalDays() { return maintenanceIntervalDays; }
    public void setMaintenanceIntervalDays(Integer maintenanceIntervalDays) { this.maintenanceIntervalDays = maintenanceIntervalDays; }
}
