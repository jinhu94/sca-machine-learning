/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mrow4a.spark.java;

import mrow4a.spark.java.alg.AssocRules;
import mrow4a.spark.java.alg.Baskets;
import mrow4a.spark.java.alg.AprioriFreqItemSets;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.SparkConf;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class FreqItemSetsJob {

    public static void main(String[] args) throws Exception {
        SparkConf conf = new SparkConf()
                .setAppName("FreqItemSetsJob")
                .setMaster("local[*]");
        SparkSession spark = SparkSession
                .builder()
                .config(conf)
                .getOrCreate();
        JavaSparkContext jsc = JavaSparkContext.fromSparkContext(spark.sparkContext());

        // /home/mrow4a/Projects/machine-learning/freq-items-assoc-rules/datasets/T10I4D100K.dat
        if (args.length < 1) {
            System.err.println();
            System.err.println("Usage: FreqItemSetsJob <file>");
            System.err.println();
            System.exit(1);
        }

        Instant start = Instant.now();

        JavaRDD<String> lines = jsc.textFile(args[0]);

        /*
         * Convert raw lines to RDD of vectors containing item ids (baskets of items)
         */
        JavaRDD<List<String>> baskets = lines
                .map(Baskets::parse);

        /*
         * Set support threshold to 0.01 (1%) and obtain frequent items using
         * A-Priori algorithm
         */
        double supportThreshold = 0.01;
        JavaPairRDD<List<String>, Integer> frequentItemSets = AprioriFreqItemSets
                .get(baskets, supportThreshold).cache();

        /*
         * Set confidence threshold to 0.5 (50%) and obtain association rules
         */
        double confidenceThreshold = 0.5;
        JavaPairRDD<Tuple2<List<String>, List<String>>, Double> associationRules = AssocRules
                .get(frequentItemSets, confidenceThreshold);

        /*
         * Print results
         */
        List<Tuple2<List<String>, Integer>> frequentItemSetsCollect = frequentItemSets.collect();
        List<Tuple2<Tuple2<List<String>, List<String>>, Double>> associationRulesCollect = associationRules.collect();
        Instant end = Instant.now();

        System.out.println("Frequent Itemsets: ");
        for (Tuple2<List<String>, Integer> tuple : frequentItemSetsCollect) {
            System.out.println(tuple._1() + ": " + tuple._2().toString());
        }
        System.out.println();

        System.out.println("Association rules detected: ");
        for (Tuple2<Tuple2<List<String>, List<String>>, Double> tuple : associationRulesCollect) {
            System.out.println(tuple._1._1 + " -> " + tuple._1._2 + " with confidence " + tuple._2.toString());
        }
        System.out.println();

        System.out.println("Time: "+
                Duration.between(start, end).toMillis() +" milliseconds");
        System.out.println();
        spark.stop();
    }
}