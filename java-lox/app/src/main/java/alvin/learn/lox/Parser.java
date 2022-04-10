package alvin.learn.lox;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static alvin.learn.lox.TokenType.BANG;
import static alvin.learn.lox.TokenType.BANG_EQUAL;
import static alvin.learn.lox.TokenType.CLASS;
import static alvin.learn.lox.TokenType.COLON;
import static alvin.learn.lox.TokenType.COMMA;
import static alvin.learn.lox.TokenType.EOF;
import static alvin.learn.lox.TokenType.EQUAL_EQUAL;
import static alvin.learn.lox.TokenType.FALSE;
import static alvin.learn.lox.TokenType.FOR;
import static alvin.learn.lox.TokenType.FUN;
import static alvin.learn.lox.TokenType.GREATER;
import static alvin.learn.lox.TokenType.GREATER_EQUAL;
import static alvin.learn.lox.TokenType.IF;
import static alvin.learn.lox.TokenType.LEFT_PAREN;
import static alvin.learn.lox.TokenType.LESS;
import static alvin.learn.lox.TokenType.LESS_EQUAL;
import static alvin.learn.lox.TokenType.MINUS;
import static alvin.learn.lox.TokenType.NIL;
import static alvin.learn.lox.TokenType.NUMBER;
import static alvin.learn.lox.TokenType.PLUS;
import static alvin.learn.lox.TokenType.PRINT;
import static alvin.learn.lox.TokenType.QUESTION;
import static alvin.learn.lox.TokenType.RETURN;
import static alvin.learn.lox.TokenType.RIGHT_PAREN;
import static alvin.learn.lox.TokenType.SEMICOLON;
import static alvin.learn.lox.TokenType.SLASH;
import static alvin.learn.lox.TokenType.STAR;
import static alvin.learn.lox.TokenType.STRING;
import static alvin.learn.lox.TokenType.TRUE;
import static alvin.learn.lox.TokenType.VAR;
import static alvin.learn.lox.TokenType.WHILE;

class Parser {

  private static class ParseError extends RuntimeException {}

  private static final Set<TokenType> binarySet = Stream
    .of(
      COMMA, // commaExpr
      BANG_EQUAL,
      EQUAL_EQUAL, // equality
      GREATER,
      GREATER_EQUAL,
      LESS,
      LESS_EQUAL, // comparison
      MINUS,
      PLUS, // term
      SLASH,
      STAR // factor
    )
    .collect(Collectors.toCollection(HashSet::new));

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  Expr parse() {
    try {
      return expression();
    } catch (ParseError error) {
      return null;
    }
  }

  private Expr expression() {
    return commaExpr();
  }

  private Expr commaExpr() {
    Expr expr = equality();

    while (match(COMMA)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr equality() {
    Expr expr = ternary();

    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = ternary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr ternary() {
    Expr expr = comparison();

    while (match(QUESTION)) {
      Token question = previous();
      Expr truly = comparison();
      consume(COLON, "Expect ':' after expression.");
      Token colon = previous();
      Expr falsely = comparison();
      expr = new Expr.Ternary(expr, question, truly, colon, falsely);
    }

    return expr;
  }

  private Expr comparison() {
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr term() {
    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr factor() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return primary();
  }

  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    if (isBinaryOperator(peek())) {
      Token op = peek();
      if (match(COMMA)) equality();
      if (match(EQUAL_EQUAL, BANG_EQUAL)) ternary();
      if (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) term();
      if (match(MINUS, PLUS)) factor();
      if (match(SLASH, STAR)) unary();
      throw error(op, "Binary operation require left expression.");
    }

    throw error(peek(), "Expect expression.");
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();

    throw error(peek(), message);
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  private boolean isBinaryOperator(Token token) {
    return binarySet.contains(token.type);
  }

  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }

  private boolean match(TokenType... types) {
    if (check(types)) {
      advance();
      return true;
    }

    return false;
  }

  private boolean check(TokenType... types) {
    if (isAtEnd()) return false;
    return Arrays.asList(types).contains(peek().type);
  }

  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }
}