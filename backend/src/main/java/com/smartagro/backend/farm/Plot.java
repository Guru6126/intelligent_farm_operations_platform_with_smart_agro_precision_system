package com.smartagro.backend.farm;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "plots")
public class Plot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "farm_id", nullable = false)
	private Farm farm;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "area_acres", precision = 12, scale = 4)
	private BigDecimal areaAcres;

	@Column(name = "soil_ph", precision = 4, scale = 2)
	private BigDecimal soilPh;

	@Column(name = "soil_type", length = 100)
	private String soilType;

	@Column(name = "boundary_geojson", columnDefinition = "TEXT")
	private String boundaryGeojson;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected Plot() {
	}

	public Plot(
			Farm farm,
			String name,
			BigDecimal areaAcres,
			BigDecimal soilPh,
			String soilType,
			String boundaryGeojson) {
		this.farm = farm;
		this.name = name;
		this.areaAcres = areaAcres;
		this.soilPh = soilPh;
		this.soilType = soilType;
		this.boundaryGeojson = boundaryGeojson;
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

	public Farm getFarm() {
		return farm;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getAreaAcres() {
		return areaAcres;
	}

	public BigDecimal getSoilPh() {
		return soilPh;
	}

	public String getSoilType() {
		return soilType;
	}

	public String getBoundaryGeojson() {
		return boundaryGeojson;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
