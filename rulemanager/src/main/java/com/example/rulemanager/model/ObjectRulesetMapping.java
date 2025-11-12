package com.example.rulemanager.model;

import jakarta.persistence.*;

@Entity
@Table(name = "OBJECT_RULESET_MAPPING")
public class ObjectRulesetMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. LOAN_APPLICATION, CREDIT_CARD_APPLICATION, PRODUCT
    @Column(name = "object_type", nullable = false)
    private String objectType;

    // e.g. the application id or business key (kept as String for flexibility)
    @Column(name = "object_key", nullable = false)
    private String objectKey;

    // Optional: store ruleset id directly (nullable)
    @Column(name = "ruleset_id")
    private Long rulesetId;

    // Optional: ruleset version
    @Column(name = "ruleset_version")
    private Integer rulesetVersion;

    // Optional: if you later want to map to RuleSet entity, keep this (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_set_id", insertable = false, updatable = false)
    private RuleSet ruleSet;

    public ObjectRulesetMapping() {}

    // --- getters / setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }

    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

    public Long getRulesetId() { return rulesetId; }
    public void setRulesetId(Long rulesetId) { this.rulesetId = rulesetId; }

    public Integer getRulesetVersion() { return rulesetVersion; }
    public void setRulesetVersion(Integer rulesetVersion) { this.rulesetVersion = rulesetVersion; }

    public RuleSet getRuleSet() { return ruleSet; }
    public void setRuleSet(RuleSet ruleSet) { this.ruleSet = ruleSet; }
}
