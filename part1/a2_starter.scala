import org.apache.spark
//
// CS 245 Assignment 2, Part I starter code
//
// 1. Loads Cities and Countries as dataframes and creates views
//    so that we can issue SQL queries on them.
// 2. Runs 2 example queries, shows their results, and explains
//    their query plans.
//

// note that we use the default `spark` session provided by spark-shell
val cities = (spark.read
        .format("csv")
        .option("header", "true") // first line in file has headers
        .load("./Cities.csv"));
cities.createOrReplaceTempView("Cities")

val countries = (spark.read
        .format("csv")
        .option("header", "true")
        .load("./Countries.csv"));
countries.createOrReplaceTempView("Countries")

// look at the schemas for Cities and Countries
cities.printSchema()
countries.printSchema()

// Example 1
var df = spark.sql("SELECT city FROM Cities")
df.show()  // display the results of the SQL query
df.explain(true)  // explain the query plan in detail:
                  // parsed, analyzed, optimized, and physical plans

// Example 2
df = spark.sql("""
    SELECT *
    FROM Cities
    WHERE temp < 5 OR true
""")
df.show()
df.explain(true)

// Example 3
df = spark.sql(
  """
    SELECT country, EU FROM Countries WHERE coastline = "yes"
    """)

// Example 4 (Problem 2)
df = spark.sql(
  """
    |SELECT city
    |FROM (
    |SELECT city, temp
    |FROM Cities
    |)
    |WHERE temp < 4
    |""".stripMargin
)

// Problem 3
df = spark.sql(
  """
    |SELECT *
    |FROM Cities, Countries
    |WHERE Cities.country = Countries.country AND Cities.temp < 4 AND Countries.pop > 6
    |""".stripMargin
)

// Problem 4
df = spark.sql(
  """
    |SELECT city, pop
    |FROM Cities, Countries
    |WHERE Cities.country = Countries.country AND Countries.pop > 6
    |""".stripMargin
)

// Problem 5
df = spark.sql(
  """
    |SELECT *
    |FROM Countries
    |WHERE country LIKE "%e%d"
    |""".stripMargin
)

// Problem 6
df = spark.sql(
  """
    |SELECT *
    |FROM Countries
    |WHERE country LIKE "%ia"
    |""".stripMargin)

// Problem 7
df = spark.sql(
  """
    |SELECT t1 + 1 as t2
    |FROM (
    |SELECT cast(temp as int) + 1 as t1
    |FROM Cities
    |)
    |""".stripMargin)