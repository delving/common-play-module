package extensions {

import org.codehaus.jackson.map.annotate.JsonCachable
import org.bson.types.ObjectId
import org.codehaus.jackson.map.{DeserializationContext, JsonDeserializer, SerializerProvider, JsonSerializer}
import org.codehaus.jackson.map.module.SimpleModule
import org.codehaus.jackson.{Version, JsonParser, JsonGenerator}
import play.mvc._
import play.mvc.Http.{Response, Request}
import results.{Result, RenderJson}
import play.data.binding.TypeBinder
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import play.Logger

/**
 * This trait provides additional actions that can be used in controllers
 */
trait Extensions {
  self: Controller =>

  play.data.binding.Binder.register(classOf[org.bson.types.ObjectId], new ObjectIdBinder())

  override def Json(data: AnyRef): RenderJson = new RenderJson() {
    override def apply(request: Request, response: Response) {
      val encoding = getEncoding
      setContentTypeIfNotSet(response, "application/json; charset=" + encoding)
      response.out.write(JJson.generate(data).getBytes(encoding))
    }
  }

  def LoggedError(msg: String): Result = {
    Logger.error(msg)
    Error(msg)
  }

  def LoggedNotFound(msg: String): Result = {
    Logger.error(msg)
    NotFound(msg)
  }
}

object JJson extends com.codahale.jerkson.Json {
  val module = new SimpleModule("JerksonJson", Version.unknownVersion())
  module.addSerializer(classOf[ObjectId], new ObjectIdSerializer)
  module.addDeserializer(classOf[ObjectId], new ObjectIdDeserializer)
  mapper.registerModule(module)
}

@JsonCachable
class ObjectIdSerializer extends JsonSerializer[ObjectId] {
  def serialize(id: ObjectId, json: JsonGenerator, provider: SerializerProvider) {
    json.writeString(id.toString)
  }
}

class ObjectIdDeserializer extends JsonDeserializer[ObjectId] {
  def deserialize(jp: JsonParser, context: DeserializationContext) = {
    if (!ObjectId.isValid(jp.getText)) throw context.mappingException("invalid ObjectId " + jp.getText)
    new ObjectId(jp.getText)
  }
}

/**
 * Binder for Play to know how to read ObjectIds from HTTP
 */
class ObjectIdBinder extends TypeBinder[ObjectId] {
  override def bind(name:String, annotations:Array[Annotation], value:String, actualClass:Class[_], genericType:Type) = {
    value match {
      case null => null
      case id if(ObjectId.isValid(id)) => new ObjectId(id)
      case id if(!ObjectId.isValid(id)) => null
    }
  }
}


}