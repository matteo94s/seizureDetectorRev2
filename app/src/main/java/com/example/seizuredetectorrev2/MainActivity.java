package com.example.seizuredetectorrev2;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {


    private LineGraphSeries<DataPoint> series;
    private LineGraphSeries<DataPoint> series2;
    private LineGraphSeries<DataPoint> hrSeries;
    private int fs = 200;
    private int lastX = 0;                          //counter
    private int lastY = 0;                          //counter

    public String timeLine;
    public String HrLine;
    private ECG ecg;
    private TextView peakCounter;
    private TextView mHRnumber;
    private int countPeaks = 0;
    private int maxDataPoints = 4000;
    private double[] rawSignal = new double[maxDataPoints];
    public double[] y_axis = new double[maxDataPoints];            //Y axis points
    private double[] HR = new double[maxDataPoints];
    private double[] time = new double[maxDataPoints];
    public String line;                             //stores the value for the string
    public double[] qrsSignal;                      // array of QRS.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //*******************LOAD graphics components
        TextView peakCounter = (TextView)findViewById(R.id.peakCounterNumber);
        this.peakCounter = peakCounter;
        mHRnumber = (TextView)findViewById(R.id.hrNumber);
//        peakCounter.setText(0);

        //******************************************************
        //              GET POINTS FROM ARRAY FOR GRAPH
        //******************************************************
        doImport();                                         //import file from string
        System.out.println("The text string is: " + line);
        String[] numberStrs = line.split(",");
        double[] numbers = new double[numberStrs.length];
        int size = numberStrs.length;

        // import time
        doImportTime();
        String[] timeString = timeLine.split(",");
        for (int j=0; j<time.length; j++){
            time[j] = Double.parseDouble(timeString[j]);
        }

        // import HR
        doImportHR();
        String[] hrString = HrLine.split(",");
        for (int j=0; j<HR.length; j++){
            HR[j] = Double.parseDouble(hrString[j]);
        }





        if(size > maxDataPoints)                                    //load a maximum of 1000 numbers
        {
            size = maxDataPoints;
        }

        //set min/max xy for axis and load array with values
        double highestNum = 0;                 //highest Y axis
        double lowestNum = maxDataPoints-1;               //lowest Y axis

        for(int i = 0;i < y_axis.length;i++)
        {
            y_axis[i] = Double.parseDouble(numberStrs[i]);
            if(y_axis[i] > highestNum)
            {
                highestNum = y_axis[i];
            }

            if(y_axis[i] < lowestNum)
            {
                lowestNum = y_axis[i];
            }
        }
        //******************************************************
        int fs = 200;
        int window = maxDataPoints;
        //double [] rawSignal = new double[window];
        rawSignal = y_axis;
        this.ecg = new ECG(fs,window,rawSignal);
//        System.out.println("The signal is: " + signal[1]);
        double [] qrsSignal = new double[rawSignal.length];
        ecg.getQRS();
        ecg.detectQRS();
//        int peakCounter = ecg.retunPeakCounter();
        //this.time = ecg.getTime();
        this.qrsSignal = ecg.getSignal();


        //******************************************************
        //              GRAPH STUFF
        //******************************************************
        //create a graph for the HR
        GraphView mHrGraph = (GraphView) findViewById(R.id.hrGraph);
        hrSeries = new LineGraphSeries<DataPoint>();
        hrSeries.setColor(Color.BLUE);
        mHrGraph.addSeries(hrSeries);
        mHrGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time [s]");
        mHrGraph.getGridLabelRenderer().setVerticalAxisTitle("HR [Bpm]");
        //viewport for HR
        Viewport hrVewport = mHrGraph.getViewport();
        //viewport.setScrollable(true);
        hrVewport.setYAxisBoundsManual(true);
        hrVewport.setMinY(40);
        hrVewport.setMaxY(180);

        hrVewport.setXAxisBoundsManual(true);
        hrVewport.setMinX(0);
        hrVewport.setMaxX(5);
//        viewport.setMaxX(400);
//        viewport.setMaxX(1000);
        hrVewport.setScrollable(true);









        //Create we get graph view instance
        GraphView graph = (GraphView) findViewById(R.id.graph1);
        //Data
        series = new LineGraphSeries<DataPoint>();  //rr peaks
        series2 = new LineGraphSeries<DataPoint>(); //raw ecg
        series.setTitle("R-Peaks");
        series.setColor(Color.BLACK);
        series2.setTitle("ECG");
        series2.setColor(Color.RED);

        //****Optional
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        //****
        series.setThickness(8);
        series2.setThickness(2);




        graph.addSeries(series);
        graph.addSeries(series2);



        graph.getGridLabelRenderer().setHorizontalAxisTitle("Time [s]");
        graph.getGridLabelRenderer().setVerticalAxisTitle("Amplitude. [mV]");


//        graph.onDataChanged(true, true);

        //customize viewport
        Viewport viewport = graph.getViewport();
        //viewport.setScrollable(true);
        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(lowestNum);
        viewport.setMaxY(highestNum);

        viewport.setXAxisBoundsManual(true);
        viewport.setMinX(0);
        viewport.setMaxX(5);
//        viewport.setMaxX(400);
//        viewport.setMaxX(1000);
        viewport.setScrollable(true);
    }

    //**********************************************************
    // Simulate real time with thread that append data to the graph
    //**********************************************************

    @Override
    protected void onResume() {
        super.onResume();

        new Thread(new Runnable() {
            @Override
            public void run() {

                // we add 1000 new entries
                for (int i = 0; i < maxDataPoints; i++) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            addEntry();

                        }
                    });
                    // sleep to slow down the add of entries
                    try {
//                        Thread.sleep(10);
                        Thread.sleep(5); // 5 ms are exactly 1/200 seconds.... real time....

                    } catch (InterruptedException e) {
                        // manage error ...
                    }
                }
            }
        }).start();
    }

    // add data to graph
    private void addEntry() {
//        if(lastX < 1000)
//        {
//
//        lastX += 1/fs;
        lastX++;
        lastY++;
//        series2.appendData(new DataPoint(lastX, rawSignal[lastY]), true, maxDataPoints);
//        series.appendData(new DataPoint(lastX, qrsSignal[lastY]), true, maxDataPoints);
        series2.appendData(new DataPoint(time[lastX], rawSignal[lastY]), true, maxDataPoints);
        series.appendData(new DataPoint(time[lastX], qrsSignal[lastY]), true, maxDataPoints);
        // for the HR
        hrSeries.appendData(new DataPoint(time[lastX], HR[lastY]), true, maxDataPoints);



        if (qrsSignal[lastY] > 0){ //if not -1 --> array bound exception
            this.countPeaks ++;
            peakCounter.setText(Integer.toString(this.countPeaks));
            //TODO add this 2 lines i works
//           double hr = ecg.getHRnumber(lastY);
//            mHRnumber.setText(Double.toString(hr));
        }

        // HR real time update number
        double tmp = HR[lastY];
        double tmp2 = Math.round(tmp);
        mHRnumber.setText(Double.toString(tmp2));
        //TODO create the time vector



//        series2.appendData(new DataPoint(lastX ++, rawSignal[lastY++]), true, maxDataPoints);
//        series.appendData(new DataPoint(lastX, qrsSignal[lastY]), true, maxDataPoints);
//        if (qrsSignal[lastY-1] > 0){ //if not -1 --> array bound exception
//            this.countPeaks ++;
//            peakCounter.setText(Integer.toString(this.countPeaks));
//
//        double hr = ecg.getHRnumber(lastY);
//        mHRnumber.setText(Double.toString(hr));
//        }

//        series.appendData(new DataPoint(lastX++, RANDOM.nextDouble() * 10d), true, 10);
//        }
    }

    //import text file
    private int doImport() {
        InputStream is = getResources().openRawResource(R.raw.sz01_20sec_filter);
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StringBuilder total = new StringBuilder();
        try {
            while ((line = r.readLine()) != null)
                total.append(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
        line = total.toString();
        return 1;
    }

    //import time
    private int doImportTime() {
        InputStream isTime = getResources().openRawResource(R.raw.time_20sec);
        BufferedReader r = new BufferedReader(new InputStreamReader(isTime));
        StringBuilder total = new StringBuilder();
        try {
            while ((line = r.readLine()) != null)
                total.append(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
        timeLine = total.toString();
        return 1;
    }

    //import time
    private int doImportHR() {
        InputStream is = getResources().openRawResource(R.raw.hr_sz01_20sec);
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StringBuilder total = new StringBuilder();
        try {
            while ((line = r.readLine()) != null)
                total.append(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HrLine = total.toString();
        return 1;
    }


}
