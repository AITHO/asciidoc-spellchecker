package org.spellchecker;

import com.contrastsecurity.sarif.*;
import org.spellchecker.model.Issue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SarifIssueConverter {

    public SarifSchema210 convert(List<Issue> issues) throws URISyntaxException {
        List<Result> results = new ArrayList<>();
        for (var issue : issues) {
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
                                                            .withStartLine(issue.getSourceMap().getSourceLine()+1)
                                                            .withStartColumn(issue.getRuleMatch().getFromPos()+1)
                                                            .withEndColumn(issue.getRuleMatch().getToPos())
                                                    )
                                    )
                    )
            );
            result.setRuleId(issue.getRuleMatch().getRule().getId());
            results.add(result);
        }
        return new SarifSchema210()
                .withVersion(SarifSchema210.Version._2_1_0)
                .with$schema(new URI("http://json.schemastore.org/sarif-2.1.0-rtm.5"))
                .withRuns(List.of(new Run()
                                .withResults(results)
                                .withTool(new Tool()
                                        .withDriver(new ToolComponent()
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
                .append(issue.getRuleMatch().getMessage())
                .append("\nFile ")
                .append(issue.getSourceMap().getSourceFile())
                .append(" on line ")
                .append(issue.getSourceMap().getSourceLine())
                .append(" at position ")
                .append(issue.getRuleMatch().getFromPos())
                .append("-")
                .append(issue.getRuleMatch().getToPos())
                .append(" ")
                .append(issue.getSourceMap().getText(), issue.getRuleMatch().getFromPos(), issue.getRuleMatch().getToPos())
                .append(".");
        Optional.ofNullable(issue.getRuleMatch().getSuggestedReplacements()).orElse(new ArrayList<>()).stream().findFirst().ifPresent(suggestion -> sb.append(suggestion).append("?"));
        return sb.toString();
    }
}
