package com.vatti.chzscout.backend.example.infrastructure;

import com.vatti.chzscout.backend.example.domain.entity.Example;
import java.util.List;
import java.util.Optional;

public interface ExampleRepository {

  Example save(Example example);

  Optional<Example> findById(Long id);

  Optional<Example> findByEmail(String email);

  List<Example> findAll();

  boolean existsByEmail(String email);

  void deleteById(Long id);
}
