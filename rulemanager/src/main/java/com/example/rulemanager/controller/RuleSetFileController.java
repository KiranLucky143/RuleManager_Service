package com.example.rulemanager.controller;

import com.example.rulemanager.model.RuleDefinition;
import com.example.rulemanager.model.RuleSet;
import com.example.rulemanager.model.RuleType;
import com.example.rulemanager.repository.RuleRepository;
import com.example.rulemanager.repository.RuleSetRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/rulesets")
public class RuleSetFileController {

    private final RuleSetRepository ruleSetRepository;
    private final RuleRepository ruleRepository;

    public RuleSetFileController(RuleSetRepository ruleSetRepository,
                                 RuleRepository ruleRepository) {
        this.ruleSetRepository = ruleSetRepository;
        this.ruleRepository = ruleRepository;
    }

    /**
     * View all rules (files) under a given RuleSet
     */
    @GetMapping("/{id}/files-ui")
    public String showFiles(@PathVariable("id") Long rulesetId, Model model) {
        Optional<RuleSet> opt = ruleSetRepository.findById(rulesetId);
        if (opt.isEmpty()) {
            model.addAttribute("error", "Ruleset not found: " + rulesetId);
            return "ruleset-files";
        }

        RuleSet ruleSet = opt.get();
        List<RuleDefinition> rules = ruleRepository.findByRuleSet(ruleSet);

        model.addAttribute("ruleset", ruleSet);
        model.addAttribute("rules", rules);
        return "ruleset-files"; // Thymeleaf template
    }

    @GetMapping("/rulesets/{id}/create-rule-ui")
    public String showCreateRuleForm(@PathVariable("id") Long rulesetId, Model model) {
        Optional<RuleSet> opt = ruleSetRepository.findById(rulesetId);
        if (opt.isEmpty()) {
            model.addAttribute("error", "Ruleset not found: " + rulesetId);
            return "ruleset-files";
        }
        model.addAttribute("ruleset", opt.get());
        return "ruleset-create-rule";
    }

    @PostMapping("/rulesets/{id}/create-rule-ui")
    public String saveCreatedRule(@PathVariable("id") Long rulesetId,
                                  @RequestParam("ruleName") String ruleName,
                                  @RequestParam("ruleType") String ruleType,
                                  @RequestParam(value = "ruleContent", required = false) String ruleContent) {

        Optional<RuleSet> opt = ruleSetRepository.findById(rulesetId);
        if (opt.isEmpty()) {
            return "redirect:/rulesets";
        }

        RuleDefinition rd = new RuleDefinition();
        rd.setRuleName(ruleName);
        rd.setRuleType(RuleType.valueOf(ruleType));
        rd.setRuleContent(ruleContent);
        rd.setActive(true);
        rd.setRuleSet(opt.get());

        ruleRepository.save(rd);
        return "redirect:/rulesets/" + rulesetId + "/files-ui";
    }


    /**
     * Upload a new rule file and link it to an existing RuleSet
     */
    @PostMapping("/{id}/files-ui")
    public String uploadFile(@PathVariable("id") Long rulesetId,
                             @RequestParam("file") MultipartFile file,
                             Model model) {

        Optional<RuleSet> opt = ruleSetRepository.findById(rulesetId);
        if (opt.isEmpty()) {
            model.addAttribute("error", "Ruleset not found: " + rulesetId);
            return "ruleset-files";
        }

        RuleSet ruleSet = opt.get();

        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "Please choose a file to upload.");
            return "redirect:/rulesets/" + rulesetId + "/files-ui";
        }

        try {
            String fileName = file.getOriginalFilename();
            String lowerName = fileName != null ? fileName.toLowerCase() : "";

            RuleDefinition rule = new RuleDefinition();
            rule.setRuleName(fileName != null ? fileName : "unnamed");
            rule.setActive(true);
            rule.setRuleSet(ruleSet);

            if (lowerName.endsWith(".drl") || lowerName.endsWith(".dmn") || lowerName.endsWith(".txt")) {
                // Handle text-based rules
                String content = new String(file.getBytes(), StandardCharsets.UTF_8);
                rule.setRuleContent(content);
                rule.setRuleFile(null);
                rule.setRuleType(lowerName.endsWith(".dmn") ? RuleType.DMN : RuleType.DRL);
            } else {
                // Handle binary (Excel Decision Table)
                rule.setRuleFile(file.getBytes());
                rule.setRuleContent(null);
                rule.setRuleType(RuleType.DECISION_TABLE);
            }

            ruleRepository.save(rule);
            return "redirect:/rulesets/" + rulesetId + "/files-ui";

        } catch (Exception ex) {
            model.addAttribute("error", "Error while uploading file: " + ex.getMessage());
            return "ruleset-files";
        }
    }

    /**
     * Download rule file (text or binary). Triggers browser download when possible.
     */
    @GetMapping("/{rulesetId}/files/{fileId}/download")
    public ResponseEntity<byte[]> downloadRuleFile(@PathVariable("rulesetId") Long rulesetId,
                                                   @PathVariable("fileId") Long fileId) {
        Optional<RuleDefinition> opt = ruleRepository.findById(fileId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        RuleDefinition rule = opt.get();

        byte[] payload;
        String filename = rule.getRuleName() != null ? rule.getRuleName() : ("rule_" + fileId);
        String contentType = "application/octet-stream";

        if (rule.getRuleContent() != null) {
            payload = rule.getRuleContent().getBytes(StandardCharsets.UTF_8);
            // choose a text content-type for DRL/DMN
            if (rule.getRuleType() == RuleType.DMN) contentType = "application/xml";
            else contentType = "text/plain";
            // ensure filename has extension
            if (!filename.toLowerCase().endsWith(".drl") && !filename.toLowerCase().endsWith(".dmn")) {
                filename = filename + (rule.getRuleType() == RuleType.DMN ? ".dmn" : ".drl");
            }
        } else if (rule.getRuleFile() != null) {
            payload = rule.getRuleFile();
            contentType = "application/octet-stream";
        } else {
            return ResponseEntity.noContent().build();
        }

        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType(contentType))
                .body(payload);
    }

    /**
     * Show edit form for a specific rule file.
     * - If rule is DRL/DMN/text -> open DRL/DMN editor (textarea)
     * - If rule is DECISION_TABLE (binary) -> show replace file option
     */
    @GetMapping("/{rulesetId}/files/{fileId}/edit")
    public String editFileForm(@PathVariable("rulesetId") Long rulesetId,
                               @PathVariable("fileId") Long fileId,
                               Model model) {
        Optional<RuleDefinition> opt = ruleRepository.findById(fileId);
        if (opt.isEmpty()) {
            model.addAttribute("error", "Rule not found");
            return "ruleset-files";
        }

        RuleDefinition rule = opt.get();
        model.addAttribute("rulesetId", rulesetId);
        model.addAttribute("rule", rule);
        return "ruleset-file-edit";
    }

    /**
     * Save edited content for DRL/DMN/text rule in DB
     */
    @PostMapping("/{rulesetId}/files/{fileId}/edit")
    public String saveEditedFile(@PathVariable("rulesetId") Long rulesetId,
                                 @PathVariable("fileId") Long fileId,
                                 @RequestParam(value = "content", required = false) String content,
                                 Model model) {
        Optional<RuleDefinition> opt = ruleRepository.findById(fileId);
        if (opt.isEmpty()) {
            model.addAttribute("error", "Rule not found");
            return "redirect:/rulesets/" + rulesetId + "/files-ui";
        }
        RuleDefinition rule = opt.get();

        if (rule.getRuleType() == RuleType.DECISION_TABLE) {
            // Decision table editing not supported in text editor
            model.addAttribute("error", "Decision table editing not supported in text editor. Please replace the file.");
            return "redirect:/rulesets/" + rulesetId + "/files/" + fileId + "/edit";
        }

        // update content
        rule.setRuleContent(content);
        ruleRepository.save(rule);
        return "redirect:/rulesets/" + rulesetId + "/files-ui";
    }

    /**
     * Replace file content (upload a new file) for both binary and text rules.
     */
    @PostMapping("/{rulesetId}/files/{fileId}/replace")
    public String replaceFile(@PathVariable("rulesetId") Long rulesetId,
                              @PathVariable("fileId") Long fileId,
                              @RequestParam("file") MultipartFile file,
                              Model model) {
        Optional<RuleDefinition> opt = ruleRepository.findById(fileId);
        if (opt.isEmpty()) {
            model.addAttribute("error", "Rule not found");
            return "redirect:/rulesets/" + rulesetId + "/files-ui";
        }
        RuleDefinition rule = opt.get();

        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "Please choose a file to upload.");
            return "redirect:/rulesets/" + rulesetId + "/files/" + fileId + "/edit";
        }

        try {
            String filename = file.getOriginalFilename();
            String lower = filename == null ? "" : filename.toLowerCase();

            if (lower.endsWith(".drl") || lower.endsWith(".dmn") || lower.endsWith(".txt")) {
                String content = new String(file.getBytes(), StandardCharsets.UTF_8);
                rule.setRuleContent(content);
                rule.setRuleFile(null);
                rule.setRuleType(lower.endsWith(".dmn") ? RuleType.DMN : RuleType.DRL);
            } else {
                rule.setRuleFile(file.getBytes());
                rule.setRuleContent(null);
                rule.setRuleType(RuleType.DECISION_TABLE);
            }

            rule.setRuleName(filename);
            ruleRepository.save(rule);
            return "redirect:/rulesets/" + rulesetId + "/files-ui";
        } catch (Exception ex) {
            model.addAttribute("error", "Replace failed: " + ex.getMessage());
            return "redirect:/rulesets/" + rulesetId + "/files/" + fileId + "/edit";
        }
    }

    /**
     * Delete a rule from a ruleset (optional)
     */
    @PostMapping("/{rulesetId}/files/{fileId}/delete")
    public String deleteRule(@PathVariable("rulesetId") Long rulesetId,
                             @PathVariable("fileId") Long fileId) {
        ruleRepository.deleteById(fileId);
        return "redirect:/rulesets/" + rulesetId + "/files-ui";
    }
}
