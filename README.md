# Asciidoc Spell Checker
A Java application using jAsciidoctor and LanguageTool to spellcheck AsciiDoc files and produce a SARIF json file that can be used with SonarQube.

## Motivation
At my company  [AITHO](https://aitho.it) we decided to take a Documentation as Code approach. We wanted to spell check documents written in Asciidoc via pipeline and observe the results on SonarQube. 

We didn't find any that fit the bill, so we decided to write it ourselves.

## Usage

You need Java 17 or later.

Download the JAR from the release page and run

```
java -jar ascidooc-spellchecker.jar -h
```

To see the available arguments option.

### Arguments

| Argument              | Environment Variable   | Description                                                                  |
|-----------------------|------------------------|------------------------------------------------------------------------------|
| file                  | ASCIIDOC_FILE          | An Asciidoc file                                                             |
| --language or -l      | ASCIIDOC_LANG          | The language of the document in the format language-COUNTRY (default en-US)  |
| --directory or -d     | ASCIIDOC_PATH          | THe directory containing the Asciidoc documents                              |
| --words-ignored or -i | ASCIIDOC_WORDS_IGNORED | The path to a file containing a list of word to ignore in the spell checking |
| --sarif-path or -s    | SARIF_PATH             | The path to the file where the SARIF analysis will be saved                  |

Command line arguments always take precedence over the environment variables.

### Examples:

```
java -jar ascidooc-spellchecker.jar book.adoc -d /documents -l it_IT -i /documents/wordsIgnored.txt -s /documents/book.sarif.json
```
Is equivalent to

```
ASCIIDOC_LANG=it-IT
ASCIIDOC_PATH=/documents
ASCIIDOC_FILE=/documents/book.adoc
ASCIIDOC_WORDS_IGNORED=/documents/wordsIgnored.txt
ASCIIDOC_SARIF_FILE=/documents/book.sarif.json

java -jar ascidooc-spellchecker.jar
```


## Using docker

You can use the `aithogg/asciidoc-spellchecker` Docker image from DockerHub.

`docker run -it --rm -v ".:/documents/" -e ASCIIDOC_LANG=it-IT -e ASCIIDOC_PATH=/documents -e ASCIIDOC_FILE=/documents/my_document.adoc -e ASCIIDOC_WORDS_IGNORED=/documents/wordsIgnored.txt -e ASCIIDOC_SARIF_FILE=/documents/my_document.sarif.json aithogg/asciidoc-spellchecker`

## Rule manipulation
### Ignored words
You can add word to the dictionary creating a file with one word per line and use it as value in the `ASCIIDOC_WORDS_IGNORED`

### Ignored rules
#### Block
You can exclude rules from a paragraph/section using a custom attribute `ignore`:

```
[ignore="RULE_01_002 ANOTHER_03_001"]
The rules in ignore will not trigger
```

To ignore all rules use the "ALL_RULES" value:

```
[ignore="ALL_RULES"]
All rules will be ignored
```

#### Inline
You can ignore rules in formatted elements using this syntax

```
[ignore UPPERCASE_SENTENCE_START RULE_2]*mvn compile;*
```

Use `ALL_RULES` to ignore all rules.

## Disclaimer
This program was created in a hurry, has no automatic test or coverage checks.

We are using a "build and fix" approach, patching bugs and introduce features as we need them for processing our documents. 

It's not production ready but should not have any game breaker bugs.

Every contribution in the form of Issue or Merge Request is welcome! 

