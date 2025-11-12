package com.example.rulemanager.controller;

import com.example.rulemanager.model.RuleDefinition;
import com.example.rulemanager.model.RuleType;
import com.example.rulemanager.service.RuleService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Controller
public class RuleController {

    private final RuleService service;

    public RuleController(RuleService service) {
        this.service = service;
    }

    /* ================== UI Endpoints ================== */

    @GetMapping("/rules")
    public String listRules(Model model) {
        model.addAttribute("rules", service.getAll());
        return "rule-list"; // Thymeleaf template
    }

    @GetMapping("/rules/new")
    public String newRule(Model model) {
        model.addAttribute("rule", new RuleDefinition());
        return "rule-form"; // Thymeleaf template
    }

    @GetMapping("/rules/edit/{id}")
    public String editRule(@PathVariable Long id, Model model) {
        RuleDefinition rule = service.getById(id);
        model.addAttribute("rule", rule);
        return "rule-form";
    }

    @PostMapping("/rules")
    public String saveRule(@ModelAttribute RuleDefinition rule) throws IOException {

        if (rule.getRuleType() == null) {
            rule.setRuleType(RuleType.DRL); // default
        }

        if (rule.getRuleType() == RuleType.DRL) {
            // DRL rules are text only
            rule.setRuleFile(null);
        } else if (rule.getUploadedFile() != null && !rule.getUploadedFile().isEmpty()) {
            // Decision Table or DMN → store file as bytes
            rule.setRuleFile(rule.getUploadedFile().getBytes());
            rule.setRuleContent(null); // clear text
        }

        service.save(rule);
        return "redirect:/rules";
    }

    @GetMapping("/rules/delete/{id}")
    public String deleteRule(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/rules";
    }

    /* ================== File Download ================== */

    @GetMapping("/rules/download/{id}")
    public ResponseEntity<byte[]> downloadRuleFile(@PathVariable Long id) {
        RuleDefinition rule = service.getById(id);

        if (rule == null || rule.getRuleFile() == null) {
            return ResponseEntity.notFound().build();
        }

        String filename = rule.getRuleName() + (
                rule.getRuleType() == RuleType.DECISION_TABLE ? ".xlsx" :
                        rule.getRuleType() == RuleType.DMN ? ".dmn" : ".bin"
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(rule.getRuleFile());
    }

    /* ================== REST API Endpoints ================== */

    @GetMapping("/api/rules")
    @ResponseBody
    public List<RuleDefinition> getAllRules() {
        return service.getAll();
    }

    @GetMapping("/api/rules/active")
    @ResponseBody
    public List<RuleDefinition> getActiveRules() {
        return service.getActiveRules();
    }

}
