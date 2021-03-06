package org.projectfloodlight.db.yang;

import java.util.List;

import org.projectfloodlight.db.ParserException;

public class YangSchemaParsingException extends ParserException {
    
    private static final long serialVersionUID = 6591331000339084715L;
    
    public YangSchemaParsingException(String message, List<String> parseErrors) {
        super(message, parseErrors);
    }
}
