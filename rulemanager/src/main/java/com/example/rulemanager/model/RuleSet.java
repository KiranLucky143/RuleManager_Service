package com.example.rulemanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "RULE_SET")
public class RuleSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // logical name for the ruleset

    @Column(length = 2000)
    private String description;

    private Integer version = 1;

    private String status = "DRAFT"; // DRAFT or PUBLISHED

    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // One-to-many relationship to RuleDefinition (mappedBy = "ruleSet")
    @OneToMany(mappedBy = "ruleSet", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // Prevent recursion and large nested JSON responses
    private List<RuleDefinition> rules = new ArrayList<>();

    public RuleSet() {}

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<RuleDefinition> getRules() { return rules; }
    public void setRules(List<RuleDefinition> rules) { this.rules = rules; }

    // Helpers
    public void addRule(RuleDefinition rule) {
        rules.add(rule);
        rule.setRuleSet(this);
    }

    public void removeRule(RuleDefinition rule) {
        rules.remove(rule);
        rule.setRuleSet(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RuleSet)) return false;
        RuleSet ruleSet = (RuleSet) o;
        return Objects.equals(id, ruleSet.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
