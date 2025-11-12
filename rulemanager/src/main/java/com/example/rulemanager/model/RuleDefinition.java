package com.example.rulemanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.web.multipart.MultipartFile;

@Entity
@Table(name = "RULE_DEFINITION")
public class RuleDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ruleName;

    @Enumerated(EnumType.STRING)
    private RuleType ruleType = RuleType.DRL; // DRL, DECISION_TABLE, DMN

    @Lob
    @Column(name = "rule_content", columnDefinition = "LONGTEXT")
    private String ruleContent;

    @Lob
    private byte[] ruleFile; // stored in DB

    private boolean active = true;

    /**
     * Relationship to RuleSet entity.
     * Marked with @JsonIgnore to avoid recursive JSON serialization and lazy-loading issues.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_set_id")
    @JsonIgnore
    private RuleSet ruleSet;

    public RuleSet getRuleSet() { return ruleSet; }
    public void setRuleSet(RuleSet ruleSet) { this.ruleSet = ruleSet; }

    // --- transient field for form upload (not persisted in DB) ---
    @Transient
    private MultipartFile uploadedFile;

    // --- getters/setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public RuleType getRuleType() { return ruleType; }
    public void setRuleType(RuleType ruleType) { this.ruleType = ruleType; }

    public String getRuleContent() { return ruleContent; }
    public void setRuleContent(String ruleContent) { this.ruleContent = ruleContent; }

    public byte[] getRuleFile() { return ruleFile; }
    public void setRuleFile(byte[] ruleFile) { this.ruleFile = ruleFile; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public MultipartFile getUploadedFile() { return uploadedFile; }
    public void setUploadedFile(MultipartFile uploadedFile) { this.uploadedFile = uploadedFile; }
}
