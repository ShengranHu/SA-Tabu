package TCYB;

import java.io.*;
import java.util.*;

import smile.math.rbf.*;
import smile.interpolation.RBFInterpolation;

import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

public class Test {
    public static void main(String[] args) throws FileNotFoundException {
        long seed = System.currentTimeMillis();
        Random random = new Random(seed);
        String filename = "2520-1";
        int m = 18;

        SLAP slap = new SLAP(filename, m);
        Search search = new Search(3, random);

        double[][] upper = null;
        File file_upper = new File("sample_upperVar-"+filename+".dat");
        FileInputStream in;
        try {
            in = new FileInputStream(file_upper);
            ObjectInputStream objIn=new ObjectInputStream(in);
            upper = (double[][]) objIn.readObject();
            objIn.close();
            System.out.println("read object success!");
        } catch (IOException e) {
            System.out.println("read object failed");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        double[] objs = null;
        File file_obj = new File("sample_obj-"+filename+".dat");
        try {
            in = new FileInputStream(file_obj);
            ObjectInputStream objIn = new ObjectInputStream(in);
            objs = (double[]) objIn.readObject();
            objIn.close();
            System.out.println("read object success!");
        } catch (IOException e) {
            System.out.println("read object failed");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        PrintStream print = new PrintStream("rbfcorr");  //写好输出位置文件；
        System.setOut(print);
        for (int k = 0; k < 10; k++) {
            for (int j = 0; j < 15; j++) {
                int sample = (j+2) * 50;

                List<Integer> idx = new ArrayList<>();
                for (int i = 0; i < objs.length; i++) {
                    idx.add(i);
                }
                Collections.shuffle(idx);

                double[][] temp_Var = new double[objs.length][];
                double[] temp_obj = new double[objs.length];

                for (int i = 0; i < objs.length; i++) {

                    temp_Var[i] = upper[idx.get(i)];
                    temp_obj[i] = objs[idx.get(i)];
                }

                int totalLength = temp_obj.length;
                int trainingLength = sample;
                int testLength = totalLength - trainingLength;

                double[][] train_var = new double[trainingLength][slap.product.size()];
                double[] train_tar = new double[trainingLength];

                double [][] test_var = new double[testLength][slap.product.size()];
                double[] test_tar = new double[testLength];
                double[] pred_tar = new double[testLength];



                System.arraycopy(temp_Var, 0, train_var, 0, trainingLength);
                System.arraycopy(temp_obj, 0, train_tar, 0, trainingLength);


                System.arraycopy(temp_Var, trainingLength, test_var, 0, testLength);
                System.arraycopy(temp_obj, trainingLength, test_tar, 0, testLength);

                MinMaxScaler targetScaler = new MinMaxScaler(train_tar, 1, 0);
                train_tar = targetScaler.transform(train_tar);

                long startTime = System.currentTimeMillis();
                RBFInterpolation rbf = new RBFInterpolation(train_var, train_tar, new InverseMultiquadricRadialBasis());
//        System.out.println("Build RBF cost: " + Search.getElapsedTime(startTime)/1000);


                startTime = System.currentTimeMillis();
                for (int i = 0; i < testLength; i++) {
                    double pred = rbf.interpolate(test_var[i]);
                    pred_tar[i] = targetScaler.rev_transform(pred);
                }
//        System.out.println("Query RBF cost: " + Search.getElapsedTime(startTime)/1000);

                KendallsCorrelation kendallsCorrelation = new KendallsCorrelation();
                double tau = kendallsCorrelation.correlation(pred_tar, test_tar);
                SpearmansCorrelation spearmansCorrelation = new SpearmansCorrelation();
                double rho = spearmansCorrelation.correlation(pred_tar, test_tar);
                System.out.println("spearmans " + rho + " sampleNum "+ sample + " idx " + k);
//                System.out.println("RBF kendalls correlation: " + tau);
            }
        }

    }

//    public static void main(String[] args) {
//        //Set a fixed seed if needed
//        long seed = System.currentTimeMillis();
//        Random random = new Random(seed);
//
//
//        String filename = "5040-5";
//        int m = 36;
//
//        SLAP slap = new SLAP(filename, m);
//        Search search = new Search(3, random);
//
//        int sampleLength = 10000;
//        double[][] upperVar = new double[sampleLength][slap.product.size()];
//        double[] obj = new double[sampleLength];
//
//        long startTime = System.currentTimeMillis();
//        for (int i = 0; i < sampleLength; i++) {
//            Layout soln = search.init(slap);
//            search.LNS(soln, 256);
//
//            ArrayList<ArrayList<Layout.GROUP>> layoutCode = soln.encode(soln.row);
//            double[] var = getUpperVar(layoutCode, slap);
//            double objVal = soln.objective();
//            upperVar[i] = var;
//            obj[i] = objVal;
//        }
//        System.out.println("Average Local search cost: " + Search.getElapsedTime(startTime)/1000/sampleLength);
//        File file_upper =new File("sample_upperVar-"+filename+".dat");
//        File file_obj =new File("sample_obj-"+filename+".dat");
//        FileOutputStream out;
//        try {
//            out = new FileOutputStream(file_upper);
//            ObjectOutputStream objOut=new ObjectOutputStream(out);
//            objOut.writeObject(upperVar);
//            objOut.flush();
//            objOut.close();
//            System.out.println("write object success!");
//        } catch (IOException e) {
//            System.out.println("write object failed");
//            e.printStackTrace();
//        }
//        try {
//            out = new FileOutputStream(file_obj);
//            ObjectOutputStream objOut=new ObjectOutputStream(out);
//            objOut.writeObject(obj);
//            objOut.flush();
//            objOut.close();
//            System.out.println("write object success!");
//        } catch (IOException e) {
//            System.out.println("write object failed");
//            e.printStackTrace();
//        }
//
//
//
//        System.out.println();
//
//
////        startTime = System.currentTimeMillis();
////        RBFInterpolation rbf = new RBFInterpolation(upperVar, obj, new GaussianRadialBasis());
////        System.out.println("Local search cost: " + Search.getElapsedTime(startTime)/1000);
////
////        double[] test = new double[250];
////        for (int j = 0; j < 250; j++) {
////            test[j] = Math.random();
////        }
////
////        startTime = System.currentTimeMillis();
////        double pred = rbf.interpolate(test);
////        System.out.println("pred: " + pred);
////        System.out.println("Local search cost: " + Search.getElapsedTime(startTime)/1000);
//
//
//
//    }

}
