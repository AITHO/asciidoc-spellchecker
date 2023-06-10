package org.spellchecker.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AnalysisConfiguration {
    private String directory;
    private String adocFile;
    private String langCode;
    private List<String> wordsToIgnore;
    private String sarifFile;
}
