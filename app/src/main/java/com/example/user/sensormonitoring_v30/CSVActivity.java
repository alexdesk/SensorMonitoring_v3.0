package com.example.user.sensormonitoring_v30;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import static android.R.layout.simple_spinner_item;

public class CSVActivity extends AppCompatActivity implements View.OnClickListener{

    Button download;
    Spinner spinnercsv;
    DatePicker datepicker;

    // IP (raspberry pi host)
    String IP = "http://tfghost.hopto.org/";
    // Web Services
    String GET_NODES = IP + "get_nodes.php";
    String EXTENSION = ".csv";

    GetNodesCSV connection;
    DownloadPastMeasures pastconnection;

    String Globalfilename;

    //variables globales para mensaje de error
    String node;
    String dia;
    String mes;
    String ano;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_csv);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        //Link to visual element on XML
        download = (Button) findViewById(R.id.download);
        spinnercsv = (Spinner)findViewById(R.id.spinnercsv);
        datepicker = (DatePicker) findViewById(R.id.datepicker);

        //Button listener
        download.setOnClickListener((View.OnClickListener) this);

        //execute getNodesCSV --> set data on the list (spinner)
        connection = new GetNodesCSV();
        connection.execute(GET_NODES); //param for doInBackground (URL)

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.download:

                int day = datepicker.getDayOfMonth();
                int month = datepicker.getMonth() + 1;
                int year = datepicker.getYear();

                String sday = String.valueOf(day);
                String smonth = String.valueOf(month);
                String syear = String.valueOf(year);

                if(month < 10){
                    smonth = "0" + smonth;
                }
                if(day < 10){
                    sday  = "0" + sday ;
                }

                String link = IP + spinnercsv.getSelectedItem().toString() + "_" + sday + "-" + smonth + "-" + syear + EXTENSION;
                String filename = spinnercsv.getSelectedItem().toString() + "_" + sday + "-" + smonth + "-" + syear + EXTENSION;

                Globalfilename = filename;
                node = spinnercsv.getSelectedItem().toString();
                dia = sday;
                mes = smonth;
                ano = syear;

                //Toast toast = Toast.makeText(this,link,Toast.LENGTH_LONG);
                //toast.show();

                pastconnection = new DownloadPastMeasures();
                pastconnection.execute(link,filename);

                break;

            default:

                break;

        }
    }

    class GetNodesCSV extends AsyncTask<String,Void,String> {

        //Array para guardar los nodos
        ArrayList<String> nodesArray = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {

            String cadena = params[0];
            URL url = null;
            String nodes = "";

            try {
                url = new URL(cadena);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection(); //Open connection

                int respuesta = connection.getResponseCode();
                StringBuilder result = new StringBuilder();

                if (respuesta == HttpURLConnection.HTTP_OK){

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

                    if (resultJSON.equals("1")){      // hay datos a mostrar
                        JSONArray medidasJSON = respuestaJSON.getJSONArray("nodes");   // nodes es el nombre del campo en el JSON
                        for(int i=0;i<medidasJSON.length();i++){
                            nodesArray.add(medidasJSON.getJSONObject(i).getString("node_id"));
                        }
                    }

                    else if (resultJSON.equals("2")){
                        nodesArray.add("No hay nodos disponibles en la red");
                    }

                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
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

            // Application of the Array to the Spinner
            ArrayAdapter adapter = new ArrayAdapter<>(getApplicationContext(), simple_spinner_item, nodesArray);
            spinnercsv.setAdapter(adapter);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view

        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
        }

    }

    class DownloadPastMeasures extends AsyncTask<String, Integer, String> {

        ProgressDialog progressDialog;

        /**
         * Set up a ProgressDialog
         */
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(CSVActivity.this);
            progressDialog.setTitle("Download in progress...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(100);
            progressDialog.setProgress(0);
            progressDialog.show();

        }

        /**
         *  Background task
         */
        @Override
        protected String doInBackground(String... params) {

            HttpURLConnection connect = null;

            if ("1".equals("1")) {
                String path = params[0];
                String filename = params[1];
                int file_length;

                try {
                    URL url = new URL(path);
                    connect = (HttpURLConnection) url.openConnection();
                    connect.connect();
                    //URLConnection urlConnection = url.openConnection();
                    //urlConnection.connect();

                    // expect HTTP 200 OK, so we don't mistakenly save error report
                    // instead of the file
                    if (connect.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        //return "Server returned HTTP " + connect.getResponseCode()
                                //+ " " + connect.getResponseMessage();
                        return "No data found for node: " + node + " on " + dia + "-" + mes + "-" + ano;
                    }

                    file_length = connect.getContentLength();

                    /**
                     * Create a folder
                     */
                    File new_folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "Sensor Monitoring Past Measures");
                    if (!new_folder.exists()) {
                        if (new_folder.mkdir()) {
                            Log.i("Info", "Folder succesfully created");
                        } else {
                            Log.i("Info", "Failed to create folder");
                        }
                    } else {
                        Log.i("Info", "Folder already exists");
                    }

                    /**
                     * Create an output file to store the image for download
                     */
                    File output_file = new File(new_folder, filename);
                    OutputStream outputStream = new FileOutputStream(output_file);

                    InputStream inputStream = new BufferedInputStream(url.openStream(), 8192);
                    byte[] data = new byte[1024];
                    int total = 0;
                    int count;
                    while ((count = inputStream.read(data)) != -1) {
                        total += count;

                        outputStream.write(data, 0, count);
                        int progress = 100 * total / file_length;
                        publishProgress(progress);

                        Log.i("Info", "Progress: " + Integer.toString(progress));
                    }
                    inputStream.close();
                    outputStream.close();

                    Log.i("Info", "file_length: " + Integer.toString(file_length));

                /*} catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
                } catch (Exception e) {
                    return e.toString();
                }
                    if (connect != null)
                        connect.disconnect();
                }

            return "Download complete";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.hide();
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
            File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "Sensor Monitoring Past Measures");
            File output_file = new File(folder, Globalfilename);
            String path = output_file.toString();
            //imageView.setImageDrawable(Drawable.createFromPath(path));
            Log.i("Info", "Path: " + path);
        }
    }

}
