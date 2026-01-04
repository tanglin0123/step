# Formula Parser with ANTLR

This package demonstrates how to use **ANTLR** (Another Tool for Language Recognition) to parse formulas into an **Abstract Syntax Tree (AST)** using the **Visitor Pattern**.

## Overview

The implementation provides:

1. **ANTLR Grammar**: Defines syntax for formulas including operators, literals, cell references, and function calls
2. **AST Node Classes**: Represent different components (operators, values, references, functions)
3. **Visitor Pattern**: For traversing and analyzing the AST with multiple operations
4. **Parser Utility**: Main entry point for parsing formulas
5. **Sample Implementations**: Demonstrate practical uses of the parser

## Key Components

### Grammar File
- **[Formula.g4](../../antlr4/com/lintang/formula/Formula.g4)** - ANTLR 4 grammar definition

### AST Node Classes
- **[ASTNode.java](ast/ASTNode.java)** - Base class for all AST nodes
- **[ASTVisitor.java](ast/ASTVisitor.java)** - Visitor interface
- **[BinaryOpNode.java](ast/BinaryOpNode.java)** - Binary operations (+, -, *, /, ^)
- **[UnaryOpNode.java](ast/UnaryOpNode.java)** - Unary operations (-, +)
- **[NumberNode.java](ast/NumberNode.java)** - Numeric literals
- **[StringNode.java](ast/StringNode.java)** - String literals
- **[BooleanNode.java](ast/BooleanNode.java)** - Boolean literals
- **[CellRefNode.java](ast/CellRefNode.java)** - Cell references (e.g., A1, B2)
- **[CellRangeNode.java](ast/CellRangeNode.java)** - Cell ranges (e.g., A1:B10)
- **[FunctionCallNode.java](ast/FunctionCallNode.java)** - Function calls (e.g., SUM())

### Parser Implementation
- **[FormulaParserUtil.java](FormulaParserUtil.java)** - Main parser utility class
- **[FormulaASTBuilder.java](FormulaASTBuilder.java)** - Custom visitor for AST construction

### Visitor Implementations
- **[EvaluationVisitor.java](visitor/EvaluationVisitor.java)** - Evaluates numeric expressions
- **[CellReferenceExtractor.java](visitor/CellReferenceExtractor.java)** - Extracts cell references
- **[FormulaStringBuilder.java](visitor/FormulaStringBuilder.java)** - Converts AST back to formula string

### Sample Programs
- **[FormulaParserSample.java](FormulaParserSample.java)** - Comprehensive demo with all features
- **[SimpleParserTest.java](SimpleParserTest.java)** - Basic parsing test

## How It Works

### 1. Parsing a Formula

```java
// Parse a formula string to AST
ASTNode ast = FormulaParserUtil.parse("2 + 3 * 4");

// The result is an AST representing the formula structure
// BinaryOpNode(+)
//   ├─ NumberNode(2)
//   └─ BinaryOpNode(*)
//       ├─ NumberNode(3)
//       └─ NumberNode(4)
```

### 2. Using the Visitor Pattern

```java
// Define a custom visitor to analyze/transform the AST
public class MyVisitor implements ASTVisitor<ResultType> {
    @Override
    public ResultType visit(BinaryOpNode node) { /* ... */ }
    @Override
    public ResultType visit(NumberNode node) { /* ... */ }
    // ... implement for other node types
}

// Use the visitor on the AST
ASTNode ast = FormulaParserUtil.parse(formula);
ResultType result = ast.accept(new MyVisitor());
```

## Usage Examples

### Example 1: Extract Cell References
```java
ASTNode ast = FormulaParserUtil.parse("SUM(A1:A10) + B5 * C3");
CellReferenceExtractor extractor = new CellReferenceExtractor();
ast.accept(extractor);
Set<String> cells = extractor.getCellReferences();
// Result: {A1:A10, B5, C3}
```

### Example 2: Evaluate Arithmetic
```java
ASTNode ast = FormulaParserUtil.parse("(10 + 5) * 2");
EvaluationVisitor evaluator = new EvaluationVisitor();
double result = ast.accept(evaluator);
// Result: 30.0
```

### Example 3: Format/Pretty-Print
```java
ASTNode ast = FormulaParserUtil.parse("2+3*4");
FormulaStringBuilder builder = new FormulaStringBuilder();
String formatted = ast.accept(builder);
// Result: "(2.0 + (3.0 * 4.0))"
```

## Supported Formula Features

### Operators
- **Arithmetic**: `+`, `-`, `*`, `/`, `^` (exponentiation)
- **Operator precedence**: Standard mathematical precedence with parentheses support

### Literals
- **Numbers**: `123`, `3.14`, `.5`
- **Strings**: `"hello"`, `"text"`
- **Booleans**: `TRUE`, `FALSE`

### Cell References
- **Single cells**: `A1`, `B2`, `Z10`
- **Ranges**: `A1:B10`, `Sheet1!A1:C3`

### Functions
- **Any function name with parameters**: `SUM()`, `IF()`, `MAX()`, `MIN()`, etc.
- **Variable argument count**: `SUM(A1:A10)` or `SUM(A1, A2, A3)`

### Examples of Valid Formulas
- `2 + 3 * 4`
- `(10 - 5) / 2`
- `A1 * B1 + C1`
- `SUM(A1:A10)`
- `IF(A1 > 0, "positive", "negative")`
- `MAX(A1:A10) - MIN(B1:B10)`
- `-5 * (3 + 2)`
- `2 ^ 3 + 1`

## Running the Samples

### Build the project
```bash
cd LinTangJavaLib
mvn clean compile
```

### Run the full sample
```bash
java -cp target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout) \
  com.lintang.formula.FormulaParserSample
```

### Expected Output
```
=== Excel Formula Parser Sample with ANTLR ===

Formula: 2 + 3 * 4
  AST: BinaryOpNode{operator='+', left=NumberNode{value=2.0}, right=BinaryOpNode{operator='*', ...}}
  Evaluated Result: 14.0
  Reconstructed: (2.0 + (3.0 * 4.0))

Formula: SUM(A1:A10) + B5
  AST: BinaryOpNode{operator='+', left=FunctionCallNode{...}, right=CellRefNode{...}}
  Cell References: [B5, A1:A10]
  Evaluation: Function evaluation not fully implemented
  Reconstructed: (SUM(A1:A10) + B5)
```

## Extending the Parser

### Add a New Grammar Rule
Edit `Formula.g4` to add new syntax:
```antlr
percentage
    : NUMBER '%'
    ;
```

### Create New AST Node
```java
public class PercentageNode extends ASTNode {
    private final double value;
    
    public PercentageNode(double value) {
        this.value = value;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
```

### Add Visitor Method
```java
public interface ASTVisitor<T> {
    T visit(PercentageNode node);
    // ... other methods
}
```

### Update Visitor Implementation
```java
public class FormulaASTBuilder extends FormulaBaseVisitor<ASTNode> {
    @Override
    public ASTNode visitPercentage(FormulaParser.PercentageContext ctx) {
        double value = Double.parseDouble(ctx.NUMBER().getText());
        return new PercentageNode(value / 100.0);
    }
}
```

### Rebuild
```bash
mvn clean compile
```

## ANTLR Generation

The build automatically generates ANTLR files from the grammar:

Generated files (in `target/generated-sources/antlr4/com/lintang/formula/`):
- `FormulaParser.java` - Generated parser
- `FormulaLexer.java` - Generated lexer
- `FormulaBaseVisitor.java` - Generated base visitor
- `FormulaListener.java` / `FormulaBaseListener.java` - Alternative listener pattern

## Maven Configuration

The `pom.xml` includes:
- ANTLR 4 runtime dependency
- ANTLR Maven plugin for code generation
- Compiler configuration for Java 17

```xml
<plugin>
    <groupId>org.antlr</groupId>
    <artifactId>antlr4-maven-plugin</artifactId>
    <version>4.13.1</version>
    <configuration>
        <visitor>true</visitor>
        <listener>false</listener>
    </configuration>
</plugin>
```

## Architecture Diagram

```
Input Formula String
       ↓
  FormulaLexer (Tokenization)
       ↓
 CommonTokenStream
       ↓
FormulaParser (Parsing)
       ↓
    Parse Tree
       ↓
FormulaASTBuilder (Visitor)
       ↓
      AST
       ↓
   Custom Visitors
   ├─ EvaluationVisitor
   ├─ CellReferenceExtractor
   └─ FormulaStringBuilder
       ↓
   Results
```

## Key Design Patterns

### 1. Visitor Pattern
Enables multiple operations on the AST without modifying node classes:
- Separation of concerns
- Easy to add new operations
- Type-safe traversal

### 2. Builder Pattern
`FormulaASTBuilder` constructs the AST from the parse tree

### 3. Strategy Pattern
Different visitors implement different algorithms on the same AST

## Performance Considerations

- **Parsing**: O(n) where n is the formula length
- **AST Traversal**: O(m) where m is the number of nodes
- **Memory**: Proportional to formula complexity

## Limitations and Future Enhancements

Current limitations:
- Function evaluation requires external data (cell values)
- No support for named ranges (yet)
- String comparisons not fully evaluated

Possible enhancements:
- Add more built-in functions (IF, COUNT, AVERAGE, etc.)
- Support array formulas
- Support more Excel functions
- Optimize AST construction
- Add error recovery in parser

## References

- [ANTLR Official Documentation](https://www.antlr.org/)
- [Visitor Pattern](https://refactoring.guru/design-patterns/visitor)
- [Excel Formula Specification](https://support.microsoft.com/en-us/office/excel-functions-by-category)
- [Language Implementation Patterns by Terence Parr](https://pragprog.com/titles/tpantlr2/the-definitive-antlr-4-reference/)

## License

Part of LinTangJavaLib - Shared Java utilities library for LinTang Lambda functions

