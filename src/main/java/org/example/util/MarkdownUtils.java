package org.example.util;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public final class MarkdownUtils {
  private static final Parser PARSER = Parser.builder().build();
  private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();

  private MarkdownUtils() {
  }

  public static String toHtml(String markdown) {
    if (markdown == null || markdown.trim().isEmpty()) {
      return "";
    }
    Node document = PARSER.parse(markdown);
    return RENDERER.render(document);
  }
}

