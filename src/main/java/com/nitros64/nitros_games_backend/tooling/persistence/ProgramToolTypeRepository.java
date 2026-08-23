
package com.nitros64.nitros_games_backend.tooling.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;

public interface ProgramToolTypeRepository extends JpaRepository<ProgramToolType, Long> {

    Page<ProgramToolType> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
