package com.ashevtsov.gatekeeper.role;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Гранулярное разрешение: ресурс + действие (например, "users:read").
 * Справочная сущность — создается через seed/миграции или CRUD.
 */
@Entity
@Table(name = "permissions", uniqueConstraints = @UniqueConstraint(columnNames = {"resource", "action"}))
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false, length = 100)
    private String resource;

    @Column(nullable = false, length = 50)
    private String action;

    private String description;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
