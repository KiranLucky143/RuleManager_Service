package com.example.rulemanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiController {

    /**
     * Simple endpoint to render the Rule Sets UI (Thymeleaf template at
     * src/main/resources/templates/ruleset-list.html).
     */
    @GetMapping("/rulesets-ui")
    public String rulesetUI() {
        return "ruleset-list";
    }
}
