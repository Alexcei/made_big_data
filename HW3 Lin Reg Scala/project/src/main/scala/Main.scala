import breeze.numerics._
import breeze.linalg._
import breeze.numerics.round

import java.io.File


object Main extends App {
  println("Read data")
  val (trainPath, testPath, predictPath) = ("input/train.csv", "input/test.csv", "input/output.csv")
  val target_index = 1

  val trainData = csvread(new File(trainPath), separator = ',', skipLines = 1)
  val trainX = trainData(::, 0 until target_index)
  val trainY = trainData(::, target_index)

  val testData = csvread(new File(testPath), separator = ',', skipLines = 1)
  val testX = testData(::, 0 until target_index)
  val testY = testData(::, target_index)
  println(s"Train size: ${trainX.rows} , test size: ${testX.rows}")

  val model = new linearRegration(.00001)
  model.fit(trainX, trainY)

  val trainPred = model.predict(trainX)
  println(s"MSE on train: ${model.MSE(trainY, trainPred)}")
  val testPred = model.predict(testX)
  println(s"MSE on test: ${model.MSE(testY, testPred)}")

  println("Show and save results")
  val result = convert(DenseMatrix.horzcat(round(testPred.toDenseMatrix.t), round(testY.toDenseMatrix.t)), Double)
  csvwrite(new File(predictPath), separator = ',', mat = result)
  println(s"Bias: ${model.a}, Cost: ${model.b}")
  println(s"Result save is ${predictPath}")


  class linearRegration(learR: Double) {
    val lr: Double = learR
    var a: Double = 0.0
    var b: DenseVector[Double] = DenseVector.zeros[Double](1)

    def fit(X: DenseMatrix[Double], y: DenseVector[Double]): Unit = {
      println("Fit model")
      b = DenseVector.zeros[Double](X.cols)
      for (_ <- 0 to 100) {
        var h = (X * b) + a
        a -= lr * sum(h - y) / h.length
        b :-= lr * sum(h - y) * a / h.length
      }
    }

    def predict(X: DenseMatrix[Double]): DenseVector[Double] = {
      X * b + a
    }

    def MSE(yTrue: DenseVector[Double], yPred: DenseVector[Double]): Double = {
      sqrt(sum(yTrue - yPred) / yTrue.length)
    }
  }
}

