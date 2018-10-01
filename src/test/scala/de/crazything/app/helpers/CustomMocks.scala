package de.crazything.app.helpers

import org.scalatest.Assertion

import scala.concurrent.{ExecutionContext, Future}

object CustomMocks {

  def mockObjectFieldAsync[R <: Future[Assertion]](className: String, fieldName: String, value: Any, assertion: => R)
                                                  (implicit ex: ExecutionContext): Future[Assertion] = {
    import scala.reflect.runtime.{universe => ru}
    val mirror = ru.runtimeMirror(getClass.getClassLoader)

    val moduleSymbol = mirror.staticModule(className)
    val moduleMirror = mirror.reflectModule(moduleSymbol)
    val instanceMirror = mirror.reflect(moduleMirror.instance)
    val fields: Iterable[ru.Symbol] = moduleSymbol.typeSignature.decls.filter(d => {
      d.toString == s"value $fieldName" && d.asTerm.isVal
    })
    if(fields.size != 1) {
      throw new RuntimeException(s"Found vals more or less than 1. We found: ${fields.size}")
    }
    val field = fields.head
    val fieldMirror = instanceMirror.reflectField(field.asTerm)
    val originalValue = fieldMirror.get
    fieldMirror.set(value)
    val fut = assertion
    fut.andThen {
      case _ => fieldMirror.set(originalValue)
    }
    fut
  }

  def interceptTestAsync[R <: Future[Assertion]](before: () => _, after: () => _, assertion: => R)
                                                (implicit ex: ExecutionContext): Future[Assertion] = {
    before()
    val fut = assertion
    fut.andThen {
      case _ => after()
    }
    fut

  }
}
