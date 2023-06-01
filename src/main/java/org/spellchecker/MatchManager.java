package org.spellchecker;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.languagetool.rules.RuleMatch;
import org.spellchecker.model.InlineIgnoredRule;
import org.spellchecker.model.Issue;
import org.spellchecker.model.Match;
import org.spellchecker.model.SourceMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@AllArgsConstructor
public class MatchManager {
    private String sourceFile;
    private String sourceDir;

    public Optional<Issue> handleMatch(SourceMap sourceMap, List<String> rulesToIgnore, RuleMatch ruleMatch, List<InlineIgnoredRule> inlineIgnoredRules) {
        String foundText = sourceMap.getText().substring(ruleMatch.getFromPos(), ruleMatch.getToPos());
        if (ignoreRuleMatch(sourceMap, rulesToIgnore, ruleMatch, inlineIgnoredRules, foundText)){
            return Optional.empty();
        }

        Match match = new Match();
        match.setRuleMatch(ruleMatch);
        match.setToPos(ruleMatch.getToPos());
        match.setFromPos(ruleMatch.getFromPos());
        if (sourceMap.getSourceFile().equals("<stdin>")) {
            sourceMap.setSourceFile(StringUtils.removeStart(StringUtils.removeStart(StringUtils.removeStart(sourceFile, sourceDir), "\\"), "\\"));
        }
        try (Stream<String> lines = Files.lines(Paths.get(sourceDir + "/" + sourceMap.getSourceFile()))) {
            lines.skip(sourceMap.getSourceLine()-1).findFirst().ifPresent(line -> {
                for (var i = ruleMatch.getFromPos(); i < line.length(); i++) {
                    if (StringUtils.equals(StringUtils.substring(line, i, (ruleMatch.getToPos() - ruleMatch.getFromPos()) + i), foundText)) {
                        match.setToPos((ruleMatch.getToPos() - ruleMatch.getFromPos()) + i);
                        match.setFromPos(i);
                        break;

                    }
                }
            });
        } catch (IOException e) {
            System.out.println("error during source position double check");
            e.printStackTrace();
        }
        System.out.println("Potential error in file " + sourceMap.getSourceFile() + " on line " + sourceMap.getSourceLine() + " column " +
                match.getFromPos() + "-" + match.getToPos() + " " + foundText + ": " +
                ruleMatch.getMessage());
        System.out.println("Rule ID: " +
                ruleMatch.getRule().getId());
        System.out.println("Rule DESC: " +
                ruleMatch.getRule().getDescription());
        System.out.println("Suggested correction(s): " +
                ruleMatch.getSuggestedReplacements());
        System.out.println("---");
        return Optional.of(new Issue(match, sourceMap));
    }

    private boolean ignoreRuleMatch(SourceMap sourceMap, List<String> rulesToIgnore, RuleMatch ruleMatch, List<InlineIgnoredRule> inlineIgnoredRules, String foundText) {
        if (rulesToIgnore.contains(ruleMatch.getRule().getId()) || rulesToIgnore.contains("ALL_RULES")) {
            return true;
        }

        var possibleInlineIgnoreRules = inlineIgnoredRules
                .stream()
                .filter(el -> el.getText().contains(foundText))
                .toList();
        for (var possibleInlineRule : possibleInlineIgnoreRules) {
            var starting = ruleMatch.getFromPos() - StringUtils.indexOf(possibleInlineRule.getText(), foundText);
            var ending = possibleInlineRule.getText().length() + starting;
            if (StringUtils.substring(sourceMap.getText(), starting, ending).equals(possibleInlineRule.getText())) {
                if (possibleInlineRule.getRules().contains(ruleMatch.getRule().getId()) || possibleInlineRule.getRules().contains("ALL_RULES")) {
                    return true;
                }
            }

        }
        return false;
    }
}
