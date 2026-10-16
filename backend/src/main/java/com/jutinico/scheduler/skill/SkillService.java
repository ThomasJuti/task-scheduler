package com.jutinico.scheduler.skill;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.jutinico.scheduler.common.Names;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillService {

    private final SkillRepository skills;

    public SkillService(SkillRepository skills) {
        this.skills = skills;
    }

    @Transactional(readOnly = true)
    public List<String> listNames() {
        return skills.findAllByOrderByNameAsc().stream().map(Skill::getName).toList();
    }

    /**
     * Returns the skill entities for the given names, creating the ones that do not exist yet.
     * Names are normalized (trimmed, whitespace collapsed) and de-duplicated case-insensitively.
     */
    @Transactional
    public Set<Skill> resolve(Collection<String> names) {
        Map<String, String> byKey = new LinkedHashMap<>();
        for (String raw : names) {
            String name = Names.normalize(raw);
            byKey.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
        }
        Map<String, Skill> existing = skills.findByKeys(byKey.keySet()).stream()
                .collect(Collectors.toMap(Skill::key, Function.identity()));

        Set<Skill> result = new LinkedHashSet<>();
        byKey.forEach((key, name) -> result.add(existing.computeIfAbsent(key, k -> skills.save(new Skill(name)))));
        return result;
    }
}
