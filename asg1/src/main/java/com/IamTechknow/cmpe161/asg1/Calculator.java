package com.IamTechknow.cmpe161.asg1;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Model class for calculator operations
 */

public class Calculator {
    //Enum for calculator operations
    public enum calcOps {
        NOP, ADD, SUB, MUL, DIV, SIN, COS, TAN
    }

    public static final String inputRegex = "([-])?(\\d{1,10})([-+*/])([-])?(\\d{1,10})";
    public static final double TRIG_ERROR = -100.0;
    public static final int DIVIDE_ZERO = 1, BAD_INPUT = 3;
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

    public double doCalc() {
        double result = 0.0;
        switch(currOp) {
            case ADD:
                result = firstNum + secondNum;
                break;
            case SUB:
                result = firstNum - secondNum;
                break;
            case MUL:
                result = firstNum * secondNum;
                break;
            case DIV:
                result = firstNum / secondNum;
                break;
        }
        return result;
    }

    public int errorCheck(double arg) {
        if(currOp == calcOps.DIV && arg == 0)
            return DIVIDE_ZERO;
        else if(currOp == calcOps.TAN && (arg-90.0) % 180.0 == 0.0)
            return 2;
        return 0;
    }

    //Use a regex expression to parse the two operands, return error code
    public int checkOp(String input) {

        //Apply regex
        Pattern p = Pattern.compile(inputRegex);
        Matcher m = p.matcher(input);

        if(m.matches()) { //do the matching
            //Check each group and set fields and op
            first = m.group(2);
            setCurrOp(m.group(3));
            second = m.group(5);
            firstNum = Double.parseDouble(first);
            secondNum = Double.parseDouble(second);

            return errorCheck(secondNum);
        } else
            return BAD_INPUT;
    }

    public double doTrigOp(String op, String input) {
        setCurrOp(op);
        double arg = Double.parseDouble(input);
        if(errorCheck(arg) == 2)
            result = TRIG_ERROR;
        else {
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
        }
        return result;
    }
}
