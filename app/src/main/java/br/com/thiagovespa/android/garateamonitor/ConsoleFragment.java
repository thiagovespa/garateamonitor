package br.com.thiagovespa.android.garateamonitor;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ConsoleFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ConsoleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConsoleFragment extends Fragment {

    public static final int BUFFER_SIZE = 10000; //in chars
    private TextView textView;

    public ConsoleFragment() {
        // Required empty public constructor
    }

    public static ConsoleFragment newInstance() {
        ConsoleFragment fragment = new ConsoleFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_console, container, false);
        textView = (TextView) v.findViewById(R.id.textView);
        textView.setText("Aguardando...\n");
        return v;
    }

    public void updateConsole(CharSequence txt) {
        tvAppend(textView, txt);
    }
    public void clear() {
        textView.setText("");
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        breakLines(ftext);

    }



    private void breakLines(CharSequence str){
        textView.append(str);
        CharSequence txt = textView.getText();
        //TODO: Melhorar esse código porco. GC
        if(txt.length()> BUFFER_SIZE) { // Buffer to avoid memory leak
            textView.setText(txt.subSequence(txt.length()-BUFFER_SIZE, txt.length()));
        }

    }



}
