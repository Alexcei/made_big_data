package org.apache.spark.ml.regression

import org.apache.spark.ml.linalg.{Matrices, Vector, Vectors}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.{col, udf}
import org.scalatest.flatspec._
import org.scalatest.matchers._
import scala.util.Random


class CustomLinearRegressionTest extends AnyFlatSpec with should.Matchers with WithSpark {
    val delta = 0.1
    lazy val data: DataFrame = StandardScalerTest.data
    lazy val vectors: Seq[Vector] = StandardScalerTest.vectors
    val predictUDF: UserDefinedFunction = StandardScalerTest.predictUDF

    "Model" should "predicted" in {
        val model = new CustomLinearRegressionModel(coefficients = Vectors.dense(2.1, 0.5, -0.8))
        validateModel(model.transform(data))
    }

    "Estimator" should "make produce" in {
        val estimator = new CustomLinearRegression()
        val dataset = data.withColumn("label", predictUDF(col("features")))
        val model = estimator.fit(dataset)
        validateModel(model.transform(dataset))
    }

    "Estimator" should "make predict" in {
        val estimator = new CustomLinearRegression().setMaxIter(1000)
        val randomData: Seq[(Vector, Double)] = Matrices.rand(1000, 3, Random.self)
          .rowIter
          .toSeq
          .map(x => (x, StandardScalerTest.predict(x)))
        import sqlContext.implicits._
        val dataset = randomData.toDF("features", "label")
        val model = estimator.fit(dataset)
        model.coefficients(0) should be(2.1 +- delta)
        model.coefficients(1) should be(0.5 +- delta)
        model.coefficients(2) should be(-0.8 +- delta)
    }

    private def validateModel(data: DataFrame): Unit = {
        val vector = data.collect().map(_.getAs[Double](1))
        vector.length should be(2)
        vector(0) should be(StandardScalerTest.predict(vectors.head) +- delta)
        vector(1) should be(StandardScalerTest.predict(vectors.last) +- delta)
    }
}

object StandardScalerTest extends WithSpark {

    lazy val vectors: Seq[Vector] = Seq(
        Vectors.dense(7, -8, 3),
        Vectors.dense(-1.5, 3, 5)
    )

    lazy val data: DataFrame = {
        import sqlContext.implicits._
        vectors.map(Tuple1.apply).toDF("features")
    }

    def predict(v: Vector): Double = {
        val arr = v.toArray
        2.1 * arr(0) + 0.5 * arr(1) - 0.8 * arr(2)
    }

    val predictUDF: UserDefinedFunction = udf(predict _)
}
