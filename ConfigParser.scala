package parseTest

import parseTest.ConfigParser.{Result, SwitchConfig}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt


object ConfigParser {
  def main(args: Array[String]): Unit = {

    val configStr: String = "switchA.enabled = true\nswitchA.depList = [1, 2, 3]\nswitchA.metaInfo.owner = \"userA\"\n" +
      "switchA.metaInfo.comment = \"hello world\"\n    \nswitchB.enabled = false\nswitchB.depList = [3, 4, 5]\n" +
      "switchB.metaInfo.owner = \"userB\"\n\nswitchB.metaInfo.comment = \"hello world\""

    val parser = new ConfigParser() // 你要实现的类

    // 如果失败，则 error 为第一个错误信息，data 为 None
    // 否则，error 为 None，data 为解析后的数据。
    val result: Result = parser.parse(configStr)
    println(s"总体解析结果如下： $result")

    val configString: String = stringify(result.data.get)
    println(s"将结果转换成配置文件后如下： $configString")

    parser.parseLine("switchA.enabled = true")
    parser.parseLine("switchA.depList = [1, 2, 3]")
    parser.parseLine("switchA.metaInfo.owner = \"userA\"")
    parser.parseLine("switchA.metaInfo.comment = \"hello world\"")
    val resultLine: Result = parser.getResult
    println(s"按行解析的结果如下： $resultLine")

    // 读取很多个配置项
    val configStrList: List[String] = List("switchA.enabled = true\nswitchA.depList = [1, 2, 3]\nswitchA.metaInfo.owner = \"userA\"\n" +
      "switchA.metaInfo.comment = \"hello world\"\n", "switchB.enabled = false\nswitchB.depList = [3, 4, 5]\n" +
      "switchB.metaInfo.owner = \"userB\"\n\nswitchB.metaInfo.comment = \"hello world\"") // 从某处读取多份配置内容

    // 一次性提交给解析器，解析器可以并行处理，但是结果需要保证顺序
    val resultList: List[Result] = parser.parseAll(configStrList)
    println(s"并行处理结果如下： $resultList")
  }

  // 逆向操作，将解析后的结果转换成配置文件的格式
  private def stringify(configMap: Map[String, SwitchConfig]): String = {
    configMap.map { case(key, switchConfig) =>
      val basicConfig = s"${key}.enabled = ${switchConfig.enabled}\n${key}.depList = [${switchConfig.depList.mkString(",")}]\n"
      val metaInfoConfig = switchConfig.metaInfo.map { case(info, value) =>
        s"${key}.metaInfo.${info} = ${value}"
      }.mkString("\n")
      s"$basicConfig$metaInfoConfig"
    }.mkString("\n")
  }

  case class SwitchConfig(
                           name: String, // 开关的名称
                           depList: List[Int] = Nil, // 依赖的模块 id 列表. 如果为空，需要报错。
                           metaInfo: Map[String, String] = Map.empty, // 一些元数据。默认为空 Map
                           enabled: Boolean = true // 是否激活. 默认为 true
                         )

  case class Result(
                     data: Option[Map[String, SwitchConfig]], // 解析后的数据
                     error: Option[String] // 第一个错误的信息
                   )
}

class ConfigParser {
  // 需要解析的配置内容文本
  private val keyValPattern: Regex = "([0-9a-zA-Z-#()_.]+[ ]*)=([ ]*[0-9a-zA-Z-#(), \\[\\]\"]+)".r
  private val switchNamePattern: Regex = "^[a-zA-Z][a-zA-Z0-9_]*[a-zA-Z0-9]$".r
  private val depListPattern: Regex = "^\\[[0-9, ]*]$".r
  private val metaInfoPattern: Regex = "[0-9a-zA-Z-_]+".r

  // 检测开关名字是否合规
  private def isValidName(input: String): Boolean = {
    switchNamePattern.pattern.matcher(input).matches()
  }
  // 检测depList是否合规
  private def isValidList(input: String): Boolean = {
    depListPattern.pattern.matcher(input).matches()
  }
  // 检测MetaInfo的字段是否合规
  private def isValidMetaInfo(input: String): Boolean = {
    metaInfoPattern.pattern.matcher(input).matches()
  }
  // 将depList从String解析成List
  private def parseList(input: String): List[Int] = {
    input.stripPrefix("[").stripSuffix("]").split(',').map(_.trim.toInt).toList
  }
  // 最后判断开关的depList字段是否为空，若为空则报错
  private def isDepListEmpty(configMap: mutable.Map[String, SwitchConfig]): Result = {
    for ((key, value) <- configMap){
      if (value.depList.isEmpty) {
        return Result(None, Some(s"Error: ${key}.depList为空！ It's Invalid !"))
      }
    }
    Result(None, None)
  }

  // 解析其中一行的配置项内容，检测是否合规，是则更新configMap，否则返回错误信息
  private def updateMap(configMap: mutable.Map[String, SwitchConfig], configList: List[String],
                        initialConfig: SwitchConfig, configValue: String, switchName: String): Option[String] = {
    val updateConfig = configList match {
      case List(_, "metaInfo", key) if (isValidMetaInfo(key)) => initialConfig.copy(metaInfo = initialConfig.metaInfo + (key -> configValue))
      case List(_, "enabled") => configValue match {
        case "true" => ()
        case "false" => ()
        case _ => return Some(s"Error: ${switchName}.enabled 只能是true或者false")
      }
        initialConfig.copy(enabled = (configValue == "true"))
      case List(_, "depList") => if (!isValidList(configValue)) {
        return Some(s"Error: ${switchName}.depList is invalid! --depList 只能是方括号包裹的数字列表 ")
      }
        initialConfig.copy(depList = parseList(configValue))
      case _ => return Some(s"Error: ${configList.mkString(".")} is Invalid ! " +
        s"配置项的属性名称只能是 enabled、depList 以及 metaInfo.xyz（xyz代表任意由字母、数字、横杠和下划线组成的字符串）")
    }
    configMap += (switchName -> updateConfig)
    None
  }

  // 输入单行正则匹配得到的内容，调用updateMap，主要为了代码可重用
  private def matchInfoParse(matchInfo: Regex.Match, configMap: mutable.Map[String, SwitchConfig]): Result = {
    val switchName = matchInfo.group(1).trim.split('.')(0)
    // 提取等号右边的值
    val configValue = matchInfo.group(2).trim

    if (!isValidName(switchName)) {
      return Result(None, Some(s"Error: switchName ${switchName} is invalid! --开关的名称由字母、下划线和数字组成，且必须以字母开头，不允许以下划线结尾。"))
    }

    configMap.get(switchName) match {
      case Some(initialConfig) =>
        updateMap(configMap = configMap, configList = matchInfo.group(1).trim.split('.').toList,
          initialConfig = initialConfig, configValue = configValue, switchName = switchName) match {
          case Some(mistakeInfo) =>  Result(None, Some(mistakeInfo))
          case None =>  Result(None, None)
        }

      case None =>
        val initialConfig = SwitchConfig(name = switchName)
        configMap += (switchName -> initialConfig)
        updateMap(configMap = configMap, configList = matchInfo.group(1).trim.split('.').toList,
          initialConfig = initialConfig, configValue = configValue, switchName = switchName) match {
          case Some(mistakeInfo) =>  Result(None, Some(mistakeInfo))
          case None =>  Result(None, None)
        }
    }
  }

  // 总体解析过程
  def parse(configStr: String): Result = {
    // 定义一个可变的Map便于更新，并在最终返回前转化为Map
    val configMap = mutable.Map[String, SwitchConfig]()
    val allMatches = keyValPattern.findAllMatchIn(configStr)

    for (patternMatch <- allMatches) {
      val lineResult = matchInfoParse(matchInfo = patternMatch, configMap = configMap)
      // 匹配单行解析结果，如果有错误信息则直接return
      lineResult.error match {
        case Some(_) => return lineResult
        case None => ()
      }
    }

    isDepListEmpty(configMap = configMap).error match {
      case Some(x) => return Result(None, Some(x))
      case None => ()
    }

    val result: Result = Result(Some(configMap.toMap), None)
    result
  }

  private val configLineMap = mutable.Map[String, SwitchConfig]()

  // 解析一行，并在configLineMap中记录好状态
  def parseLine(configLine: String): Unit = {
    val lineMatch = keyValPattern.findFirstMatchIn(configLine)
    lineMatch match {
      case Some(configInfo) =>
        val lineResult = matchInfoParse(matchInfo = configInfo, configMap = configLineMap)
        // 匹配单行解析结果，如果有错误信息则直接打印
        lineResult.error match {
          case Some(x) => println(x)
          case None => ()
        }
    }
  }

  // 获得解析结果
  def getResult: Result = {
    isDepListEmpty(configMap = configLineMap).error match {
      case Some(x) => return Result(None, Some(x))
      case None => ()
    }
    val result: Result = Result(Some(configLineMap.toMap), None)
    result
  }

  // 并行解析多份配置文本，并返回结果列表
  def parseAll(configStrList: List[String]): List[Result] = {
    // 使用一个可变的ListBuffer来存储每个线程执行得到的结果，返回时转化为List
    val resultListBuffer: ListBuffer[Result] = ListBuffer.empty
    val futures: List[Future[Result]] = configStrList.map { str =>
      Future {
        val result = parse(str)
        result
      }
    }

    // 用synchronized加锁，防止出现线程安全问题
    futures.foreach(_.onComplete{
      case Success(value) => synchronized{
        resultListBuffer += value
      }
      case Failure(exception) => println(s"Processing failed with error: ${exception.getMessage}")
    })

    Await.result(Future.sequence(futures), 10.seconds)
    println("All futures completed")
    resultListBuffer.toList
  }
}