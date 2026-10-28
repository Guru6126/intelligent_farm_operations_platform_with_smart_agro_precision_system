package com.smartagro.backend.farm;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CropRepository extends JpaRepository<Crop, Long> {

	List<Crop> findAllByFarm_IdIn(Collection<Long> farmIds);
}
