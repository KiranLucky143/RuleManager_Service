package com.example.rulemanager.repository;

import com.example.rulemanager.model.RuleSetFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RuleSetFileRepository extends JpaRepository<RuleSetFile, Long> {
    List<RuleSetFile> findByRulesetIdOrderByOrderIndexAsc(Long rulesetId);


}
