package wvlet.airframe.http

import _root_.akka.http.scaladsl.model.HttpRequest
import _root_.akka.http.scaladsl.model.HttpResponse
import _root_.akka.actor.ActorSystem
import wvlet.airframe.http

import scala.concurrent.Await
import scala.concurrent.duration._

package object akka {
  val to = 10 seconds

  implicit class AkkaHttpRequestWrapper(val raw: HttpRequest)(implicit actorSystem: ActorSystem)
      extends wvlet.airframe.http.HttpRequest[HttpRequest] {
    override protected def adapter: HttpRequestAdapter[HttpRequest] = new AkkaHttpRequestAdapter
    override def toRaw: HttpRequest                                 = raw
  }

  class AkkaHttpRequestAdapter(implicit actorSystem: ActorSystem) extends HttpRequestAdapter[HttpRequest] {
    override def methodOf(request: HttpRequest): String = request.method.value
    override def pathOf(request: HttpRequest): String   = request.uri.path.toString()
    override def headerOf(request: HttpRequest): HttpMultiMap = {
      val builder = HttpMultiMap.newBuilder
      request.headers.foreach { header =>
        builder.+=(header.name, header.value())
      }
      builder.result()
    }
    override def queryOf(request: HttpRequest): HttpMultiMap = {
      val builder = HttpMultiMap.newBuilder
      request.uri.query().foreach {
        case (k, v) =>
          builder += (k, v)
      }
      builder.result()
    }
    override def messageOf(request: HttpRequest): HttpMessage.Message = {
      val entity    = Await.result(request.entity.toStrict(to), Duration.Inf)
      val byteArray = entity.getData().asByteBuffer.array()
      HttpMessage.byteArrayMessage(byteArray)
    }
    override def contentTypeOf(request: HttpRequest): Option[String] = {
      val entity = Await.result(request.entity.toStrict(to), Duration.Inf)
      Some(entity.contentType.toString())
    }
    override def requestType: Class[HttpRequest]                           = classOf[HttpRequest]
    override def uriOf(request: HttpRequest): String                       = request.uri.toString()
    override def wrap(request: HttpRequest): http.HttpRequest[HttpRequest] = new AkkaHttpRequestWrapper(request)
  }

  implicit class AkkaHttpResponseWrapper(val raw: HttpResponse)(implicit actorSystem: ActorSystem)
      extends wvlet.airframe.http.HttpResponse[HttpResponse] {
    override protected def adapter: HttpResponseAdapter[HttpResponse] = new AkkaHttpResponseAdapter
    override def toRaw: HttpResponse                                  = raw
  }

  implicit class AkkaHttpResponseAdapter(implicit actorSystem: ActorSystem) extends HttpResponseAdapter[HttpResponse] {
    override def statusCodeOf(resp: HttpResponse): Int = resp.status.intValue()
    override def messageOf(resp: HttpResponse): HttpMessage.Message = {
      val entity    = Await.result(resp.entity.toStrict(to), Duration.Inf)
      val byteArray = entity.data.asByteBuffer.array()
      HttpMessage.byteArrayMessage(byteArray)
    }
    override def contentTypeOf(resp: HttpResponse): Option[String] = {
      val entity = Await.result(resp.entity.toStrict(to), Duration.Inf)
      Some(entity.contentType.toString())
    }
    override def headerOf(resp: HttpResponse): HttpMultiMap = {
      val builder = HttpMultiMap.newBuilder
      resp.headers.foreach { header =>
        builder.+=(header.name, header.value())
      }
      builder.result()
    }
    override def wrap(resp: HttpResponse): http.HttpResponse[HttpResponse] = new AkkaHttpResponseWrapper(resp)
  }
}
