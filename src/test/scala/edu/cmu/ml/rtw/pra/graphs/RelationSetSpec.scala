package edu.cmu.ml.rtw.pra.graphs

import java.io.BufferedReader
import java.io.StringReader

import org.scalatest._

import edu.cmu.ml.rtw.users.matt.util.Dictionary
import edu.cmu.ml.rtw.users.matt.util.FakeFileWriter
import edu.cmu.ml.rtw.users.matt.util.TestUtil
import edu.cmu.ml.rtw.users.matt.util.TestUtil.Function

import scala.collection.mutable
import scala.collection.JavaConverters._

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

class RelationSetSpec extends FlatSpecLike with Matchers {
  val relationFile = "test relation file"
  val relationPrefix = "test relation prefix"
  val isKb = true
  val aliasFile = "test alias file"
  val aliasesOnly = false
  val aliasRelation = "alias"
  val aliasFileFormat = "nell"
  val embeddingsFile = "test embeddings file"
  val prefix = "prefix-"
  val keepOriginalEdges = false
  val alias1 = "alias1"
  val alias2 = "alias2"
  val concept1 = "concept1"
  val concept2 = "concept2"
  val embedded1 = "embedded1"
  val embedded2 = "embedded2"
  val np1 = "np1"
  val np2 = "np2"
  val relation1 = "relation1"
  val relation2 = "relation2"
  val verb1 = "verb1"
  val verb2 = "verb2"
  val nodeDict = new Dictionary()
  val edgeDict = new Dictionary()
  val concept1Index = nodeDict.getIndex(concept1)
  val concept2Index = nodeDict.getIndex(concept2)
  val np1Index = nodeDict.getIndex(np1)
  val np2Index = nodeDict.getIndex(np2)
  val verb1Index = edgeDict.getIndex(prefix + verb1)
  val verb2Index = edgeDict.getIndex(prefix + verb2)
  val embedded1Index = edgeDict.getIndex(prefix + embedded1)
  val embedded2Index = edgeDict.getIndex(prefix + embedded2)
  val aliasRelationIndex = edgeDict.getIndex(aliasRelation)
  val relation1Index = edgeDict.getIndex(prefix + relation1)
  val relation2Index = edgeDict.getIndex(prefix + relation2)
  val relationNoPrefix1Index = edgeDict.getIndex(relation1)
  val relationFileContents =
    np1 + "\t" + verb1 + "\t" + np2 + "\n" +
    np1 + "\t" + verb2 + "\t" + np2 + "\n"
  val kbRelationFileContents =
    concept1 + "\t" + concept2 + "\t" + relation1 + "\n" +
    concept1 + "\t" + concept2 + "\t" + relation2 + "\n"

  val embeddings = Map(verb1 -> List(embedded1, embedded2))
  val kbEmbeddings = Map(relation1 -> List(embedded1, embedded2))
  val aliasesSet = Map(np1 -> List(concept1), np2 -> List(concept1, concept2))
  val aliases = List(("alias", aliasesSet))

  val defaultParams =
    ("relation file" -> relationFile) ~
    ("is kb" -> isKb) ~
    ("alias file" -> aliasFile) ~
    ("aliases only" -> aliasesOnly) ~
    ("alias relation" -> aliasRelation) ~
    ("alias file format" -> aliasFileFormat) ~
    ("embeddings file" -> embeddingsFile) ~
    ("keep original edges" -> keepOriginalEdges)

  def expectCount[T](collection: Seq[T], element: T, count: Int) {
    val actualCount = collection.filter(_.equals(element)).size
    actualCount should be(count)
  }

  "getAliases" should "read NELL formatted aliases" in {
    val params: JValue = ("alias file format" -> "nell")
    val relationSet = new RelationSet(defaultParams merge params)

    val aliasFile =
     concept1 + "\t" + np1 + "\n" +
     concept1 + "\t" + np2 + "\n" +
     concept2 + "\t" + np2 + "\n"

    val reader = new BufferedReader(new StringReader(aliasFile))
    val aliases = relationSet.getAliasesFromReader(reader)
    expectCount(aliases(np1), concept1, 1)
    expectCount(aliases(np2), concept1, 1)
    expectCount(aliases(np2), concept2, 1)
  }

  it should "read freebase formatted aliases" in {
    val params: JValue = ("alias file format" -> "freebase")
    val relationSet = new RelationSet(defaultParams merge params)

    var aliasFile = ""
    // Good rows that should be seen in the resultant map.
    aliasFile += concept1 + "\t/type/object/name\t/lang/en\t" + alias1 + "\n"
    aliasFile += concept2 + "\t/common/topic/alias\t/lang/en\t" + alias1 + "\n"
    aliasFile += concept2 + "\tdoesn't really matter\t/lang/en\t" + alias2 + "\n"
    // Bad rows that should be filtered out.
    aliasFile += concept1 + "\t/type/object/key\t/wikipedia/en\twiki1\n"
    aliasFile += concept1 + "\t/type/object/name\t/lang/en\t" + alias1 + "\textra column\n"
    aliasFile += concept1 + "\t/type/object/name\t/lang/en\n"

    val reader = new BufferedReader(new StringReader(aliasFile))
    val aliases = relationSet.getAliasesFromReader(reader)
    aliases.size should be(2)
    expectCount(aliases(alias1), concept1, 1)
    expectCount(aliases(alias1), concept2, 1)
    expectCount(aliases(alias2), concept2, 1)
  }

  it should "fail with unrecognized format" in {
    TestUtil.expectError(classOf[IllegalStateException], "bad alias file format", new Function() {
      override def call() {
        val params: JValue = ("alias file format" -> "unknown")
        val relationSet = new RelationSet(defaultParams merge params)
        val reader = new BufferedReader(new StringReader(""))
        val aliases = relationSet.getAliasesFromReader(reader)
      }
    })
  }

  "getEmbeddedRelations" should "just return the relation with empty embeddings map" in {
    val params: JValue = ("is kb" -> false)
    val relationSet = new RelationSet(defaultParams merge params)

    val result = relationSet.getEmbeddedRelations(relation1, Map())
    result.size should be(1)
    expectCount(result, relation1, 1)
  }

  it should "just return the relation with null embeddings map" in {
    val params: JValue = ("is kb" -> false)
    val relationSet = new RelationSet(defaultParams merge params)

    val result = relationSet.getEmbeddedRelations(relation1, null)
    result.size should be(1)
    expectCount(result, relation1, 1)
  }

  it should "return embeddings with they are present" in {
    val params: JValue = ("is kb" -> false)
    val relationSet = new RelationSet(defaultParams merge params)
    val embeddings = Map(relation1 -> List(embedded1, embedded2))

    val result = relationSet.getEmbeddedRelations(relation1, embeddings)
    result.size should be(2)
    expectCount(result, embedded1, 1)
    expectCount(result, embedded2, 1)
  }

  it should "keep original edges when requested" in {
    val params: JValue = ("is kb" -> false) ~ ("keep original edges" -> true)
    val relationSet = new RelationSet(defaultParams merge params)
    val embeddings = Map(relation1 -> List(embedded1, embedded2))
    val result = relationSet.getEmbeddedRelations(relation1, embeddings)
    result.size should be(3)
    expectCount(result, embedded1, 1)
    expectCount(result, embedded2, 1)
    expectCount(result, relation1, 1)
  }

  "writeRelationEdges" should "write surface relation sets correctly" in {
    val params: JValue = ("is kb" -> false)
    val relationSet = new RelationSet(defaultParams merge params)

    val seenNps = new mutable.HashSet[String]
    val writer = new FakeFileWriter()
    val reader = new BufferedReader(new StringReader(relationFileContents))

    val expected = List(
      // Np-to-concept edges.
      np1Index + "\t" + concept1Index + "\t" + aliasRelationIndex + "\n",
      np2Index + "\t" + concept1Index + "\t" + aliasRelationIndex + "\n",
      np2Index + "\t" + concept2Index + "\t" + aliasRelationIndex + "\n",
      // And verb edges.
      np1Index + "\t" + np2Index + "\t" + embedded1Index + "\n",
      np1Index + "\t" + np2Index + "\t" + embedded2Index + "\n",
      np1Index + "\t" + np2Index + "\t" + verb2Index + "\n")
    val numEdges = relationSet.writeRelationEdgesFromReader(
      reader, embeddings, null, prefix, seenNps, aliases, writer, nodeDict, edgeDict)
    numEdges should be(expected.size)
    writer.expectWritten(expected.asJava)
  }

  it should "write aliases only correctly" in {
    val params: JValue = ("is kb" -> false ) ~ ("aliases only" -> true)
    val relationSet = new RelationSet(defaultParams merge params)

    val seenNps = new mutable.HashSet[String]
    val writer = new FakeFileWriter()
    val reader = new BufferedReader(new StringReader(relationFileContents))

    val expected = List(
      // Np-to-concept edges.
      np1Index + "\t" + concept1Index + "\t" + aliasRelationIndex + "\n",
      np2Index + "\t" + concept1Index + "\t" + aliasRelationIndex + "\n",
      np2Index + "\t" + concept2Index + "\t" + aliasRelationIndex + "\n")
    val numEdges = relationSet.writeRelationEdgesFromReader(
      reader, embeddings, null, prefix, seenNps, aliases, writer, nodeDict, edgeDict)
    numEdges should be(expected.size)
    writer.expectWritten(expected.asJava)
  }

  it should "write KB relations correctly" in {
    val params: JValue = ("is kb" -> true)
    val relationSet = new RelationSet(defaultParams merge params)

    val seenNps = new mutable.HashSet[String]
    val writer = new FakeFileWriter()
    val reader = new BufferedReader(new StringReader(kbRelationFileContents))

    val expected = List(
      concept1Index + "\t" + concept2Index + "\t" + embedded1Index + "\n",
      concept1Index + "\t" + concept2Index + "\t" + embedded2Index + "\n",
      concept1Index + "\t" + concept2Index + "\t" + relation2Index + "\n")
    val numEdges = relationSet.writeRelationEdgesFromReader(
      reader, kbEmbeddings, null, prefix, seenNps, aliases, writer, nodeDict, edgeDict)
    numEdges should be(expected.size)
    writer.expectWritten(expected.asJava)
  }

  it should "handle prefixes correctly" in {
    val params: JValue = ("is kb" -> true) ~ ("relation prefix" -> prefix)
    val relationSet = new RelationSet(defaultParams merge params)
    val relations = concept1 + "\t" + concept2 + "\t" + relation1 + "\n"

    val seenNps = new mutable.HashSet[String]
    val writer = new FakeFileWriter()
    val reader = new BufferedReader(new StringReader(relations))

    val expected = List(concept1Index + "\t" + concept2Index + "\t" + relation1Index + "\n")
    val numEdges = relationSet.writeRelationEdgesFromReader(
      reader, Map(), null, null, seenNps, aliases, writer, nodeDict, edgeDict)
    numEdges should be(expected.size)
    writer.expectWritten(expected.asJava)
  }

  it should "handle the absense of prefixes correctly" in {
    val params: JValue = ("is kb" -> true)
    val relationSet = new RelationSet(defaultParams merge params)
    val relations = concept1 + "\t" + concept2 + "\t" + relation1 + "\n"

    val seenNps = new mutable.HashSet[String]
    val writer = new FakeFileWriter()
    val reader = new BufferedReader(new StringReader(relations))

    val expected = List(concept1Index + "\t" + concept2Index + "\t" + relationNoPrefix1Index + "\n")
    val numEdges = relationSet.writeRelationEdgesFromReader(
      reader, Map(), null, null, seenNps, aliases, writer, nodeDict, edgeDict)
    numEdges should be(expected.size)
    println(expected)
    println(writer.getWritten)
    println(edgeDict.getString(8))
    println(edgeDict.getString(9))
    writer.expectWritten(expected.asJava)
  }
}
