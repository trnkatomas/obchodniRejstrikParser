package eu.trnkatomas.obchodniRejstrik

import scala.xml.{XML, Node}
import java.io.{File, BufferedWriter, FileWriter}

// https://alvinalexander.com/scala/xml-parsing-xpath-extract-xml-tag-attributes/

import org.rogach.scallop._

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val input_file = opt[String](required = true)
  val output_file = opt[String](required = true)
  val type_of_input = choice(Seq("sro", "as"), required = true)
  val compressed = opt[Boolean]()
  verify()
}

object ORParser {

  def main(args: Array[String]): Unit = {
    val conf = new Conf(args)
    val input_file = conf.input_file.apply()
    val output_file = conf.output_file.apply()
    val gzipped = conf.compressed
    val sep = "\t"
    // val input_file_gz = new GZIPInputStream(new BufferedInputStream(new FileInputStream("data.bin")))
    // input_file_gz.
    //val xml = XML.load(input_file_gz)
    val xml = XML.loadFile(input_file)
    val file = new File(output_file)
    val fw = new BufferedWriter(new FileWriter(file))

    fw.write(s"${java.time.LocalDateTime.now}\n")
    val subjects = xml \ "Subjekt"
    val str_out = subjects.map(x => processSubjectSRO(x, sep))
    for (line <- str_out){
      fw.write(line)
    }
    fw.write(s"${java.time.LocalDateTime.now}\n")
    fw.flush()
    fw.close()
  }

  def processSubjectSRO(subject: Node, sep: String): String = {
    var output = ""
    val line_start = Seq((subject \ "nazev").text, (subject \ "ico").text).mkString(sep)
    val udaje = subject \ "udaje" \ "Udaj"
    for (u <- udaje) {
      val elem_value = (u \ "hlavicka").text
      if (elem_value == "Společníci") {
        val subelems = u \ "podudaje" \ "Udaj"
        for (s <- subelems) {
          //println(s)
          val header = s \ "hlavicka"
          val itemType = s \ "hodnotaUdaje" \ "typ"
          val nazev = s \ "osoba" \ "nazev"
          val ico = s \ "osoba" \ "ico"
          val moneyEntry = s \\ "vklad" \ "textValue"
          val share = s \\ "souhrn" \ "textValue"
          val dateEntered = s \ "zapisDatum"
          val dateErased = s \ "vymazDatum"
          if (ico.nonEmpty) {
            val shareChange = Seq(line_start, header.text, nazev.text, ico.text,
              moneyEntry.text, share.text, dateEntered.text, dateErased.text)
            //println(shareChange.mkString(sep))
            output += shareChange.mkString(sep) + "\n"
          }
        }
      }
    }
    output
  }
}
