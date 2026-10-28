package com.smartagro.backend.farm;

import java.time.LocalDate;
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
@Table(name = "crops")
public class Crop {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "farm_id", nullable = false)
	private Farm farm;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "plot_id")
	private Plot plot;

	@Column(name = "crop_name", nullable = false, length = 255)
	private String cropName;

	@Column(length = 255)
	private String variety;

	@Column(length = 100)
	private String season;

	@Column(nullable = false, length = 30)
	private String stage;

	@Column(name = "sowing_date")
	private LocalDate sowingDate;

	@Column(name = "expected_harvest")
	private LocalDate expectedHarvest;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected Crop() {
	}

	public Crop(
			Farm farm,
			Plot plot,
			String cropName,
			String variety,
			String season,
			String stage,
			LocalDate sowingDate,
			LocalDate expectedHarvest) {
		this.farm = farm;
		this.plot = plot;
		this.cropName = cropName;
		this.variety = variety;
		this.season = season;
		this.stage = stage;
		this.sowingDate = sowingDate;
		this.expectedHarvest = expectedHarvest;
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

	public Plot getPlot() {
		return plot;
	}

	public String getCropName() {
		return cropName;
	}

	public String getVariety() {
		return variety;
	}

	public String getSeason() {
		return season;
	}

	public String getStage() {
		return stage;
	}

	public LocalDate getSowingDate() {
		return sowingDate;
	}

	public LocalDate getExpectedHarvest() {
		return expectedHarvest;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
