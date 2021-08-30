package TCYB;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

public class DataGenerator {
    /**
     Generate pseudo-random floating point values, with an
     approximately Gaussian (normal) distribution.

     reference: https://stackoverflow.com/questions/29389412/generating-numbers-which-follow-normal-distribution-in-java
     */
    class RandomGaussian {

        private Random fRandom = new Random();

        public double getGaussian(double aMean, double aVariance){
            return aMean + fRandom.nextGaussian() * aVariance;
        }


    }

    private RandomGaussian randomGaussian = new RandomGaussian();

    int maxfq = 1000;
    int minfq = 1;

    public static void main(String[] args) {

        DataGenerator dataGenerator = new DataGenerator();

        int L = 140;
        int N = 5040;
        int M = (int) Math.ceil((double) N/(double) L);

        int meanNp = 26;
        int maxNp = 162;

        double difficulty = 0.05;
        int sampleNum = 2;

        for (int i = 0; i < sampleNum; i++) {
            try {
                String fileName = N + "-" + (i + 1);
                BufferedWriter out = new BufferedWriter(new FileWriter(fileName));

                int currentN = N;
                int idx = 1;
                while (currentN > 0){
                    ArrayList<String> product = dataGenerator.generateProduct(idx,
                            (int) Math.min(maxNp, currentN),
                            meanNp, 1, difficulty);
                    currentN -= product.size() - 2;
                    for (String s : product){
                        out.write(s + "\n");
                    }
                    out.write("\n");
                    idx++;
                }
                out.close();
                System.out.println("generate data: " + fileName + " product in total: "+ Integer.toString(idx - 1)
                        + " item in total: " + Integer.toString(N - currentN));
            } catch (IOException e) {
            }
        }
    }

    private ArrayList<String> generateProduct(int idx, int maxNp, int meanNp, int minNp, double difficulty){
        ArrayList<String> product = new ArrayList<>();
        ArrayList<Integer> fqList = new ArrayList<>();

        String name = "P" + Integer.toString(idx);

        product.add("Product: " + name + " ");

        int Np = 0;
        if (maxNp <= 3){
            Np = maxNp;
        }
        else if(Math.random() < difficulty){
            Np = (Math.random() < 0.5) ? 1 : 2;
        } else{
            double GaussianRandom = randomGaussian.getGaussian(meanNp, meanNp - minNp);
            if (GaussianRandom < minNp || GaussianRandom > maxNp){
                GaussianRandom = (Math.random() * (maxNp - minNp)) + minNp;
            }
            Np = (int) GaussianRandom;
        }

        for (int i = 0; i < Np; i++) {
            String item = generateItem(name, i);
            int fq = Integer.parseInt(Split(item).get(2));
            fqList.add(fq);
            product.add(item);
        }

        double mean = 0;
        for (int fq : fqList) {
            mean += (double) fq / (double) fqList.size();
        }

        product.add(String.format("Average picking frequency: %.1f", mean));

        return product;
    }

    private String generateItem(String productName, int idx){
        String itemString = "Item: " + productName + " ";

        int pickFq = (int) ((Math.random() * (maxfq - minfq)) + minfq);

        itemString += Integer.toString(pickFq) + " ";

        itemString += "NA" + " ";
        itemString += productName + "Item" + Integer.toString(idx) + " ";

        return itemString;
    }

    private ArrayList<String> Split(String line) {
        ArrayList<String> SplittedString = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(line);

        while (st.hasMoreElements()) {
            String cur = st.nextElement().toString();
            SplittedString.add(cur);
        }
        return SplittedString;
    }
}
