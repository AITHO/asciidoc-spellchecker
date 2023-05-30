package org.spellchecker.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SourceMap {
    private String text;
    private int sourceLine;
    private String sourceFile;

}
