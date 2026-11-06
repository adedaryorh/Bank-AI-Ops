package com.bankops.ai;
import java.util.regex.Pattern;
public final class ContentSafety {
    private ContentSafety() {}
    private static final Pattern INJECTION=Pattern.compile(
        "(?is)(ignore|disregard|override|forget).{0,60}(instruction|prompt|rule|previous)|system\\s*(prompt|message)|"+
        "(reveal|exfiltrate|print|send).{0,60}(secret|password|api.key|token)|<\\|?(system|assistant)|"+
        "(call|execute|invoke).{0,30}(tool|shell|command)|jailbreak");
    public static boolean unsafe(String text) { return text==null || INJECTION.matcher(text).find(); }
    public static String text(byte[] data) {
        try {
            String value=java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(data)).toString();
            if(value.isBlank() || value.indexOf(0)>=0) throw new IllegalArgumentException("Upload a non-empty UTF-8 text document.");
            return value;
        } catch(java.nio.charset.CharacterCodingException ex) { throw new IllegalArgumentException("Only UTF-8 text documents are supported."); }
    }
}
