package com.IamTechknow.cmpe161.asg1;

/**
 * Model class for calculator operations
 */

public class Calculator {
    //Enum for calculator operations
    public enum calcOps {
        NOP, ADD, SUB, MUL, DIV, SIN, COS, TAN
    }

    private double firstNum, secondNum, result;
    private String first, second;
    private calcOps currOp;

    public Calculator() {
        currOp = calcOps.NOP;
        firstNum = secondNum = result = 0.0;
        first = second = null;
    }

    public void setCurrOp(String op) {
        switch(op) {
            case "+":
                currOp = calcOps.ADD;
                break;
            case "-":
                currOp = calcOps.SUB;
                break;
            case "*":
                currOp = calcOps.MUL;
                break;
            case "/":
                currOp = calcOps.DIV;
                break;
            case "cos":
                currOp = calcOps.COS;
                break;
            case "sin":
                currOp = calcOps.SIN;
                break;
            case "tan":
                currOp = calcOps.TAN;
                break;
        }
    }

    //Use a regex expression to parse the two operands
    public double doOp(String input) {
        return 0;
    }

    public double doTrigOp(String op, String input) {
        setCurrOp(op);
        double arg = Double.parseDouble(input);
        switch (currOp) {
            case COS: //cosine
                result = Math.cos(arg * Math.PI / 180);
                break;
            case SIN: //sine
                result = Math.sin(arg * Math.PI / 180);
                break;
            case TAN: //tangent
                result = Math.tan(arg * Math.PI / 180);
                break;
            default: //invalid op
                result = 0.0;
                break;
        }
        currOp = calcOps.NOP;
        return result;
    }
}
