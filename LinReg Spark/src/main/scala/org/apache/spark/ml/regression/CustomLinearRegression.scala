package org.apache.spark.ml.regression

import breeze.linalg.DenseVector
import breeze.linalg.functions.euclideanDistance
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.param.shared.{HasMaxIter, HasTol}
import org.apache.spark.ml.param.{DoubleParam, Param, ParamMap}
import org.apache.spark.ml.linalg.{Vector, VectorUDT, Vectors}
import org.apache.spark.ml.util._
import org.apache.spark.ml.{Estimator, Model, PredictorParams}
import org.apache.spark.mllib.linalg.{Vectors => MLLibVectors}
import org.apache.spark.mllib.stat.MultivariateOnlineSummarizer
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Dataset, Encoder}


trait CustomLinearRegressionParams extends PredictorParams with HasMaxIter with HasTol {
    final val learningRate: Param[Double] = new DoubleParam(
        this,
        "learningRate",
        "learning rate"
    )
    def getLearningRate: Double = $(learningRate)
    def setMaxIter(value: Int): this.type = set(maxIter, value)

    setDefault(maxIter -> 1000, learningRate -> 0.05, tol -> 1e-7)
    protected def validateAndTransformSchema(schema: StructType): StructType = {
        SchemaUtils.checkColumnType(schema, getFeaturesCol, new VectorUDT())
        if (schema.fieldNames.contains($(predictionCol))) {
            SchemaUtils.checkColumnType(schema, getPredictionCol, new VectorUDT())
            schema
        } else {
            SchemaUtils.appendColumn(schema, schema(getFeaturesCol).copy(name = getPredictionCol))
        }
    }
}

class CustomLinearRegression(override val uid: String) extends Estimator[CustomLinearRegressionModel] with CustomLinearRegressionParams with DefaultParamsWritable {

    def this() = this(Identifiable.randomUID("linearRegression"))

    override def fit(dataset: Dataset[_]): CustomLinearRegressionModel = {
        implicit val vectorEncoder: Encoder[Vector] = ExpressionEncoder()
        val assembler = new VectorAssembler().setInputCols(Array(getFeaturesCol, getLabelCol)).setOutputCol("result")
        val vectors = assembler.transform(dataset).select("result").as[Vector]
        val count = vectors.first().size - 1
        val epochs = getMaxIter
        val learningRate = getLearningRate
        val tolerance = getTol
        var oldWeights = DenseVector.fill(count)(Double.PositiveInfinity)
        var weights = DenseVector.fill(count)(0.0)
        var i = 0
        while (i < epochs && euclideanDistance(weights, oldWeights) > tolerance) {
            i += 1
            val summary = vectors.rdd.mapPartitions { data =>
                val summarizer = new MultivariateOnlineSummarizer()
                data.foreach { row =>
                    val x = row.asBreeze(0 until count).toDenseVector
                    val y = row.asBreeze(-1)
                    val yPred = x.dot(weights)
                    summarizer.add(MLLibVectors.fromBreeze((yPred - y) * x))
                }
                Iterator(summarizer)
            }.reduce(_ merge _)
            oldWeights = weights.copy
            weights -= learningRate * summary.mean.asBreeze
        }
        println(s"Finished at epoch $i")
        copyValues(new CustomLinearRegressionModel(Vectors.fromBreeze(weights)).setParent(this))
    }

    override def copy(extra: ParamMap): Estimator[CustomLinearRegressionModel] = defaultCopy(extra)

    override def transformSchema(schema: StructType): StructType = validateAndTransformSchema(schema)
}

class CustomLinearRegressionModel(override val uid: String, val coefficients: Vector) extends Model[CustomLinearRegressionModel] with CustomLinearRegressionParams {
    private[regression] def this(coefficients: Vector) = this(Identifiable.randomUID("linearRegressionModel"), coefficients)

    override def transformSchema(schema: StructType): StructType = {
        var outputSchema = validateAndTransformSchema(schema)
        if ($(predictionCol).nonEmpty) {
            outputSchema = SchemaUtils.updateNumeric(outputSchema, $(predictionCol))
        }
        outputSchema
    }

    private def predict(features: Vector) = features.asBreeze.dot(coefficients.asBreeze)

    override def transform(dataset: Dataset[_]): DataFrame = {
        val outputSchema = transformSchema(dataset.schema)
        val predictUDF = udf { features: Any =>
            predict(features.asInstanceOf[Vector])
        }
        dataset.withColumn($(predictionCol), predictUDF(col($(featuresCol))), outputSchema($(predictionCol)).metadata)
    }

    override def copy(extra: ParamMap): CustomLinearRegressionModel = copyValues(new CustomLinearRegressionModel(coefficients), extra)
}
