package org.spellchecker.model;

import lombok.Data;
import java.util.Set;

@Data
public class InlineIgnoredRule {
    String text;
    Set<String> rules;
}
