package org.spellchecker;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.ListItem;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;
import org.jsoup.nodes.Document;
import org.spellchecker.model.*;
import org.jsoup.Jsoup;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.spelling.morfologik.MorfologikSpellerRule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AsciidocIssueFinder {

    private final JLanguageTool langTool;

    private final Asciidoctor asciidoctor;

    private MatchManager matchManager;

    private List<Issue> issues;

    public AsciidocIssueFinder(String locale, List<String> wordsIgnored) {
        langTool = new JLanguageTool(Languages.getLanguageForShortCode(locale));
        for (Rule rule : langTool.getAllActiveRules()) {
            if (rule.getId().equals("MORFOLOGIK_RULE_"+locale.toUpperCase().replace("-", "_"))) {
                ((MorfologikSpellerRule) rule).addIgnoreTokens(wordsIgnored);
            }
        }

        asciidoctor = Asciidoctor.Factory.create();
    }

    public AnalysisResult parseAsciidoc(String file, String path) throws IOException {
        Options options = Options
                .builder()
                .safe(SafeMode.SAFE)
                .sourceDir(new File(path))
                .baseDir(new File(path))
                .sourcemap(true)
                .build();
        this.matchManager = new MatchManager(file, path);
        var document = asciidoctor.load(Files.readString(Path.of(file)), options);
        issues = new ArrayList<>();
        process(document);
        System.out.println("Analysis found " + issues.size() + " issues");
        return new AnalysisResult(issues, langTool.getAllActiveRules());
    }

    private void process(StructuralNode block) throws IOException {
        List<StructuralNode> blocks = block.getBlocks();

        for (final StructuralNode currentBlock : blocks) {
            checkBlock(currentBlock);
        }
    }

    private void checkBlock(StructuralNode currentBlock) throws IOException {
        if (currentBlock != null) {
            SourceMap sourceMap = null;
            if (currentBlock instanceof ListItem list) {
                sourceMap = new SourceMap(list.getText(), list.getSourceLocation().getLineNumber(), list.getSourceLocation().getPath());
            } else
            if (currentBlock instanceof Section section) {
                sourceMap = new SourceMap(section.getTitle(), section.getSourceLocation().getLineNumber(), section.getSourceLocation().getPath());

            } else
            if (currentBlock.getContent() instanceof String content) {
                sourceMap = new SourceMap(content, currentBlock.getSourceLocation().getLineNumber(), currentBlock.getSourceLocation().getPath());

            }

            if (sourceMap != null && sourceMap.getText() != null && !sourceMap.getText().isEmpty()) {
                List<String> rulesToIgnore = extractRuleToIgnore(currentBlock);
                int currentLine = sourceMap.getSourceLine();
                var subLines = sourceMap.getText().split("\n");
                for (var subLine : subLines){
                    SourceMap subSourceMap = new SourceMap(subLine, currentLine++, sourceMap.getSourceFile());
                    List<InlineIgnoredRule> inlineIgnoredRules = processSourceMap(subSourceMap);
                    List<RuleMatch> matches = langTool.check(subSourceMap.getText());
                    for (RuleMatch match : matches) {
                        matchManager.handleMatch(subSourceMap, rulesToIgnore, match, inlineIgnoredRules)
                                .ifPresent(issues::add);
                    }
                }

            }

            if ((currentBlock.getBlocks() != null && !currentBlock.getBlocks().isEmpty())) {
                process(currentBlock);
            }
        }
    }

    private List<String> extractRuleToIgnore(StructuralNode currentBlock) {
        List<String> toReturn = new ArrayList<>();
        var ignoreAttribute = currentBlock.getAttribute("ignore");
        if (ignoreAttribute instanceof String rules) {
            toReturn.addAll(List.of(rules.split(" ")));
        }
        return toReturn;
    }




    private List<InlineIgnoredRule> processSourceMap(SourceMap sourceMap) {
        List<InlineIgnoredRule> inlineIgnoredRules = new ArrayList<>();
        Document parsed = Jsoup.parse(sourceMap.getText());
        for (var element : parsed.select(".ignore")) {
            InlineIgnoredRule inlineIgnoredRule = new InlineIgnoredRule();
            inlineIgnoredRule.setRules(element.classNames());
            inlineIgnoredRule.setText(element.text());
            inlineIgnoredRules.add(inlineIgnoredRule);
        }
        sourceMap.setText(parsed.text());
        return inlineIgnoredRules;
    }
}
