package org.metalib.papifly.fx.code.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.code.lexer.Token;
import org.metalib.papifly.fx.code.lexer.TokenType;

import java.util.List;
import java.util.Objects;

/**
 * Paints tokenized text content for visible lines.
 */
final class TextPass implements RenderPass {

    @Override
    public void renderFull(RenderContext context) {
        GraphicsContext gc = context.graphics();
        gc.setFont(context.glyphCache().getFont());
        for (RenderLine renderLine : context.renderLines()) {
            drawTokenizedLine(context, renderLine);
        }
    }

    @Override
    public void renderLine(RenderContext context, RenderLine renderLine) {
        GraphicsContext gc = context.graphics();
        gc.setFont(context.glyphCache().getFont());
        drawTokenizedLine(context, renderLine);
    }

    private void drawTokenizedLine(RenderContext context, RenderLine renderLine) {
        GraphicsContext gc = context.graphics();
        String text = renderLine.text();
        if (text.isEmpty()) {
            return;
        }
        Paint foreground = context.theme().editorForeground();
        gc.setFill(foreground);
        gc.fillText(text, 0, renderLine.y() + context.baseline());
        List<Token> tokens = renderLine.tokens();
        if (tokens.isEmpty()) {
            return;
        }

        int runStart = -1;
        int runEnd = -1;
        Paint runColor = null;
        int textLength = text.length();
        for (Token token : tokens) {
            int start = Math.max(0, Math.min(token.startColumn(), textLength));
            int end = Math.max(start, Math.min(token.endColumn(), textLength));
            if (end <= start) {
                continue;
            }
            Paint color = tokenColor(context, token.type());
            if (samePaint(color, foreground)) {
                if (runColor != null) {
                    drawSegment(context, text, runStart, runEnd, renderLine.y(), runColor);
                    runStart = -1;
                    runEnd = -1;
                    runColor = null;
                }
                continue;
            }
            if (runColor != null && samePaint(runColor, color) && start <= runEnd) {
                runEnd = Math.max(runEnd, end);
                continue;
            }
            if (runColor != null) {
                drawSegment(context, text, runStart, runEnd, renderLine.y(), runColor);
            }
            runStart = start;
            runEnd = end;
            runColor = color;
        }
        if (runColor != null) {
            drawSegment(context, text, runStart, runEnd, renderLine.y(), runColor);
        }
    }

    private void drawSegment(
        RenderContext context,
        String text,
        int startColumn,
        int endColumn,
        double y,
        Paint color
    ) {
        if (endColumn <= startColumn) {
            return;
        }
        GraphicsContext gc = context.graphics();
        gc.setFill(color);
        gc.fillText(
            text.substring(startColumn, endColumn),
            startColumn * context.charWidth(),
            y + context.baseline()
        );
    }

    private boolean samePaint(Paint a, Paint b) {
        return Objects.equals(a, b);
    }

    private Paint tokenColor(RenderContext context, TokenType tokenType) {
        if (tokenType == null) {
            return context.theme().editorForeground();
        }
        return switch (tokenType) {
            case KEYWORD -> context.theme().keywordColor();
            case STRING -> context.theme().stringColor();
            case COMMENT -> context.theme().commentColor();
            case NUMBER -> context.theme().numberColor();
            case BOOLEAN -> context.theme().booleanColor();
            case NULL_LITERAL -> context.theme().nullLiteralColor();
            case HEADLINE -> context.theme().headlineColor();
            case LIST_ITEM -> context.theme().listItemColor();
            case CODE_BLOCK -> context.theme().codeBlockColor();
            case TEXT -> context.theme().editorForeground();
            default -> context.theme().editorForeground();
        };
    }
}
