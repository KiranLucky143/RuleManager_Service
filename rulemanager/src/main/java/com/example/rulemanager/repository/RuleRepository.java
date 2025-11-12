package com.example.rulemanager.repository;

import com.example.rulemanager.model.RuleDefinition;
import com.example.rulemanager.model.RuleSet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RuleRepository extends JpaRepository<RuleDefinition, Long> {

    // Fetch only active rules
    List<RuleDefinition> findByActiveTrue();

    // Fetch rules by RuleSet entity
    List<RuleDefinition> findByRuleSet(RuleSet ruleSet);

    // Optional: Fetch active rules within a specific RuleSet
    List<RuleDefinition> findByRuleSetAndActiveTrue(RuleSet ruleSet);
}
