package TCYB;

import java.io.*;
import java.util.*;

import smile.math.rbf.GaussianRadialBasis;
import smile.interpolation.RBFInterpolation;
import smile.math.rbf.InverseMultiquadricRadialBasis;
import smile.math.rbf.MultiquadricRadialBasis;

public class Surrogate {



    public static double[] getUpperVar(ArrayList<ArrayList<Layout.GROUP>> layoutCode, SLAP slap){

        double[] upperVar = new double[slap.product.size()];

        ArrayList<Layout.GROUP> upperVarGroup = new ArrayList<>();
        for(ArrayList<Layout.GROUP> row : layoutCode){
            for (Layout.GROUP group : row){
                Surrogate.insertGroup(upperVarGroup, group);
            }
        }

        Collections.sort(upperVarGroup, new Comparator<Layout.GROUP>(){
            @Override
            public int compare(Layout.GROUP g1, Layout.GROUP g2){
                String productName1 = g1.product;
                String productName2 = g2.product;
                return productName1.compareTo(productName2);
            }
        });
        int idx = 0;

        for (Layout.GROUP group : upperVarGroup){
            String product = group.product;
            double size = (double) group.size;
            double totalSize = (double) slap.product.get(product).size();
            double proportion = size / totalSize;
            double normalizedProportion = (proportion - 0.5) / (1 - 0.5);
            upperVar[idx++] = normalizedProportion;
        }

        return upperVar;
    }

    private static void insertGroup(ArrayList<Layout.GROUP> upperVar, Layout.GROUP group){
        if(upperVar.size()==0)
            upperVar.add(group);
        else{
            Layout.GROUP addGroup = group;
            for (Layout.GROUP g : upperVar){
                if(g.product.equals(group.product)){
                    if(g.size > group.size){
                        addGroup = g;
                    }
                    upperVar.remove(g);
                    break;
                }
            }
            upperVar.add(addGroup);
        }
    }

    public static RBFInterpolation getSurrogateModel(double[][] train_var, double[] train_tar){
//        long startTime = System.currentTimeMillis();

        MinMaxScaler targetScaler = new MinMaxScaler(train_tar, 1, 0);
        train_tar = targetScaler.transform(train_tar);

        RBFInterpolation rbf = new RBFInterpolation(train_var, train_tar, new InverseMultiquadricRadialBasis());

//        System.out.println("Build RBF cost: " + Search.getElapsedTime(startTime)/1000);
        return rbf;
    }

}
