package com.vatti.chzscout.backend.example.infrastructure;

import com.vatti.chzscout.backend.example.domain.entity.Example;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ExampleRepositoryImpl implements ExampleRepository {

  private final ExampleJpaRepository exampleJpaRepository;

  @Override
  public Example save(Example example) {
    return exampleJpaRepository.save(example);
  }

  @Override
  public Optional<Example> findById(Long id) {
    return exampleJpaRepository.findById(id);
  }

  @Override
  public Optional<Example> findByEmail(String email) {
    return exampleJpaRepository.findByEmail(email);
  }

  @Override
  public List<Example> findAll() {
    return exampleJpaRepository.findAll();
  }

  @Override
  public boolean existsByEmail(String email) {
    return exampleJpaRepository.existsByEmail(email);
  }

  @Override
  public void deleteById(Long id) {
    exampleJpaRepository.deleteById(id);
  }
}
