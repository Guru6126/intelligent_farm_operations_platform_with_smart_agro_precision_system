package com.smartagro.backend.farm;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.smartagro.backend.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "farms")
public class Farm {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private AppUser owner;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 255)
	private String location;

	private Double latitude;

	private Double longitude;

	@Column(name = "total_area_acres", precision = 12, scale = 2)
	private BigDecimal totalAreaAcres;

	@Column(name = "is_demo", nullable = false)
	private boolean demo;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected Farm() {
	}

	public Farm(
			AppUser owner,
			String name,
			String location,
			Double latitude,
			Double longitude,
			BigDecimal totalAreaAcres) {
		this.owner = owner;
		this.name = name;
		this.location = location;
		this.latitude = latitude;
		this.longitude = longitude;
		this.totalAreaAcres = totalAreaAcres;
	}

	@PrePersist
	void initializeCreatedAt() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}

	public Long getId() {
		return id;
	}

	public AppUser getOwner() {
		return owner;
	}

	public String getName() {
		return name;
	}

	public String getLocation() {
		return location;
	}

	public Double getLatitude() {
		return latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public BigDecimal getTotalAreaAcres() {
		return totalAreaAcres;
	}

	public boolean isDemo() {
		return demo;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
