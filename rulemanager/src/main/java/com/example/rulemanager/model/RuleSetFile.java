package com.example.rulemanager.model;

import jakarta.persistence.*;

@Entity
@Table(name = "RULESET_FILE")
public class RuleSetFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long rulesetId;

    private String fileName;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content; // for text DRL/DMN

    @Lob
    private byte[] contentBytes; // optional for xlsx or binary

    private Integer orderIndex = 0;

    public RuleSetFile() {}

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRulesetId() { return rulesetId; }
    public void setRulesetId(Long rulesetId) { this.rulesetId = rulesetId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public byte[] getContentBytes() { return contentBytes; }
    public void setContentBytes(byte[] contentBytes) { this.contentBytes = contentBytes; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
}
