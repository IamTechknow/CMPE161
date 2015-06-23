package com.IamTechknow.cmpe161.asg1;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageButton;

public class CalcActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setActionBar(toolbar);
    }

    //Enum for calculator operations
    public enum calcOps {
        NOP, ADD, SUB, MUL, DIV, SIN, COS, TAN
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        private EditText mCalcField;
        private String first, second;
        private calcOps currOp = calcOps.NOP;
        private double firstNum, secondNum;
        private Button b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, bMul, bDiv, bSub, bAdd, bClear, bCos, bSin, bTan, bPi, bEquals;
        private ImageButton bDel;

        public PlaceholderFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_calc, container, false);

            mCalcField = (EditText) v.findViewById(R.id.editText);
            b0 = (Button) v.findViewById(R.id.zero);
            b1 = (Button) v.findViewById(R.id.one);
            b2 = (Button) v.findViewById(R.id.two);
            b3 = (Button) v.findViewById(R.id.three);
            b4 = (Button) v.findViewById(R.id.four);
            b5 = (Button) v.findViewById(R.id.five);
            b6 = (Button) v.findViewById(R.id.six);
            b7 = (Button) v.findViewById(R.id.seven);
            b8 = (Button) v.findViewById(R.id.eight);
            b9 = (Button) v.findViewById(R.id.nine);
            bMul = (Button) v.findViewById(R.id.multiply);
            bDiv = (Button) v.findViewById(R.id.divide);
            bAdd = (Button) v.findViewById(R.id.add);
            bSub = (Button) v.findViewById(R.id.subtract);
            bClear = (Button) v.findViewById(R.id.clear);
            bDel = (ImageButton) v.findViewById(R.id.delete);
            bCos = (Button) v.findViewById(R.id.cos);
            bSin = (Button) v.findViewById(R.id.sin);
            bTan = (Button) v.findViewById(R.id.tan);
            bPi = (Button) v.findViewById(R.id.pi);
            bEquals = (Button) v.findViewById(R.id.equals);

            //Set listeners for numeric buttons
            b0.setOnClickListener(createListener(0));
            b1.setOnClickListener(createListener(1));
            b2.setOnClickListener(createListener(2));
            b3.setOnClickListener(createListener(3));
            b4.setOnClickListener(createListener(4));
            b5.setOnClickListener(createListener(5));
            b6.setOnClickListener(createListener(6));
            b7.setOnClickListener(createListener(7));
            b8.setOnClickListener(createListener(8));
            b9.setOnClickListener(createListener(9));
            bAdd.setOnClickListener(createOpListener("+"));
            bSub.setOnClickListener(createOpListener("-"));
            bMul.setOnClickListener(createOpListener("*"));
            bDiv.setOnClickListener(createOpListener("/"));

            return v;
        }

        private View.OnClickListener createListener(final int value) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCalcField.setText(mCalcField.getText().toString() + Integer.toString(value));
                }
            };
        }

        private View.OnClickListener createOpListener(final String op) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCalcField.setText(mCalcField.getText().toString() + op);

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
            };
        }
    }
}
