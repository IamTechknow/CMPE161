package com.IamTechknow.cmpe161.asg1;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

public class CalcActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new CalcFragment())
                    .commit();
        }

        FrameLayout layout = (FrameLayout) findViewById(R.id.container);

        //Allow keyboard to be hidden when area outside EditText is touched
        //from: http://karimvarela.com/2012/07/24/android-how-to-hide-keyboard-by-touching-screen-outside-keyboard/
        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return false;
            }
        });
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class CalcFragment extends Fragment {
        public static final double TRIG_ERROR = -100.0;
        public static final int DIVIDE_ZERO = 1, BAD_INPUT = 3;
        private String histTop, histBottom;
        private EditText mCalcField;
        private TextView mHistory;
        private Button b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, bMul, bDiv, bSub, bAdd, bClear, bCos, bSin, bTan, bDot, bEquals;
        private ImageButton bDel;
        private Calculator calc;

        public CalcFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            histTop = histBottom = null;
            calc = new Calculator();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_calc, container, false);

            mCalcField = (EditText) v.findViewById(R.id.editText);
            mHistory = (TextView) v.findViewById(R.id.textView);
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
            bDot = (Button) v.findViewById(R.id.pi);
            bEquals = (Button) v.findViewById(R.id.equals);

            //Set listeners for numeric buttons
            b0.setOnClickListener(createListener('0'));
            b1.setOnClickListener(createListener('1'));
            b2.setOnClickListener(createListener('2'));
            b3.setOnClickListener(createListener('3'));
            b4.setOnClickListener(createListener('4'));
            b5.setOnClickListener(createListener('5'));
            b6.setOnClickListener(createListener('6'));
            b7.setOnClickListener(createListener('7'));
            b8.setOnClickListener(createListener('8'));
            b9.setOnClickListener(createListener('9'));
            bDot.setOnClickListener(createListener('.'));
            bAdd.setOnClickListener(createOpListener("+"));
            bSub.setOnClickListener(createOpListener("-"));
            bMul.setOnClickListener(createOpListener("*"));
            bDiv.setOnClickListener(createOpListener("/"));
            bSin.setOnClickListener(createTrigListener("sin"));
            bCos.setOnClickListener(createTrigListener("cos"));
            bTan.setOnClickListener(createTrigListener("tan"));

            bClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { //Clear input and input string field
                    mCalcField.setText("");
                }
            });

            //Parse input, check for errors, perform operation, then update history
            bEquals.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int status = calc.checkOp(mCalcField.getText().toString());

                    if (status == DIVIDE_ZERO)
                        Toast.makeText(getActivity().getApplicationContext(),R.string.divideError,Toast.LENGTH_SHORT).show();
                    else if(status == BAD_INPUT)
                        Toast.makeText(getActivity().getApplicationContext(),R.string.badInput,Toast.LENGTH_SHORT).show();
                    else
                        updateHistory(String.format("%s=%s", mCalcField.getText().toString(), Double.toString(calc.doCalc() )));
                    mCalcField.setText(Double.toString(calc.doCalc() )); //if bad input, this happens to restore previous result
                }
            });

            //Deletes the last character in the EditText field
            bDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String temp = mCalcField.getText().toString();
                    mCalcField.setText(temp.substring(0,temp.length()-1));
                }
            });

            return v;
        }

        //Creates a listener that just adds the number to the EditText field
        private View.OnClickListener createListener(final char value) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCalcField.setText(mCalcField.getText().toString() + Character.toString(value));
                }
            };
        }

        //Adds the operator symbol to the EditText. Operation will be parsed when equals is pressed
        private View.OnClickListener createOpListener(final String op) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCalcField.setText(mCalcField.getText().toString() + op);
                }
            };
        }

        private View.OnClickListener createTrigListener(final String op) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isNumeric(mCalcField.getText().toString())) {
                        double result = calc.doTrigOp(op, mCalcField.getText().toString());
                        if(result == TRIG_ERROR)
                            Toast.makeText(getActivity().getApplicationContext(),R.string.trigError,Toast.LENGTH_SHORT).show();
                        else
                            updateHistory(String.format("%s(%s)=%s", op, mCalcField.getText().toString(),Double.toString(result) ));
                    }
                }
            };
        }

        //Use regex to determine if a string is a decimal number
        private boolean isNumeric(String str) {
            return str.matches("-?\\d+(\\.\\d+)?");
        }

        //For any button that wants to print to the history, use this function. There are two strings to keep track
        //of. The result string will always go to the first string, but first if the first string already exists, move it to the second.
        private void updateHistory(String result) {
            if(histTop != null)
                histBottom = histTop;
            histTop = result;

            if(histBottom != null)
                mHistory.setText(histTop + "\n" + histBottom);
            else
                mHistory.setText(histTop);
        }
    }
}
