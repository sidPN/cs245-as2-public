package edu.stanford.cs245

import edu.stanford.cs245.Transforms.isDistUdf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.expressions.{And, BinaryComparison, EqualTo, Expression, GreaterThan, GreaterThanOrEqual, LessThan, LessThanOrEqual, Literal, Multiply, ScalaUDF}
import org.apache.spark.sql.catalyst.plans.logical.{LogicalPlan, Sort}
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.types.{BooleanType, DoubleType}

object Transforms {

  // Check whether a ScalaUDF Expression is our dist UDF
  def isDistUdf(udf: ScalaUDF): Boolean = {
    udf.udfName.getOrElse("") == "dist"
  }

  // Get an Expression representing the dist_sq UDF with the provided
  // arguments
  def getDistSqUdf(args: Seq[Expression]): ScalaUDF = {
    ScalaUDF(
      (x1: Double, y1: Double, x2: Double, y2: Double) => {
        val xDiff = x1 - x2
        val yDiff = y1 - y2
        xDiff * xDiff + yDiff * yDiff
      }, DoubleType, args, Seq(DoubleType, DoubleType, DoubleType, DoubleType),
      udfName = Some("dist_sq"))
  }

  // Return any additional optimization passes here
  def getOptimizationPasses(spark: SparkSession): Seq[Rule[LogicalPlan]] = {
    Seq(EliminateZeroDists(spark), DistanceNonNegative(spark))
  }

  case class EliminateZeroDists(spark: SparkSession) extends Rule[LogicalPlan] {
    def apply(plan: LogicalPlan): LogicalPlan = plan.transformAllExpressions {
      case udf: ScalaUDF if isDistUdf(udf) && udf.children(0) == udf.children(2) &&
        udf.children(1) == udf.children(3) => Literal(0.0, DoubleType)
      case EqualTo(udf: ScalaUDF, Literal(c: Double, DoubleType))
        if isDistUdf(udf) && c == 0 => And(
        EqualTo(udf.children(0), udf.children(2)), EqualTo(udf.children(1), udf.children(3))
      )
    }
  }

  case class DistanceNonNegative(spark: SparkSession) extends Rule[LogicalPlan] {
    def apply(plan: LogicalPlan): LogicalPlan = plan.transformAllExpressions {
      case LessThan(udf: ScalaUDF, Literal(c: Double, DoubleType)) if isDistUdf(udf) && c <= 0 => Literal(false, BooleanType)
      case GreaterThan(udf1: ScalaUDF, udf2: ScalaUDF) if isDistUdf(udf1) && isDistUdf(udf2) =>
        GreaterThan(getDistSqUdf(udf1.children), getDistSqUdf(udf2.children))
      case GreaterThan(udf: ScalaUDF, Literal(c: Double, DoubleType)) if isDistUdf(udf) =>
        GreaterThan(getDistSqUdf(udf.children), Literal(c*c, DoubleType))
      case EqualTo(udf: ScalaUDF, Literal(c: Double, DoubleType)) if isDistUdf(udf) && c < 0 => Literal(false, BooleanType)
      case GreaterThanOrEqual(Literal(c: Double, DoubleType), udf: ScalaUDF) if isDistUdf(udf) && c <= 0 => Literal(false, BooleanType)
      case LessThanOrEqual(Literal(c: Double, DoubleType), udf: ScalaUDF) if isDistUdf(udf) && c < 0 => Literal(true, BooleanType)
    }
  }


}
