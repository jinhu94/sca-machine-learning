package se.kth.spark.lab1.task7

import org.apache.spark._
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.{RegexTokenizer, VectorSlicer}
import org.apache.spark.ml.regression.{LinearRegression, LinearRegressionModel}
import org.apache.spark.ml.tuning.{CrossValidator, CrossValidatorModel, ParamGridBuilder}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.sql.SparkSession
import se.kth.spark.lab1.{Array2Vector, DoubleUDF, Vector2DoubleUDF}

object LinRegTextSongs {
  def main(args: Array[String]) {
    val conf = new SparkConf()
      .setAppName("LinRegTextSongs")

    val spark = SparkSession
      .builder()
      .config(conf)
      .getOrCreate()

    import spark.implicits._

    // src/main/resources/million-song-all.txt
    if (args.length < 1) {
      System.err.println()
      System.err.println("Usage: LinRegTextSongs <file>")
      System.err.println()
      System.exit(1)
    }

    val filePath = args(0)
    val songsDf = spark.sparkContext.textFile(filePath)
      // Remove all " chars
      .map(record => record.replace("\"", ""))
      .toDF("record")
      .cache()
    val numSongs = songsDf.count()
    println(s"Songs: $numSongs")

    // Pre-process data (pipeline)
    val regexTokenizer = new RegexTokenizer()
      .setInputCol("record")
      .setOutputCol("record_array")
      .setPattern(",")
    val arr2Vect = new Array2Vector()
      .setInputCol("record_array")
      .setOutputCol("record_vector")

    // Prepare label (pipeline)
    val lSlicer = new VectorSlicer()
      .setInputCol("record_vector")
      .setOutputCol("year_vector")
      .setIndices(Array(0))
    val v2d = new Vector2DoubleUDF(year => year.apply(0))
      .setInputCol("year_vector")
      .setOutputCol("rawyear")
    val lShifter = new DoubleUDF(vector => vector - 1922.0)
      .setInputCol("rawyear")
      .setOutputCol("label")

    // Prepare features (pipeline)
    val fSlicer = new VectorSlicer()
      .setInputCol("record_vector")
      .setOutputCol("features")
      .setIndices(Array(1,2,3))

    // prepare linear regression
    val maxIter = 10
    val regParam = 0.1
    val elNet = 0.1
    val lrStage = new LinearRegression()
      .setMaxIter(maxIter)
      .setRegParam(regParam)
      .setElasticNetParam(elNet)
      .setLabelCol("label")
      .setFeaturesCol("features")

    // Construct linear regression training pipeline
    val lrPipeline = new Pipeline()
      .setStages(Array(
        regexTokenizer,
        arr2Vect,
        lSlicer,
        v2d,
        lShifter,
        fSlicer,
        lrStage))

    val lrModel: CrossValidatorModel = new CrossValidator()
      // We now treat the Pipeline as an Estimator
      .setEstimator(lrPipeline)
      // Use RegressionEvaluator
      .setEvaluator(
        new RegressionEvaluator
      )
      // A CrossValidator requires an Estimator, a regParam and maxIter params
      .setEstimatorParamMaps(
        new ParamGridBuilder()
          .addGrid(lrStage.regParam,
            Array(0.01, 0.05, 0.08, 0.25, 0.5, 0.95)
          )
          .addGrid(lrStage.maxIter,
            Array(1, 5, 8, 15, 45, 75)
          )
          .build()
      )
      // Run cross-validation, and choose the best set of parameters.
      .fit(songsDf)

    // Get predictions for test data
    lrModel.transform(songsDf)
      .select("label", "features", "prediction")
      .show(5)

    val lrBestModel = lrModel
      // Get best LinearRegression model
      .bestModel
      .asInstanceOf[PipelineModel]
      .stages(6) // lrStage is 6th
      .asInstanceOf[LinearRegressionModel]
    println(s"RMSE: ${lrBestModel.summary.rootMeanSquaredError} " +
      s"for parameters: maxIter[${lrBestModel.getMaxIter}], regParam[${lrBestModel.getRegParam}], elNet[${lrBestModel.getElasticNetParam}]")
  }
}