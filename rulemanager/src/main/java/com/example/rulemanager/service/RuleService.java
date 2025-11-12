package com.example.rulemanager.service;

import com.example.rulemanager.model.RuleDefinition;
import com.example.rulemanager.model.RuleType;
import com.example.rulemanager.repository.RuleRepository;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Service
public class RuleService {

    private final RuleRepository repository;

    public RuleService(RuleRepository repository) {
        this.repository = repository;
    }

    /* ================== Persistence ================== */

    public List<RuleDefinition> getAll() {
        return repository.findAll();
    }

    public RuleDefinition save(RuleDefinition rule) {
        return repository.save(rule);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<RuleDefinition> getActiveRules() {
        return repository.findByActiveTrue();
    }

    public RuleDefinition getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    /* ================== Drools Engine ================== */

    private KieContainer buildKieContainer() throws IOException {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kfs = kieServices.newKieFileSystem();

        List<RuleDefinition> activeRules = getActiveRules();

        for (RuleDefinition rule : activeRules) {
            if (rule.getRuleType() == RuleType.DRL && rule.getRuleContent() != null) {
                kfs.write("src/main/resources/" + rule.getRuleName() + ".drl", rule.getRuleContent());

            } else if (rule.getRuleType() == RuleType.DECISION_TABLE && rule.getRuleFile() != null) {
                Resource res = kieServices.getResources()
                        .newInputStreamResource(new ByteArrayInputStream(rule.getRuleFile()));
                res.setResourceType(ResourceType.DTABLE);
                kfs.write("src/main/resources/" + rule.getRuleName() + ".xls", res);

            } else if (rule.getRuleType() == RuleType.DMN && rule.getRuleFile() != null) {
                Resource res = kieServices.getResources()
                        .newInputStreamResource(new ByteArrayInputStream(rule.getRuleFile()));
                res.setResourceType(ResourceType.DMN);
                kfs.write("src/main/resources/" + rule.getRuleName() + ".dmn", res);
            }
        }

        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();
        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Drools build errors: \n" + kieBuilder.getResults().toString());
        }

        return kieServices.newKieContainer(
                kieServices.getRepository().getDefaultReleaseId()
        );
    }

    public <T> T evaluate(T fact) throws IOException {
        KieContainer container = buildKieContainer();
        KieSession kieSession = container.newKieSession();

        kieSession.insert(fact);
        kieSession.fireAllRules();
        kieSession.dispose();

        return fact;
    }
}
