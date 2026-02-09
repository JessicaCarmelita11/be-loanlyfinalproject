package com.example.loanlyFinalProject.repository;

import com.example.loanlyFinalProject.entity.PlafondDocument;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlafondDocumentRepository extends JpaRepository<PlafondDocument, Long> {

  List<PlafondDocument> findByUserPlafondId(Long userPlafondId);

  void deleteByUserPlafondId(Long userPlafondId);
}
