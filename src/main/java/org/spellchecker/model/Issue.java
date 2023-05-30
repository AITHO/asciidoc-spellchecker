package org.spellchecker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.languagetool.rules.RuleMatch;

@Data
@AllArgsConstructor
public class Issue {
    private RuleMatch ruleMatch;
    private SourceMap sourceMap;

}
