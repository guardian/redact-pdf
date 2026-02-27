package redact

import redact.PdfRedactor
import java.io.{File, FileOutputStream}
import scala.util.Using

object Main {
  def main(args: Array[String]): Unit = {
    // 1. Basic argument validation
    if (args.length < 2) {
      System.err.println("Usage: java -jar CvRedactor.jar <input.pdf> <output.pdf> [name1 name2 ...]")
      System.err.println("Example: java -jar CvRedactor.jar in.pdf out.pdf Jane Doe")
      sys.exit(1)
    }

    val inputFilePath = args(0)
    val outputFilePath = args(1)
    
    // Capture any remaining arguments as names to redact
    val namesToRedact = args.drop(2).toList

    val inputFile = new File(inputFilePath)

    // 2. Validate input file exists
    if (!inputFile.exists() || !inputFile.isFile) {
      System.err.println(s"Error: Cannot find input file at $inputFilePath")
      sys.exit(1)
    }

    // 3. Validate that the input file appears to be a PDF based on its extension
    if (!inputFilePath.toLowerCase.endsWith(".pdf")) {
      System.err.println(s"Error: Input file must have a .pdf extension: $inputFilePath")
      sys.exit(1)
    }
    println(s"Processing: $inputFilePath -> $outputFilePath")
    if (namesToRedact.nonEmpty) {
      println(s"Targeting specific names for redaction: ${namesToRedact.mkString(", ")}")
    }

    // 3. Validate/create output directory, open output stream and execute the redactor
    val outputFile = new File(outputFilePath)
    val parentDir = outputFile.getAbsoluteFile.getParentFile
    if (parentDir != null && !parentDir.exists()) {
      if (!parentDir.mkdirs()) {
        System.err.println(s"Error: Could not create output directory: ${parentDir.getAbsolutePath}")
        sys.exit(1)
      }
    }

    // Using.resource ensures the FileOutputStream is safely closed even if an error occurs
    Using(new FileOutputStream(outputFile)) { outputStream =>
      
      PdfRedactor.redact(inputFile, outputStream, namesToRedact)
      
    }.fold(
      exception => {
        System.err.println(s"Failed to process PDF: ${exception.getMessage}")
        exception.printStackTrace()
        sys.exit(1)
      },
      _ => println(s"Success! Redacted file saved.")
    )
  }
}