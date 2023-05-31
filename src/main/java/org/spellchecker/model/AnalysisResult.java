package org.spellchecker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.languagetool.rules.Rule;

import java.util.List;

@Data
@AllArgsConstructor
public class AnalysisResult {
    private List<Issue> issues;
    private List<Rule> rules;
}
