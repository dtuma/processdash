// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash.data.compiler;

import java.io.PushbackReader;
import java.io.StringReader;
import java.util.Map;

import pspdash.ResourcePool;
import pspdash.data.DateData;
import pspdash.data.ImmutableDoubleData;
import pspdash.data.ImmutableStringData;
import pspdash.data.StringData;
import pspdash.data.TagData;

import pspdash.data.compiler.lexer.*;
import pspdash.data.compiler.parser.*;
import pspdash.data.compiler.node.*;
import pspdash.data.compiler.analysis.*;


public class Compiler extends DepthFirstAdapter {

    private CompiledScript script = null;

    private static final ResourcePool compilerPool =
        new ResourcePool("compilerPool") {
                protected Object createNewResource() {
                    return new Compiler(); } };

    public Compiler() {}

    void setScript(CompiledScript script) {
        this.script = script;
    }

    public static CompiledScript compile(PValue expression)
        throws CompilationException
    {
        Compiler compiler = null;
        CompiledScript result = null;
        try {
            // get a compiler from the pool.
            compiler = (Compiler) compilerPool.get();

            // compile the expression into a CompiledScript object.
            result = new CompiledScript();
            compiler.setScript(result);
            expression.apply(compiler);
            compiler.setScript(null);
            result.setSaveString(expression.toString());
            result.commit();
        } finally {
            // release the compiler.
            compilerPool.release(compiler);
        }
        return result;
    }

    public static CompiledScript compile(String expression)
        throws CompilationException
    {
        try {
            // Create a Parser instance.
            Parser p = new Parser(new Lexer(new PushbackReader
                (new StringReader("[foo] = " + expression + ";"), 1024)));

            // Parse the input
            Start tree = p.parse();

            // get the expression
            FindLastExpression search = new FindLastExpression();
            tree.apply(search);

            // compile the expression and return it.
            return compile(search.expression);
        } catch (CompilationException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CompilationException("Error while compiling: " + e);
        }
    }


    public static CompiledScript exprAndDefined(CompiledScript s,
                                                String identifier) {
        CompiledScript result = new CompiledScript(s);
        result.add(FunctionCall.PUSH_STACK_MARKER);
        result.add(new PushVariable(identifier));
        result.add(FunctionCall.get("Defined"));
        result.add(LogicOperators.AND);
        result.commit();

        return result;
    }


    private void binaryExpression(Node left, Node op, Node right) {
        if (left  != null) left.apply(this);
        if (right != null) right.apply(this);
        if (op    != null) op.apply(this);
    }

    public void caseABinaryLevel7Expr(ABinaryLevel7Expr node)
    {
        inABinaryLevel7Expr(node);
        binaryExpression(node.getLeft(), node.getOp(), node.getRight());
        outABinaryLevel7Expr(node);
    }

    public void caseABinaryLevel6Expr(ABinaryLevel6Expr node)
    {
        inABinaryLevel6Expr(node);
        binaryExpression(node.getLeft(), node.getOp(), node.getRight());
        outABinaryLevel6Expr(node);
    }

    public void caseABinaryLevel5Expr(ABinaryLevel5Expr node)
    {
        inABinaryLevel5Expr(node);
        binaryExpression(node.getLeft(), node.getOp(), node.getRight());
        outABinaryLevel5Expr(node);
    }

    public void caseABinary1Level4Expr(ABinary1Level4Expr node)
    {
        inABinary1Level4Expr(node);
        binaryExpression(node.getLeft(), node.getOp(), node.getRight());
        outABinary1Level4Expr(node);
    }

    public void caseABinary2Level4Expr(ABinary2Level4Expr node)
    {
        inABinary2Level4Expr(node);
        binaryExpression(node.getLeft(), node.getOp(), node.getRight());
        outABinary2Level4Expr(node);
    }

    public void caseABinaryLevel3Expr(ABinaryLevel3Expr node)
    {
        inABinaryLevel3Expr(node);
        binaryExpression(node.getLeft(), node.getOp(), node.getRight());
        outABinaryLevel3Expr(node);
    }

    public void caseABinaryLevel2Expr(ABinaryLevel2Expr node)
    {
        inABinaryLevel2Expr(node);
        binaryExpression(node.getLeft(), node.getOp(), node.getRight());
        outABinaryLevel2Expr(node);
    }

    public void caseAUnaryNotLevel1Expr(AUnaryNotLevel1Expr node) {
        inAUnaryNotLevel1Expr(node);
        binaryExpression(null, node.getOp(), node.getRight());
        outAUnaryNotLevel1Expr(node);
    }

    private static final Instruction PUSH_ZERO =
        new PushConstant(ImmutableDoubleData.EDITABLE_ZERO);
    private static final Instruction PUSH_UNDEF_NUM =
        new PushConstant(ImmutableDoubleData.EDITABLE_UNDEF_NAN);

    public void caseAUnaryMinusLevel1Expr(AUnaryMinusLevel1Expr node) {
        inAUnaryMinusLevel1Expr(node);
        add(PUSH_ZERO);
        binaryExpression(null, node.getOp(), node.getRight());
        outAUnaryMinusLevel1Expr(node);
    }

    public void caseAFunctionCall(AFunctionCall node)
    {
        inAFunctionCall(node);
        add(FunctionCall.PUSH_STACK_MARKER);
        if(node.getArglist() != null)
            node.getArglist().apply(this);
        if(node.getFunctionName() != null)
            node.getFunctionName().apply(this);
        outAFunctionCall(node);
    }

    public void caseTMult(TMult node)   { add(MathOperators.MULTIPLY); }
    public void caseTDiv(TDiv node)     { add(MathOperators.DIVIDE);   }
    public void caseTPlus(TPlus node)   { add(MathOperators.ADD);      }
    public void caseTMinus(TMinus node) { add(MathOperators.SUBTRACT); }

    public void caseTConcat(TConcat node) { add(StringOperators.CONCAT); }
    public void caseTPathconcat(TPathconcat node) {
        add(StringOperators.PATHCONCAT); }

    public void caseTLogicAnd(TLogicAnd node) { add(LogicOperators.AND); }
    public void caseTLogicOr(TLogicOr node)   { add(LogicOperators.OR);  }
    public void caseTLogicNot(TLogicNot node) { add(LogicOperators.NOT); }

    public void caseTEq(TEq node)     { add(RelationalOperators.EQ);   }
    public void caseTNeq(TNeq node)   { add(RelationalOperators.NEQ);  }
    public void caseTLt(TLt node)     { add(RelationalOperators.LT);   }
    public void caseTLteq(TLteq node) { add(RelationalOperators.LTEQ); }
    public void caseTGt(TGt node)     { add(RelationalOperators.GT);   }
    public void caseTGteq(TGteq node) { add(RelationalOperators.GTEQ); }

    public void caseTFunctionName(TFunctionName node) {
        add(FunctionCall.get(node.getText())); }
    public void caseTIdentifier(TIdentifier node) {
        add(new PushVariable(trimDelim(node))); }
    public void caseTStringLiteral(TStringLiteral node) {
        add(new PushConstant(new ImmutableStringData(trimDelim(node)))); }
    public void caseTDateLiteral(TDateLiteral node) {
        try {
            add(new PushConstant(new DateData(node.getText())));
        } catch (Exception mve) {
            throw new CompilationException
                ("Couldn't parse date literal '" + node.getText() + "'.");
        }
    }
    public void caseAZeroTerm(AZeroTerm node) { add(PUSH_ZERO); }
    public void caseAUndefNumTerm(AUndefNumTerm node) { add(PUSH_UNDEF_NUM); }
    public void caseTNumberLiteral(TNumberLiteral node) {
        try {
            add(new PushConstant(new ImmutableDoubleData(node.getText())));
        } catch (Exception mve) {
            throw new CompilationException
                ("Couldn't parse number literal '" + node.getText() + "'.");
        }
    }
    public void caseTTag(TTag node) {
        add(new PushConstant(TagData.getInstance())); }
    public void caseTNull(TNull node) {
        add(new PushConstant(null)); }

    /** Convenience routine for adding an instruction to the script */
    private void add(Instruction i) { script.add(i); }

    /** Convenience routine: trims the first and last char from a string */
    public static String trimDelim(Token node) {
        return trimDelim(node.getText()); }
    public static String trimDelim(String text) {
        if (text == null) return null;
        int len = text.length();
        if (len < 2) return "";
        return StringData.unescapeString(text.substring(1, len-1));
    }
}

class FindLastExpression extends DepthFirstAdapter {
    PValue expression = null;
    public FindLastExpression() {}
    public void caseANewStyleDeclaration(ANewStyleDeclaration node) {
        expression = node.getValue();
    }
}
