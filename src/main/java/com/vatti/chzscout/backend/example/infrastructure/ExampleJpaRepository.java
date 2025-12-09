package com.vatti.chzscout.backend.example.infrastructure;

import com.vatti.chzscout.backend.example.domain.entity.Example;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExampleJpaRepository extends JpaRepository<Example, Long> {

  Optional<Example> findByEmail(String email);

  boolean existsByEmail(String email);
}
