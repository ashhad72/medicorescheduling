package com.medicoreschedulingengine.os.model.entity;

import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "department_type", nullable = false, unique = true, length = 20)
    private DepartmentType departmentType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Patient> patients = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────

    public Department() {}

    public Department(String name, DepartmentType departmentType, String description) {
        this.name = name;
        this.departmentType = departmentType;
        this.description = description;
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public DepartmentType getDepartmentType() { return departmentType; }
    public void setDepartmentType(DepartmentType departmentType) { this.departmentType = departmentType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Patient> getPatients() { return patients; }
    public void setPatients(List<Patient> patients) { this.patients = patients; }

    @Override
    public String toString() {
        return "Department{id=" + id + ", name='" + name + "', type=" + departmentType + "}";
    }
}
