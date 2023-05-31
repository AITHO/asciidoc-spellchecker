package org.spellchecker.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Issue {
    private Match match;
    private SourceMap sourceMap;

}
