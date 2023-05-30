package org.spellchecker;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AsciidocSpellChecker {
    public static void main(String[] args) throws IOException, URISyntaxException {
        var directory = System.getenv("ASCIIDOC_PATH");
        var adocFile = System.getenv("ASCIIDOC_FILE");
        var langCode = System.getenv("ASCIIDOC_LANG");
        var wordsIgnoredFile = System.getenv("ASCIIDOC_WORDS_IGNORED");
        var sarifFile = System.getenv("ASCIIDOC_SARIF_FILE");
        List<String> wordsToIgnore = new ArrayList<>();
        try {
            String words = Files.readString(Paths.get(wordsIgnoredFile));
            wordsToIgnore = Arrays.stream(words.split("\n")).map(String::trim).collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Error reading words file " + wordsIgnoredFile);
        }

        AsciidocIssueFinder parser = new AsciidocIssueFinder(langCode, wordsToIgnore);
        var issues = parser.parseAsciidoc(adocFile, directory);
        SarifIssueConverter sarifConverter = new SarifIssueConverter();
        var sarif = sarifConverter.convert(issues);
        ObjectMapper om = new ObjectMapper();
        BufferedWriter writer = new BufferedWriter(new FileWriter(sarifFile));
        writer.write(om.writeValueAsString(sarif));

    }

}