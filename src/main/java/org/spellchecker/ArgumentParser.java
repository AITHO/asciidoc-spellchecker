package org.spellchecker;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.languagetool.Language;
import org.languagetool.Languages;
import org.spellchecker.model.AnalysisConfiguration;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ArgumentParser {
    public AnalysisConfiguration parse(String[] args) {
        var directory = System.getenv("ASCIIDOC_PATH");
        var adocFile = System.getenv("ASCIIDOC_FILE");
        var langCode = Optional.ofNullable(System.getenv("ASCIIDOC_LANG")).orElse("en-US");
        var wordsIgnoredFile = System.getenv("ASCIIDOC_WORDS_IGNORED");
        var sarifFile = System.getenv("ASCIIDOC_SARIF_FILE");

        net.sourceforge.argparse4j.inf.ArgumentParser parser = ArgumentParsers.newFor("asciidoctor-spellcheck").build()
                .description("Perform a spellcheck analysis on an AsciiDoc document.");
        parser.addArgument("-l", "--language")
                .choices(Languages.get().stream().map(Language::getShortCodeWithCountryAndVariant).collect(Collectors.toList()))
                .setDefault(langCode)
                .help("specify the language to use (default: en-US)");
        parser.addArgument("-d", "--directory")
                .setDefault(directory)
                .help("specify the base directory containing the Asciidoc files");
        parser.addArgument("-i", "--words-ignored")
                .setDefault(wordsIgnoredFile)
                .help("The file containing the ignored words");
        parser.addArgument("-s", "--sarif-path")
                .setDefault(sarifFile)
                .help("The path where save the Sarif file");
        parser.addArgument("file").nargs("?")
                .help("File to spellcheck")
                .setDefault(adocFile)
                .required(false);

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }


        directory = ns.getString("directory");
        adocFile = ns.getString("file");
        langCode = ns.getString("language");
        wordsIgnoredFile = ns.getString("words_ignored");
        sarifFile = ns.getString("sarif_path");

        if (directory == null) {
            parser.printUsage();
            System.err.println("asciidoctor-spellcheck: error: argument -d/--directory is required");
            System.exit(1);
        }
        if (adocFile == null) {
            parser.printUsage();
            System.err.println("asciidoctor-spellcheck: error: argument file is required");
            System.exit(1);
        }
        if (sarifFile == null) {
            parser.printUsage();
            System.err.println("asciidoctor-spellcheck: error: argument -s/--sarif-path is required");
            System.exit(1);
        }
        List<String> wordsToIgnore = new ArrayList<>();
        try {
            String words = Files.readString(Paths.get(wordsIgnoredFile));
            wordsToIgnore = Arrays.stream(words.split("\n")).map(String::trim).collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Error reading words file " + wordsIgnoredFile);
        }

        return AnalysisConfiguration
                .builder()
                .adocFile(adocFile)
                .directory(directory)
                .langCode(langCode)
                .sarifFile(sarifFile)
                .wordsToIgnore(wordsToIgnore)
                .build();
    }
}
