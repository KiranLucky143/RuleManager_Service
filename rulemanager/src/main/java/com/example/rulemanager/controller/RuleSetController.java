package com.example.rulemanager.controller;

import com.example.rulemanager.model.*;
import com.example.rulemanager.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

@RestController
@RequestMapping("/api")
public class RuleSetController {

    @Autowired
    private RuleSetRepository ruleSetRepository;

    // reuse existing RuleDefinition storage instead of creating new RuleSetFile
    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private ObjectRulesetMappingRepository mappingRepository;

    // ===================== READ endpoints (ADDITIVE, safe) =====================

    /**
     * List all rulesets (used by UI).
     */
    @GetMapping("/rulesets")
    public ResponseEntity<List<RuleSet>> listRuleSets() {
        List<RuleSet> list = ruleSetRepository.findAll();
        return ResponseEntity.ok(list);
    }

    /**
     * Get details for a ruleset, plus a 'files' array describing contained rules.
     * The response shape is:
     * {
     *   "ruleset": { ... },
     *   "files": [ { id, fileName, ruleType, orderIndex }, ... ]
     * }
     */
    @GetMapping("/rulesets/{id}")
    public ResponseEntity<?> getRuleSetDetails(@PathVariable Long id) {
        Optional<RuleSet> opt = ruleSetRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        RuleSet rs = opt.get();
        List<RuleDefinition> rules = ruleRepository.findByRuleSet(rs);

        Map<String,Object> payload = new HashMap<>();
        payload.put("ruleset", rs);
        payload.put("files", rules.stream().map(r -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("fileName", r.getRuleName());
            m.put("ruleType", r.getRuleType());
            m.put("orderIndex", 0);
            return m;
        }).collect(Collectors.toList()));

        return ResponseEntity.ok(payload);
    }

    // ===================== WRITE / action endpoints (existing behavior) =====================

    // Create a ruleset (draft)
    @PostMapping("/rulesets")
    public ResponseEntity<?> createRuleSet(@RequestBody RuleSet dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            return ResponseEntity.badRequest().body("ruleset name is required");
        }
        Optional<RuleSet> existing = ruleSetRepository.findByName(dto.getName());
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body("ruleset with same name already exists");
        }
        dto.setStatus(Optional.ofNullable(dto.getStatus()).orElse("DRAFT"));
        dto.setVersion(Optional.ofNullable(dto.getVersion()).orElse(1));
        RuleSet saved = ruleSetRepository.save(dto);
        return ResponseEntity.ok(saved);
    }

    /**
     * Upload/add a rule file to a ruleset.
     * We create a new RuleDefinition and associate it to the RuleSet.
     * For text files (.drl/.dmn) we store as ruleContent; for binary (xlsx) we store as ruleFile bytes.
     */
    @PostMapping("/rulesets/{rulesetId}/files")
    public ResponseEntity<?> uploadRuleFile(@PathVariable Long rulesetId,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "orderIndex", required = false, defaultValue = "0") Integer orderIndex) {
        Optional<RuleSet> opt = ruleSetRepository.findById(rulesetId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        RuleSet rs = opt.get();

        try {
            String filename = file.getOriginalFilename();
            String lower = filename == null ? "" : filename.toLowerCase();

            RuleDefinition rd = new RuleDefinition();
            rd.setRuleName(filename == null ? "unnamed" : filename);
            rd.setActive(true);

            if (lower.endsWith(".drl") || lower.endsWith(".dmn") || lower.endsWith(".txt")) {
                // store text content as ruleContent
                String content = new String(file.getBytes(), StandardCharsets.UTF_8);
                rd.setRuleContent(content);
                rd.setRuleType(lower.endsWith(".dmn") ? RuleType.DMN : RuleType.DRL);
                rd.setRuleFile(null);
            } else {
                // binary (e.g., xlsx decision table)
                rd.setRuleFile(file.getBytes());
                rd.setRuleContent(null);
                rd.setRuleType(RuleType.DECISION_TABLE);
            }

            // associate to ruleset (RuleDefinition.ruleSet field expected)
            rd.setRuleSet(rs);

            // save the rule definition
            RuleDefinition saved = ruleRepository.save(rd);
            return ResponseEntity.ok(saved);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("error storing file: " + ex.getMessage());
        }
    }

    // Publish ruleset (simple: set status PUBLISHED and increment version)
    @PostMapping("/rulesets/{rulesetId}/publish")
    public ResponseEntity<?> publish(@PathVariable Long rulesetId) {
        Optional<RuleSet> opt = ruleSetRepository.findById(rulesetId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        RuleSet rs = opt.get();
        rs.setStatus("PUBLISHED");
        rs.setVersion(rs.getVersion() == null ? 1 : rs.getVersion() + 1);
        ruleSetRepository.save(rs);
        return ResponseEntity.ok(rs);
    }

    /**
     * Map object -> ruleset. We store mapping with rulesetId (non-destructive).
     * If your ObjectRulesetMapping model uses a ruleSet relation instead, adapt accordingly.
     */
    @PostMapping("/objects/{objectType}/{objectKey}/mapping")
    public ResponseEntity<?> mapObject(@PathVariable String objectType,
                                       @PathVariable String objectKey,
                                       @RequestParam Long rulesetId,
                                       @RequestParam(required = false) Integer version) {

        Optional<RuleSet> rs = ruleSetRepository.findById(rulesetId);
        if (rs.isEmpty()) return ResponseEntity.badRequest().body("ruleset not found");

        // use repository method that might be present (try multiple patterns safely)
        ObjectRulesetMapping m = null;
        try {
            // if repository has findFirstByObjectTypeAndObjectKey
            m = mappingRepository.findFirstByObjectTypeAndObjectKey(objectType, objectKey);
        } catch (NoSuchMethodError | AbstractMethodError ex) {
            // ignore — fall back
        } catch (Exception e) {
            // repository might return null
        }

        if (m == null) {
            // try Optional-style method (if repository uses Optional)
            try {
                Optional<ObjectRulesetMapping> opt = mappingRepository.findByObjectTypeAndObjectKey(objectType, objectKey);
                m = opt.orElse(null);
            } catch (Exception e) {
                // last resort: try findByObjectType and filter (less ideal)
                List<ObjectRulesetMapping> list = mappingRepository.findByObjectType(objectType);
                for (ObjectRulesetMapping mm : list) {
                    if (objectKey.equals(mm.getObjectKey())) {
                        m = mm; break;
                    }
                }
            }
        }

        if (m == null) m = new ObjectRulesetMapping();

        // prefer storing rulesetId if model has that field (non-destructive)
        try {
            m.setRulesetId(rulesetId);
        } catch (NoSuchMethodError err) {
            // model might use ruleSet relation; try to set it
            try {
                m.setRuleSet(rs.get());
            } catch (Throwable t) {
                // ignore
            }
        }

        if (version != null) m.setRulesetVersion(version);
        mappingRepository.save(m);
        return ResponseEntity.ok(m);
    }

    @DeleteMapping("/rulesets/{id}")
    @Transactional
    public ResponseEntity<?> deleteRuleset(@PathVariable Long id) {
        return ruleSetRepository.findById(id)
                .map(rs -> {
                    // remove any object→ruleset mappings first (separate table)
                    try { mappingRepository.deleteByRulesetId(id); } catch (Exception ignore) {}

                    // RuleSet → RuleDefinition uses cascade = ALL, orphanRemoval = true
                    ruleSetRepository.delete(rs);
                    return ResponseEntity.noContent().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Update ruleset (name/description/status). Non-breaking: POST with JSON.
     * Body example: { "name": "New Name", "description": "New desc", "status": "DRAFT|PUBLISHED" }
     */
    @PostMapping("/rulesets/{id}/update")
    @Transactional
    public ResponseEntity<?> updateRuleset(@PathVariable Long id, @RequestBody RuleSet dto) {
        Optional<RuleSet> opt = ruleSetRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        RuleSet rs = opt.get();

        // name change → uniqueness check
        if (dto.getName() != null && !dto.getName().isBlank()) {
            String incoming = dto.getName().trim();
            if (!incoming.equalsIgnoreCase(rs.getName())) {
                Optional<RuleSet> byName = ruleSetRepository.findByName(incoming);
                if (byName.isPresent() && !byName.get().getId().equals(id)) {
                    return ResponseEntity.badRequest().body("ruleset with same name already exists");
                }
                rs.setName(incoming);
            }
        }

        if (dto.getDescription() != null) {
            rs.setDescription(dto.getDescription());
        }

        // allow toggling status (optional)
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            String st = dto.getStatus().trim().toUpperCase();
            if (st.equals("DRAFT") || st.equals("PUBLISHED")) {
                rs.setStatus(st);
            }
        }

        // updatedAt handled by @PreUpdate in entity (you already have it)
        ruleSetRepository.save(rs);
        return ResponseEntity.ok(rs);
    }

    /**
     * Endpoint used by DroolsEngine to fetch rules for an object.
     * This returns an array of objects compatible with RuleDefinition shape:
     * { ruleName, ruleContent, active, ruleType }
     */
    @GetMapping("/objects/{objectType}/{objectKey}/rules")
    public ResponseEntity<?> getRulesForObject(@PathVariable String objectType, @PathVariable String objectKey) {
        // try different repository method signatures safely

        ObjectRulesetMapping m = null;
        try {
            m = mappingRepository.findFirstByObjectTypeAndObjectKey(objectType, objectKey);
        } catch (Exception e) {
            // ignore and try optional variant
        }
        if (m == null) {
            try {
                Optional<ObjectRulesetMapping> opt = mappingRepository.findByObjectTypeAndObjectKey(objectType, objectKey);
                m = opt.orElse(null);
            } catch (Exception ex) {
                // fallback: try findByObjectType and filter
                List<ObjectRulesetMapping> list = mappingRepository.findByObjectType(objectType);
                for (ObjectRulesetMapping mm : list) {
                    if (objectKey.equals(mm.getObjectKey())) { m = mm; break; }
                }
            }
        }

        if (m == null) {
            return ResponseEntity.notFound().build();
        }

        // load RuleDefinitions for the mapped ruleset
        Long rulesetId = m.getRulesetId();
        RuleSet rs = null;
        if (rulesetId != null) {
            rs = ruleSetRepository.findById(rulesetId).orElse(null);
        } else {
            // mapping may store RuleSet relation
            try { rs = m.getRuleSet(); } catch (Throwable t) { rs = null; }
        }

        if (rs == null) {
            return ResponseEntity.notFound().build();
        }

        List<RuleDefinition> rules = ruleRepository.findByRuleSet(rs);
        if (rules == null || rules.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Map<String,Object>> defs = rules.stream().map(r -> {
            Map<String,Object> map = new HashMap<>();
            map.put("ruleName", r.getRuleName());
            if (r.getRuleContent() != null) map.put("ruleContent", r.getRuleContent());
            else if (r.getRuleFile() != null) map.put("ruleContent", Base64.getEncoder().encodeToString(r.getRuleFile()));
            map.put("active", r.isActive());
            // map RuleType enum to string expected by DroolsEngine
            map.put("ruleType", r.getRuleType() == RuleType.DMN ? "DMN" :
                    r.getRuleType() == RuleType.DECISION_TABLE ? "DECISION_TABLE" : "DRL");
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(defs);
    }
}
