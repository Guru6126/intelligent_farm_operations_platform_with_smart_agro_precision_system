package com.agroprecision.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invite_tokens")
public class InviteToken {
    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 64, nullable = false, unique = true)
    private String token;

    @Column(name = "farm_id", length = 36, nullable = false)
    private String farmId;

    @Column(name = "created_by", length = 36, nullable = false)
    private String createdBy;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "used_by", length = 36)
    private String usedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getFarmId() { return farmId; }
    public void setFarmId(String farmId) { this.farmId = farmId; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
    public String getUsedBy() { return usedBy; }
    public void setUsedBy(String usedBy) { this.usedBy = usedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }
    public boolean isUsed() { return usedAt != null; }
}

