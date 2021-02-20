package eu.trnkatomas.obchodniRejstrik

import scala.xml.{Node, XML}
import java.io.{BufferedInputStream, BufferedWriter, File, FileInputStream, FileWriter, InputStream}
import java.util.zip.GZIPInputStream
import java.time.temporal.ChronoUnit
// https://alvinalexander.com/scala/xml-parsing-xpath-extract-xml-tag-attributes/


import org.rogach.scallop._

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val input_file = opt[String](required = true)
  val output_file = opt[String](required = true)
  val type_of_input = choice(Seq("sro", "as"), required = true)
  val separator = opt[String](default = Option("\t"))
  val compressed = opt[Boolean]()
  verify()
}

object ORParser {

  def main(args: Array[String]): Unit = {
    val conf = new Conf(args)
    val input_file = conf.input_file.apply()
    val output_file = conf.output_file.apply()
    val sep = conf.separator.apply()
    val start = java.time.LocalDateTime.now
    val xml = if (conf.compressed.apply()) {
      val input_file_gz = new GZIPInputStream(new BufferedInputStream(new FileInputStream(input_file)))
      XML.load(input_file_gz)
    } else {
      XML.loadFile(input_file)
    }
    val loadingMs = java.time.LocalDateTime.now
    println(s"Loading time: ${ChronoUnit.MILLIS.between(start, loadingMs)} ms")
    val fw = new BufferedWriter(new FileWriter(new File(output_file)))
    val subjects = (xml \ "Subjekt")
    val str_out: Seq[String] = conf.type_of_input.apply() match {
      case "sro" => subjects.map(x => processSubjectSRO(x, sep))
      case "as" => subjects.map(x => processSubjectAS(x, sep))
    }
    for (line <- str_out){
      fw.write(line)
    }
    fw.flush()
    fw.close()
    val processMs = java.time.LocalDateTime.now
    println(s"Processing time: ${ChronoUnit.MILLIS.between(loadingMs, processMs)} ms")
    println(s"Total time elapsed: ${ChronoUnit.MILLIS.between(start, processMs)} ms")
  }

  def determineTypeOfParentCompany(s: Node): (String, String) ={
    s match {
      case s if (s \ "osoba" \ "ico").nonEmpty => ((s \ "osoba" \ "ico").text, "CZECH_COMPANY")
      case s if (s \ "osoba" \ "regCislo").nonEmpty => ((s \ "osoba" \ "regCislo").text, "FOREIGN_COMPANY")
      case s if (s \ "osoba" \ "_").length == 1 => ("0", "WEIRD_SUBJECT")
      case _ => ("", "")
    }
  }

  def processSubjectSRO(subject: Node, sep: String): String = {
    var output = ""
    val companyName = (subject \ "nazev").text
    val companyId = (subject \ "ico").text
    val dateCreated = (subject \ "zapisDatum").text
    val dateCeased = (subject \ "vymazDatum").text
    val line_start = Seq(companyName, companyId, dateCreated, dateCeased)
    val udaje = subject \ "udaje" \ "Udaj"
    for (u <- udaje) {
      if ((u \ "udajTyp" \ "kod").text == "SPOLECNIK") {
        val subelems = u \ "podudaje" \ "Udaj"
        for (s <- subelems) {
          //println(s)
          val header = s \ "hlavicka"
          val itemType = s \ "hodnotaUdaje" \ "typ"
          val nazev = s \ "osoba" \ "nazev"
          val (ico, companyType) = determineTypeOfParentCompany(s)
          if (ico.nonEmpty) {
            for (changeEntry <- s \ "podudaje" \ "Udaj") {
              val moneyEntryType = changeEntry \\ "vklad" \ "typ"
              val moneyEntry = changeEntry \\ "vklad" \ "textValue"
              val share = changeEntry \\ "souhrn" \ "textValue"
              val shareType = changeEntry \\ "souhrn" \ "typ"
              val payment = changeEntry \\ "splaceni" \ "textValue"
              val paymentType = changeEntry \\ "splaceni" \ "typ"
              val dateEntered = changeEntry \\ "zapisDatum"
              val dateErased = changeEntry \\ "vymazDatum"
              val entryType = changeEntry \ "udajTyp" \ "kod"
              val shareChange = line_start ++ Seq(header.text, nazev.text, ico, companyType,
                                                  moneyEntryType.text, moneyEntry.text,
                                                  paymentType.text, payment.text,
                                                  shareType.text, share.text,
                                                  entryType.text, dateEntered.text, dateErased.text)
              output += shareChange.mkString(sep) + "\n"
            }
          }
        }
      }
    }
    output
  }

  def processSubjectAS(subject: Node, sep: String): String = {
    var output = ""
    val companyName = (subject \ "nazev").text
    val companyId = (subject \ "ico").text
    val dateCreated = (subject \ "zapisDatum").text
    val dateCeased = (subject \ "vymazDatum").text
    val line_start = Seq(companyName, companyId, dateCreated, dateCeased)
    val udaje = subject \ "udaje" \ "Udaj"
    for (u <- udaje) {
      if ((u \ "udajTyp" \ "kod").text == "AKCIONAR_SEKCE") {
        val subelems = u \ "podudaje" \ "Udaj"
        for (s <- subelems) {
          //println(s)
          val header = s \ "hlavicka"
          val itemType = s \ "hodnotaUdaje" \ "typ"
          val nazev = s \ "osoba" \ "nazev"
          val (ico, companyType) = determineTypeOfParentCompany(s)
          if (ico.nonEmpty) {
            val dateEntered = s \ "zapisDatum"
            val dateErased = s \ "vymazDatum"
            val entryType = s \ "udajTyp" \ "kod"
            val shareChange = line_start ++ Seq(header.text, nazev.text, ico, companyType,
              entryType.text, dateEntered.text, dateErased.text)
            output += shareChange.mkString(sep) + "\n"
          }
        }
      }
    }
    output
  }

}
