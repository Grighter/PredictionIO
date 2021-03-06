package io.prediction.commons.settings

import io.prediction.commons.Spec

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class OfflineEvalResultsSpec extends Specification {
  def is = s2"""

  PredictionIO OfflineEvalResults Specification

    OfflineEvalResults can be implemented by:
    - MongoOfflineEvalResults ${mongoOfflineEvalResults}

  """

  def mongoOfflineEvalResults = s2"""

    MongoOfflineEvalResults should
    - behave like any OfflineEvalResults implementation ${offlineEvalResultsTest(newMongoOfflineEvalResults)}
    - (database cleanup) ${Step(Spec.mongoClient(mongoDbName).dropDatabase())}

  """

  def offlineEvalResultsTest(offlineEvalResults: OfflineEvalResults) = s2"""

    get two OfflineEvalResults by evalid ${getByEvalid(offlineEvalResults)}
    delete two OfflineEvalResults by evalid ${deleteByEvalid(offlineEvalResults)}
    backup and restore OfflineEvalResults ${backuprestore(offlineEvalResults)}

  """

  val mongoDbName = "predictionio_mongoofflineevalresults_test"
  def newMongoOfflineEvalResults = new mongodb.MongoOfflineEvalResults(Spec.mongoClient(mongoDbName))

  /**
   * save a few and get by evalid
   */
  def getByEvalid(offlineEvalResults: OfflineEvalResults) = {
    val obj1 = OfflineEvalResult(
      evalid = 16,
      metricid = 2,
      algoid = 3,
      score = 0.09876,
      iteration = 1,
      splitset = "test"
    )
    val obj2 = OfflineEvalResult(
      evalid = 16,
      metricid = 2,
      algoid = 3,
      score = 0.123,
      iteration = 2, // only this is diff from obj1
      splitset = "test"
    )
    val obj3 = OfflineEvalResult(
      evalid = 16,
      metricid = 2,
      algoid = 3,
      score = 0.123,
      iteration = 2,
      splitset = "validation" // only this is diff from obj2
    )
    val obj4 = OfflineEvalResult(
      evalid = 2,
      metricid = 3,
      algoid = 4,
      score = 0.567,
      iteration = 3,
      splitset = ""
    )

    val id1 = offlineEvalResults.save(obj1)
    val id2 = offlineEvalResults.save(obj2)
    val id3 = offlineEvalResults.save(obj3)
    val id4 = offlineEvalResults.save(obj4)

    val it = offlineEvalResults.getByEvalid(16).toSeq

    /*
    val itData1 = it.next()
    val itData2 = it.next()
    val itData3 = it.next()
    */

    val it2 = offlineEvalResults.getByEvalidAndMetricidAndAlgoid(2, 3, 4).toSeq

    //val it2Data1 = it2.next()

    /*
    itData1 must be equalTo (obj1) and
      (itData2 must be equalTo (obj2)) and
      (itData3 must be equalTo (obj3)) and
      (it.hasNext must be_==(false)) and // make sure it has 2 only
      (it2Data1 must equalTo(obj4)) and
      (it2.hasNext must be_==(false))
      */
    it must contain(obj1) and (it must contain(obj2)) and
      (it must contain(obj3)) and (it.size must be_==(3)) and
      (it2 must contain(obj4)) and (it2.size must be_==(1))
  }

  /**
   * save a few and delete by evalid and get back
   */
  def deleteByEvalid(offlineEvalResults: OfflineEvalResults) = {
    val obj1 = OfflineEvalResult(
      evalid = 25,
      metricid = 6,
      algoid = 8,
      score = 0.7601,
      iteration = 1,
      splitset = "abc"
    )
    val obj2 = OfflineEvalResult(
      evalid = 7,
      metricid = 1,
      algoid = 9,
      score = 0.001,
      iteration = 2,
      splitset = ""
    )
    val obj3 = OfflineEvalResult(
      evalid = 25,
      metricid = 33,
      algoid = 41,
      score = 0.999,
      iteration = 1,
      splitset = "efg"
    )

    val id1 = offlineEvalResults.save(obj1)
    val id2 = offlineEvalResults.save(obj2)
    val id3 = offlineEvalResults.save(obj3)

    val it1 = offlineEvalResults.getByEvalid(25)

    val it1Data1 = it1.next()
    val it1Data2 = it1.next()

    offlineEvalResults.deleteByEvalid(25)

    val it2 = offlineEvalResults.getByEvalid(25)
    val it3 = offlineEvalResults.getByEvalid(7) // others shouldn't be deleted
    val it3Data1 = it3.next()

    it1Data1 must be equalTo (obj1) and
      (it1Data2 must be equalTo (obj3)) and
      (it1.hasNext must be_==(false)) and //make sure it has 2 only
      (it2.hasNext must be_==(false))
    (it3Data1 must be equalTo (obj2)) and
      (it3.hasNext must be_==(false))

  }

  def backuprestore(offlineEvalResults: OfflineEvalResults) = {
    val obj1 = OfflineEvalResult(
      evalid = 26,
      metricid = 6,
      algoid = 10,
      score = 0.7601,
      iteration = 1,
      splitset = "abc"
    )
    val obj2 = OfflineEvalResult(
      evalid = 8,
      metricid = 1,
      algoid = 11,
      score = 0.001,
      iteration = 2,
      splitset = ""
    )
    offlineEvalResults.save(obj1)
    offlineEvalResults.save(obj2)
    val fn = "results.json"
    val fos = new java.io.FileOutputStream(fn)
    try {
      fos.write(offlineEvalResults.backup())
    } finally {
      fos.close()
    }
    offlineEvalResults.restore(scala.io.Source.fromFile(fn)(scala.io.Codec.UTF8).mkString.getBytes("UTF-8")) map { data =>
      (data must contain(obj1)) and (data must contain(obj2))
    } getOrElse 1 === 2
  }
}
