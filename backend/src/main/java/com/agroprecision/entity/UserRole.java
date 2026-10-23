package com.agroprecision.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_roles")
public class UserRole {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String role; // admin | farmer | field_worker

    public UserRole() {}
    public UserRole(String id, User user, String role) { this.id = id; this.user = user; this.role = role; }

    public String getId() { return id; }
    public User getUser() { return user; }
    public String getRole() { return role; }
}
