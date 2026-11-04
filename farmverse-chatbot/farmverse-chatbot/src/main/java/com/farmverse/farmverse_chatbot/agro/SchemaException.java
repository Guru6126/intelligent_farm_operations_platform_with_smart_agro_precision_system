package com.farmverse.farmverse_chatbot.agro;

import java.util.List;

public class SchemaException extends Exception {

    private static final long serialVersionUID = 1L;

    private final transient List<String> problems;

    public SchemaException(List<String> problems) {
        super("agroprecision schema problem: " + String.join(" | ", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> problems() {
        return problems;
    }
}
