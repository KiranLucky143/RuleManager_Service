package com.example.rulemanager.repository;

import com.example.rulemanager.model.ObjectRulesetMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ObjectRulesetMappingRepository extends JpaRepository<ObjectRulesetMapping, Long> {

    // exact match — controller expects this
    ObjectRulesetMapping findFirstByObjectTypeAndObjectKey(String objectType, String objectKey);

    // optional-style
    Optional<ObjectRulesetMapping> findByObjectTypeAndObjectKey(String objectType, String objectKey);

    // list by objectType (controller fallback uses this)
    List<ObjectRulesetMapping> findByObjectType(String objectType);

    // NEW: clean up mappings for a ruleset
    void deleteByRulesetId(Long rulesetId);
}
