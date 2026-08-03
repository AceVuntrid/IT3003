package com.university.assets.location;

import com.university.assets.common.model.BaseEntity;
import com.university.assets.common.model.Enums.LocationType;
import com.university.assets.department.Department;
import com.university.assets.faculty.Faculty;
import com.university.assets.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "locations")
public class Location extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Location parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType type;

    private String address;

    private Integer capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private User responsibleUser;

    private String description;

    /** Price-list flat fee charged per venue booking; null or zero = free. */
    @Column(name = "booking_fee", precision = 15, scale = 2)
    private BigDecimal bookingFee;

    @Column(nullable = false)
    private boolean active = true;

    public Location getParent() { return parent; }
    public void setParent(Location parent) { this.parent = parent; }
    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocationType getType() { return type; }
    public void setType(LocationType type) { this.type = type; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public User getResponsibleUser() { return responsibleUser; }
    public void setResponsibleUser(User responsibleUser) { this.responsibleUser = responsibleUser; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getBookingFee() { return bookingFee; }
    public void setBookingFee(BigDecimal bookingFee) { this.bookingFee = bookingFee; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
