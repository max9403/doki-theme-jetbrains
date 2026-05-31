import groovy.util.Node
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

fun writeXmlToFile(pluginXml: Path, parsedPluginXml: Node) {
  val sw = StringWriter()
  val writer = PrintWriter(sw)
  printNode(writer, parsedPluginXml, 0)
  writer.flush()
  Files.writeString(pluginXml, sw.toString())
}

private fun escapeXml(text: String): String = text
  .replace("&", "&amp;")
  .replace("<", "&lt;")
  .replace(">", "&gt;")
  .replace("\"", "&quot;")
  .replace("'", "&apos;")

private fun printNode(writer: PrintWriter, node: Node, indent: Int) {
  val pad = "  ".repeat(indent)
  val name = node.name()
  val attributes = node.attributes()

  if (attributes.isNotEmpty()) {
    val attrStr = attributes.map { "${it.key}=\"${escapeXml(it.value.toString())}\"" }.joinToString(" ")
    writer.println("${pad}<${name} ${attrStr}>")
  } else {
    writer.println("${pad}<${name}>")
  }

  val children = node.children()
  for (child in children) {
    when (child) {
      is Node -> {
        if (child.name() == "#text") {
          writer.print("${"  ".repeat(indent + 1)}${escapeXml(child.value().toString())}")
        } else {
          printNode(writer, child, indent + 1)
        }
      }
      else -> writer.print("${"  ".repeat(indent + 1)}${escapeXml(child.toString())}")
    }
  }

  writer.println("${pad}</${name}>")
}


fun parseXml(pluginXml: Path): Node {
  val content = Files.readString(pluginXml)
    .replace(Regex("<#text>(.*?)</#text>", RegexOption.DOT_MATCHES_ALL), "$1")

  val parsedPlugin = content.byteInputStream().use { input ->
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = true
    factory.isValidating = false
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)

    val builder = factory.newDocumentBuilder()
    builder.setErrorHandler(object : ErrorHandler {
      override fun warning(exception: SAXParseException?) {}
      override fun error(exception: SAXParseException?) {}
      override fun fatalError(exception: SAXParseException) { throw exception }
    })

    val doc = builder.parse(InputSource(InputStreamReader(input, "UTF-8")))
    domToNode(doc.documentElement)
  }
  return parsedPlugin
}

private fun domToNode(element: org.w3c.dom.Element): Node {
  val attrs = mutableMapOf<String, Any>()
  val nl = element.attributes
  for (i in 0 until nl.length) {
    attrs[nl.item(i).nodeName] = nl.item(i).nodeValue
  }
  val node = Node(null, element.nodeName, attrs)

  val children = element.childNodes
  for (i in 0 until children.length) {
    val child = children.item(i)
    when (child.nodeType) {
      org.w3c.dom.Node.ELEMENT_NODE -> {
        node.append(domToNode(child as org.w3c.dom.Element))
      }
      org.w3c.dom.Node.TEXT_NODE -> {
        val text = child.textContent?.trim()
        if (!text.isNullOrEmpty()) {
          node.append(Node(null, "#text", text))
        }
      }
    }
  }

  return node
}
