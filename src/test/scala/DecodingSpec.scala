import mx.cinvestav.commons.types.Monitoring.NodeInfo
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import mx.cinvestav.Declarations.Implicits.nodeInfoDecoder

class DecodingSpec extends munit.CatsEffectSuite {
  val info = NodeInfo.empty
  test("Basic"){
    val xJson = info.asJson
    println(xJson.toString())
//    val x¡ xJson.as[NodeInf
  }

}
