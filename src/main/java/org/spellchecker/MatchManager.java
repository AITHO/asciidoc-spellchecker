package org.spellchecker;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.languagetool.JLanguageTool;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.spelling.SpellingCheckRule;
import org.spellchecker.model.InlineIgnoredRule;
import org.spellchecker.model.Issue;
import org.spellchecker.model.Match;
import org.spellchecker.model.SourceMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@AllArgsConstructor
public class MatchManager {
    private String sourceFile;
    private String sourceDir;

    public Optional<Issue> handleMatch(SourceMap sourceMap, List<String> rulesToIgnore, RuleMatch ruleMatch, List<InlineIgnoredRule> inlineIgnoredRules, List<JLanguageTool> altLangTools) {
        String foundText = sourceMap.getText().substring(ruleMatch.getFromPos(), ruleMatch.getToPos());
        if (ignoreRuleMatch(sourceMap, rulesToIgnore, ruleMatch, inlineIgnoredRules, foundText)){
            return Optional.empty();
        }

        // when the RuleMatch to handle is due to a SpellingCheckRule, check if foundText is valid for AT LEAST one of the alternative languages
        if(ruleMatch.getRule() instanceof SpellingCheckRule) {

            List<List<RuleMatch>> altLangRuleMatches = new ArrayList<>();

            try {
                for (var altLangTool : altLangTools) {
                    altLangRuleMatches.add(altLangTool.check(foundText));
                }
            } catch (IOException e) {
                System.out.println("error during source position double check");
                e.printStackTrace();
            }

            // if found text is valid for AT LEAST one of the alternative languages, the corresponding List<RuleMatch> must be empty
            for (var ruleMatches : altLangRuleMatches) {
                if (ruleMatches.isEmpty()) {
                    return Optional.empty();
                }
            }
        }


        Match match = new Match();
        match.setRuleMatch(ruleMatch);
        match.setToPos(ruleMatch.getToPos());
        match.setFromPos(ruleMatch.getFromPos());
        if (sourceMap.getSourceFile().equals("<stdin>")) {
            sourceMap.setSourceFile(StringUtils.removeStart(StringUtils.removeStart(StringUtils.removeStart(sourceFile, sourceDir), "\\"), "\\"));
        }
        try (Stream<String> lines = Files.lines(Paths.get(sourceDir + "/" + sourceMap.getSourceFile()))) {
            int lineNo = Math.max(sourceMap.getSourceLine() - 1, 0);
            lines.skip(lineNo).findFirst().ifPresent(line -> {
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
        System.out.println("Potential error in file " + sourceMap.getSourceFile() + " on line " + (sourceMap.getSourceLine()+1) + " column " +
                (match.getFromPos()+1) + "-" + match.getToPos() + " " + foundText + ": " +
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
                .filter(el -> el.getText().contains(StringUtils.trim(foundText)) || foundText.contains(StringUtils.trim(el.getText())))
                .toList();
        for (var possibleInlineRule : possibleInlineIgnoreRules) {
            var startOffset = StringUtils.indexOf(possibleInlineRule.getText(), foundText) >= 0 ? StringUtils.indexOf(possibleInlineRule.getText(), foundText) : StringUtils.indexOf(foundText, possibleInlineRule.getText());
            var starting = ruleMatch.getFromPos() - startOffset;
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
