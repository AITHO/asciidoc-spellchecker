package org.spellchecker;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

public class AsciidocSpellChecker {
    public static void main(String[] args) throws IOException, URISyntaxException {
        ArgumentParser argumentParser = new ArgumentParser();
        var analysisConfiguration = argumentParser.parse(args);

        AsciidocIssueFinder finder = new AsciidocIssueFinder(analysisConfiguration.getLangCode(), analysisConfiguration.getWordsToIgnore());
        var result = finder.parseAsciidoc(analysisConfiguration.getAdocFile(), analysisConfiguration.getDirectory());
        SarifIssueConverter sarifConverter = new SarifIssueConverter();
        var sarif = sarifConverter.convert(result);
        ObjectMapper om = new ObjectMapper();
        om.writeValue(Paths.get(analysisConfiguration.getSarifFile()).toFile(), sarif);

    }

}