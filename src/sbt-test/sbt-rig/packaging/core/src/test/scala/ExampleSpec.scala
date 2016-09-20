package foo

import org.scalatest._

class ExampleSpec extends FlatSpec with Matchers {
  it should "check stuff" in {
    val x = new CommonStuff {

    }

    x.foo should equal ("bar")
  }
}
