package com.smartagro.backend.farm;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlotRepository extends JpaRepository<Plot, Long> {

	List<Plot> findAllByFarm_IdIn(Collection<Long> farmIds);
}
