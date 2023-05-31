package org.spellchecker.model;

import lombok.Data;
import org.languagetool.rules.RuleMatch;

@Data
public class Match {
    private int toPos;
    private int fromPos;
    private RuleMatch ruleMatch;
}
