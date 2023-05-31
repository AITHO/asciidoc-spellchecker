package org.spellchecker;

import com.contrastsecurity.sarif.*;
import org.spellchecker.model.AnalysisResult;
import org.spellchecker.model.Issue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class SarifIssueConverter {

    public SarifSchema210 convert(AnalysisResult analysisResult) throws URISyntaxException {
        List<Result> results = new ArrayList<>();
        for (var issue : analysisResult.getIssues()) {
            Result result = new Result();
            result.setLevel(Result.Level.WARNING);
            result.setMessage(new Message().withText(generateMessage(issue)));
            result.setLocations(
                    List.of(
                            new Location()
                                    .withPhysicalLocation(
                                            new PhysicalLocation()
                                                    .withArtifactLocation(
                                                            new ArtifactLocation()
                                                                    .withUri(issue.getSourceMap().getSourceFile()
                                                                    )
                                                    )
                                                    .withRegion(new Region()
                                                            .withStartLine(issue.getSourceMap().getSourceLine())
                                                            .withStartColumn(issue.getMatch().getFromPos()+1)
                                                            .withEndColumn(issue.getMatch().getToPos()+1)
                                                    )
                                    )
                    )
            );
            result.setRuleId(issue.getMatch().getRuleMatch().getRule().getId());
            results.add(result);
        }
        Set<ReportingDescriptor> rules = new HashSet<>();
        for (var rule : analysisResult.getRules()) {
            rules.add(new ReportingDescriptor()
                    .withName(rule.getDescription())
                    .withId(rule.getId())
                    .withDefaultConfiguration(new ReportingConfiguration()
                            .withLevel(ReportingConfiguration.Level.NOTE)
                    )
            );
        }

        return new SarifSchema210()
                .withVersion(SarifSchema210.Version._2_1_0)
                .with$schema(new URI("http://json.schemastore.org/sarif-2.1.0-rtm.5"))
                .withRuns(List.of(new Run()
                                .withResults(results)
                                .withTool(new Tool()
                                        .withDriver(new ToolComponent()
                                                .withRules(rules)
                                                .withVersion("1.0")
                                                .withInformationUri(new URI("https://github.com/AITHO/asciidoc-spellchecker"))
                                                .withName("asciidoc-spell-checker")
                                        )

                                                                        )
                        )
                );

    }

    private String generateMessage(Issue issue) {
        StringBuffer sb = new StringBuffer()
                .append(issue.getMatch().getRuleMatch().getMessage())
                .append(": ")
                .append(issue.getSourceMap().getText(), issue.getMatch().getRuleMatch().getFromPos(), issue.getMatch().getRuleMatch().getToPos())
                .append(". ");
        Optional.ofNullable(issue.getMatch().getRuleMatch().getSuggestedReplacements()).orElse(new ArrayList<>()).stream().findFirst().ifPresent(suggestion -> sb.append("Maybe you mean ").append(suggestion).append("?"));
        return sb.toString();
    }
}
