
package com.nitros64.nitros_games_backend.tooling.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

public interface ProgramToolTypeRepository extends BaseRepository<ProgramToolType, Long> {

    Page<ProgramToolType> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
