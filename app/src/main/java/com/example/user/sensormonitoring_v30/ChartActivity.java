package com.example.user.sensormonitoring_v30;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import static java.lang.Float.parseFloat;


public class ChartActivity extends AppCompatActivity implements View.OnClickListener,
        OnChartGestureListener,
        OnChartValueSelectedListener{

    Button back;
    Boolean activo = true;

    // IP de mi Url
    String IP = "http://tfghost.hopto.org";
    // Ruta al Web Service
    String GET_TEMP = IP + "/get_temp.php";
    String GET_BAT = IP + "/get_bat.php";
    String GET_EXT1 = IP + "/get_ext1.php";
    String GET_EXT2 = IP + "/get_ext2.php";
    String DEACTIVATE = IP + "/deactivate.php";


    GetMeasures hiloconexion;
    String valor = null;
    String type = null;

    DeactivateNode hilothread;

    /*---------------------------------------------------------------------------------------------
            CHART
    ---------------------------------------------------------------------------------------------*/

    private LineChart mChart;

    /*---------------------------------------------------------------------------------------------
            CHART END
    ---------------------------------------------------------------------------------------------*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        String value = getIntent().getStringExtra("spinner_value"); //if it's a string you stored.
        valor = value;
        String measure_type = getIntent().getStringExtra("measure_type");
        type = measure_type;

        // Enlaces con elementos visuales del XML
        back = (Button)findViewById(R.id.back);

        // Listener de los botones
        back.setOnClickListener(this);

        /*---------------------------------------------------------------------------------------------
                 CHART
        ---------------------------------------------------------------------------------------------*/

        mChart = (LineChart) findViewById(R.id.linechart);

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        //l.setPosition(Legend.LegendPosition.LEFT_OF_CHART);
        l.setForm(Legend.LegendForm.LINE);

        mChart.setOnChartGestureListener(this);
        mChart.setOnChartValueSelectedListener(this);

        // no description text
        mChart.setDescription("Sensor Monitoring v3.0");
        mChart.setNoDataTextDescription("No data found.");
        mChart.clear();

        // enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();

        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(false);

        leftAxis.setDrawLimitLinesBehindData(true);

        mChart.getAxisRight().setEnabled(false);

        mChart.animateX(1500, Easing.EasingOption.EaseInOutQuart); //2500
        mChart.animateY(1500, Easing.EasingOption.EaseInOutQuart);

        /*---------------------------------------------------------------------------------------------
                CHART END
        ---------------------------------------------------------------------------------------------*/

        hiloconexion = new GetMeasures();

        if (type.equals("1")){
            String cadenallamada = GET_TEMP + "?node_id=" + valor;
            hiloconexion.execute(cadenallamada,"1");
        }
        else if (type.equals("2")){
            String cadenallamada = GET_BAT + "?node_id=" + valor;
            hiloconexion.execute(cadenallamada,"2");
        }
        else if (type.equals("3")){
            String cadenallamada = GET_EXT1 + "?node_id=" + valor;
            hiloconexion.execute(cadenallamada,"3");
        }
        else if (type.equals("4")){
            String cadenallamada = GET_EXT2 + "?node_id=" + valor;
            hiloconexion.execute(cadenallamada,"4");
        }

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
    }

    @Override
    public void onClick(View v) {

        hilothread = new DeactivateNode();
        String link = DEACTIVATE;
        String active = "N";
        hilothread.execute(link,valor,active);

        activo = false;
        Intent myIntent = new Intent(ChartActivity.this, MainActivity.class);
        ChartActivity.this.startActivity(myIntent);
    }

    /*---------------------------------------------------------------------------------------------
            CHART
    ---------------------------------------------------------------------------------------------*/

    @Override
    public void onChartGestureStart(MotionEvent me,
                                    ChartTouchListener.ChartGesture
                                            lastPerformedGesture) {

        Log.i("Gesture", "START, x: " + me.getX() + ", y: " + me.getY());
    }

    @Override
    public void onChartGestureEnd(MotionEvent me,
                                  ChartTouchListener.ChartGesture
                                          lastPerformedGesture) {

        Log.i("Gesture", "END, lastGesture: " + lastPerformedGesture);

        // un-highlight values after the gesture is finished and no single-tap
        if(lastPerformedGesture != ChartTouchListener.ChartGesture.SINGLE_TAP)
            // or highlightTouch(null) for callback to onNothingSelected(...)
            mChart.highlightValues(null);
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {
        Log.i("LongPress", "Chart longpressed.");
    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {
        Log.i("DoubleTap", "Chart double-tapped.");
    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {
        Log.i("SingleTap", "Chart single-tapped.");
    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2,
                             float velocityX, float velocityY) {
        Log.i("Fling", "Chart flinged. VeloX: "
                + velocityX + ", VeloY: " + velocityY);
    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
        Log.i("Scale / Zoom", "ScaleX: " + scaleX + ", ScaleY: " + scaleY);
    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {
        Log.i("Translate / Move", "dX: " + dX + ", dY: " + dY);
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        Log.i("Entry selected", e.toString());
        Log.i("LOWHIGH", "low: " + mChart.getLowestVisibleXIndex()
                + ", high: " + mChart.getHighestVisibleXIndex());

        Log.i("MIN MAX", "xmin: " + mChart.getXChartMin()
                + ", xmax: " + mChart.getXChartMax()
                + ", ymin: " + mChart.getYChartMin()
                + ", ymax: " + mChart.getYChartMax());
    }

    @Override
    public void onNothingSelected() {
        Log.i("Nothing selected", "Nothing selected.");
    }

    /*---------------------------------------------------------------------------------------------
            CHART END
    ---------------------------------------------------------------------------------------------*/

    public class GetMeasures extends AsyncTask<String,Void,String> {

        String cadenallamada = null; //GET + "?node_id=" + valor; //para llamar al asynctask de refresh

        /*---------------------------------------------------------------------------------------------
                CHART
        ---------------------------------------------------------------------------------------------*/

        //arraylists para el chart
        ArrayList<String> miXArray = new ArrayList<>();
        ArrayList<Entry> miYArray = new ArrayList<>();

        /*---------------------------------------------------------------------------------------------
                 CHART END
        ---------------------------------------------------------------------------------------------*/


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {

            String cadena = params[0];
            URL url = null;
            String retorno = "";

            if (params[1].equals("1")) { //get temp

                try {
                    url = new URL(cadena);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection(); //Open connection

                    int respuesta = connection.getResponseCode();
                    StringBuilder result = new StringBuilder();

                    if (respuesta == HttpURLConnection.HTTP_OK) {

                        InputStream in = new BufferedInputStream(connection.getInputStream());  // preparo la cadena de entrada

                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));  // la introduzco en un BufferedReader

                        // El siguiente proceso lo hago porque el JSONOBject necesita un String y tengo
                        // que tranformar el BufferedReader a String. Esto lo hago a traves de un
                        // StringBuilder.

                        String line;
                        while ((line = reader.readLine()) != null) {
                            result.append(line);        // Paso toda la entrada al StringBuilder
                        }

                        //Creamos un objeto JSONObject para poder acceder a los atributos (campos) del objeto.
                        JSONObject respuestaJSON = new JSONObject(result.toString());   //Creo un JSONObject a partir del StringBuilder pasado a cadena

                        //Accedemos al vector de resultados
                        String resultJSON = respuestaJSON.getString("estado");   // estado es el nombre del campo(tag) en el JSON

                        if (resultJSON.equals("1")) {      // hay datos a mostrar
                            JSONArray medidasJSON = respuestaJSON.getJSONArray("medida");   // nodes es el nombre del campo en el JSON
                            for (int i = 0; i < medidasJSON.length(); i++) {
                                /*---------------------------------------------------------------------------------------------
                                        CHART
                                ---------------------------------------------------------------------------------------------*/

                                miXArray.add(medidasJSON.getJSONObject(i).getString("time"));
                                miYArray.add(new Entry(parseFloat(medidasJSON.getJSONObject(i).getString("temperature")), i));

                                /*---------------------------------------------------------------------------------------------
                                        CHART END
                                ---------------------------------------------------------------------------------------------*/

                                retorno = "temp";

                            }
                        } else if (resultJSON.equals("2")) {
                            miXArray.add("No Data");
                        }

                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return retorno;
            }
            else if (params[1].equals("2")){ //get battery
                try {
                    url = new URL(cadena);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection(); //Open connection

                    int respuesta = connection.getResponseCode();
                    StringBuilder result = new StringBuilder();

                    if (respuesta == HttpURLConnection.HTTP_OK) {

                        InputStream in = new BufferedInputStream(connection.getInputStream());  // preparo la cadena de entrada

                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));  // la introduzco en un BufferedReader

                        // El siguiente proceso lo hago porque el JSONOBject necesita un String y tengo
                        // que tranformar el BufferedReader a String. Esto lo hago a traves de un
                        // StringBuilder.

                        String line;
                        while ((line = reader.readLine()) != null) {
                            result.append(line);        // Paso toda la entrada al StringBuilder
                        }

                        //Creamos un objeto JSONObject para poder acceder a los atributos (campos) del objeto.
                        JSONObject respuestaJSON = new JSONObject(result.toString());   //Creo un JSONObject a partir del StringBuilder pasado a cadena

                        //Accedemos al vector de resultados
                        String resultJSON = respuestaJSON.getString("estado");   // estado es el nombre del campo(tag) en el JSON

                        if (resultJSON.equals("1")) {      // hay datos a mostrar
                            JSONArray medidasJSON = respuestaJSON.getJSONArray("medida");   // nodes es el nombre del campo en el JSON
                            for (int i = 0; i < medidasJSON.length(); i++) {
                                /*---------------------------------------------------------------------------------------------
                                        CHART
                                ---------------------------------------------------------------------------------------------*/

                                miXArray.add(medidasJSON.getJSONObject(i).getString("time"));
                                miYArray.add(new Entry(parseFloat(medidasJSON.getJSONObject(i).getString("battery")), i));

                                /*---------------------------------------------------------------------------------------------
                                        CHART END
                                ---------------------------------------------------------------------------------------------*/

                                retorno = "bat";

                            }
                        } else if (resultJSON.equals("2")) {
                            miXArray.add("No Data");
                        }

                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return retorno;
            }

            else if (params[1].equals("3")){ //get external sensor 1
                try {
                    url = new URL(cadena);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection(); //Open connection

                    int respuesta = connection.getResponseCode();
                    StringBuilder result = new StringBuilder();

                    if (respuesta == HttpURLConnection.HTTP_OK) {

                        InputStream in = new BufferedInputStream(connection.getInputStream());  // preparo la cadena de entrada

                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));  // la introduzco en un BufferedReader

                        // El siguiente proceso lo hago porque el JSONOBject necesita un String y tengo
                        // que tranformar el BufferedReader a String. Esto lo hago a traves de un
                        // StringBuilder.

                        String line;
                        while ((line = reader.readLine()) != null) {
                            result.append(line);        // Paso toda la entrada al StringBuilder
                        }

                        //Creamos un objeto JSONObject para poder acceder a los atributos (campos) del objeto.
                        JSONObject respuestaJSON = new JSONObject(result.toString());   //Creo un JSONObject a partir del StringBuilder pasado a cadena

                        //Accedemos al vector de resultados
                        String resultJSON = respuestaJSON.getString("estado");   // estado es el nombre del campo(tag) en el JSON

                        if (resultJSON.equals("1")) {      // hay datos a mostrar
                            JSONArray medidasJSON = respuestaJSON.getJSONArray("medida");   // nodes es el nombre del campo en el JSON
                            for (int i = 0; i < medidasJSON.length(); i++) {
                                /*---------------------------------------------------------------------------------------------
                                        CHART
                                ---------------------------------------------------------------------------------------------*/

                                miXArray.add(medidasJSON.getJSONObject(i).getString("time"));
                                miYArray.add(new Entry(parseFloat(medidasJSON.getJSONObject(i).getString("external1")), i));

                                /*---------------------------------------------------------------------------------------------
                                        CHART END
                                ---------------------------------------------------------------------------------------------*/

                                retorno = "ext1";

                            }
                        } else if (resultJSON.equals("2")) {
                            miXArray.add("No Data");
                        }

                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return retorno;
            }

            else if (params[1].equals("4")){ //get external sensor 2
                try {
                    url = new URL(cadena);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection(); //Open connection

                    int respuesta = connection.getResponseCode();
                    StringBuilder result = new StringBuilder();

                    if (respuesta == HttpURLConnection.HTTP_OK) {

                        InputStream in = new BufferedInputStream(connection.getInputStream());  // preparo la cadena de entrada

                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));  // la introduzco en un BufferedReader

                        // El siguiente proceso lo hago porque el JSONOBject necesita un String y tengo
                        // que tranformar el BufferedReader a String. Esto lo hago a traves de un
                        // StringBuilder.

                        String line;
                        while ((line = reader.readLine()) != null) {
                            result.append(line);        // Paso toda la entrada al StringBuilder
                        }

                        //Creamos un objeto JSONObject para poder acceder a los atributos (campos) del objeto.
                        JSONObject respuestaJSON = new JSONObject(result.toString());   //Creo un JSONObject a partir del StringBuilder pasado a cadena

                        //Accedemos al vector de resultados
                        String resultJSON = respuestaJSON.getString("estado");   // estado es el nombre del campo(tag) en el JSON

                        if (resultJSON.equals("1")) {      // hay datos a mostrar
                            JSONArray medidasJSON = respuestaJSON.getJSONArray("medida");   // nodes es el nombre del campo en el JSON
                            for (int i = 0; i < medidasJSON.length(); i++) {
                                /*---------------------------------------------------------------------------------------------
                                        CHART
                                ---------------------------------------------------------------------------------------------*/

                                miXArray.add(medidasJSON.getJSONObject(i).getString("time"));
                                miYArray.add(new Entry(parseFloat(medidasJSON.getJSONObject(i).getString("external2")), i));

                                /*---------------------------------------------------------------------------------------------
                                        CHART END
                                ---------------------------------------------------------------------------------------------*/

                                retorno = "ext2";

                            }
                        } else if (resultJSON.equals("2")) {
                            miXArray.add("No Data");
                        }

                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return retorno;
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            /*---------------------------------------------------------------------------------------------
                    CHART
            ---------------------------------------------------------------------------------------------*/

            // add data methods
            setData();
            //setRandomData();

            /*---------------------------------------------------------------------------------------------
                    CHART END
            ---------------------------------------------------------------------------------------------*/

            if (activo){
                if (s.equals("temp")){
                    cadenallamada = GET_TEMP + "?node_id=" + valor;
                    new GetMeasures().execute(cadenallamada,"1");
                }
                else if (s.equals("bat")){
                    cadenallamada = GET_BAT + "?node_id=" + valor;
                    new GetMeasures().execute(cadenallamada,"2");
                }
                else if (s.equals("ext1")){
                    cadenallamada = GET_EXT1 + "?node_id=" + valor;
                    new GetMeasures().execute(cadenallamada,"3");
                }
                else if (s.equals("ext2")){
                    cadenallamada = GET_EXT2 + "?node_id=" + valor;
                    new GetMeasures().execute(cadenallamada,"4");
                }

            }

        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
        }

        /*---------------------------------------------------------------------------------------------
                CHART
        ---------------------------------------------------------------------------------------------*/

        private void setData() {

            ArrayList<String> xVals = miXArray;
            ArrayList<Entry> yVals = miYArray;

            LineDataSet set1;

            // create a dataset and give it a type
            //set1 = new LineDataSet(yVals, "DataSet 1");
            set1 = new LineDataSet(yVals, ""); //hacer dinamico segun measure type
            set1.setFillAlpha(110);
            // set1.setFillColor(Color.RED);

            // set the line to be drawn like this "- - - - - -"
            // set1.enableDashedLine(10f, 5f, 0f);
            // set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLUE);
            set1.setCircleColor(Color.BLACK);
            set1.setLineWidth(1f);
            set1.setCircleRadius(3f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(0f);
            set1.setDrawFilled(false);

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1); // add the datasets

            // create a data object with the datasets
            LineData data = new LineData(xVals, dataSets);

            // set data
            mChart.setData(data);
            mChart.setVisibleXRangeMaximum(40);
            mChart.moveViewToX(data.getXValCount());

            mChart.forceLayout();

        }


        private void setRandomData() { //https://www.youtube.com/watch?v=a20EchSQgpw

            LineData data = new LineData();
            mChart.setData(data);

        }

        /*---------------------------------------------------------------------------------------------
                CHART END
        ---------------------------------------------------------------------------------------------*/


    }

    public class DeactivateNode extends AsyncTask<String,Void,String>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {

            String chain = params[0];
            URL url = null;
            String devuelve ="";

            try {
                HttpURLConnection urlConn;

                DataOutputStream printout;
                DataInputStream input;
                url = new URL(chain);
                urlConn = (HttpURLConnection) url.openConnection();
                urlConn.setDoInput(true);
                urlConn.setDoOutput(true);
                urlConn.setUseCaches(false);
                urlConn.setRequestProperty("Content-Type", "application/json");
                urlConn.setRequestProperty("Accept", "application/json");
                urlConn.connect();
                //Creo el Objeto JSON
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("node_id",params[1]);
                jsonParam.put("is_active", params[2]);
                // Envio los parámetros post.
                OutputStream os = urlConn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(jsonParam.toString());
                writer.flush();
                writer.close();

                int respuesta = urlConn.getResponseCode();


                StringBuilder result = new StringBuilder();

                if (respuesta == HttpURLConnection.HTTP_OK) {

                    String line;
                    BufferedReader br=new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                    while ((line=br.readLine()) != null) {
                        result.append(line);
                        //response+=line;
                    }

                    //Creamos un objeto JSONObject para poder acceder a los atributos (campos) del objeto.
                    JSONObject respuestaJSON = new JSONObject(result.toString());   //Creo un JSONObject a partir del StringBuilder pasado a cadena
                    //Accedemos al vector de resultados

                    String resultJSON = respuestaJSON.getString("estado");   // estado es el nombre del campo en el JSON

                    if (resultJSON == "1") {      // hay un alumno que mostrar
                        devuelve = "Nodo activado correctamente";

                    } else if (resultJSON == "2") {
                        devuelve = "Nodo no activado";
                    }

                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return devuelve;
            //return null;

        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }

    }

}
