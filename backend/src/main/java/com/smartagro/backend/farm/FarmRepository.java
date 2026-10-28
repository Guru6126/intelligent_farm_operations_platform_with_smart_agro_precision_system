package com.smartagro.backend.farm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmRepository extends JpaRepository<Farm, Long> {

	List<Farm> findAllByOwner_Id(Long ownerId);

	Optional<Farm> findByIdAndOwner_Id(Long id, Long ownerId);
}
