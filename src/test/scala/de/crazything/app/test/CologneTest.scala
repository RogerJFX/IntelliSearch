package de.crazything.app.test

import org.apache.commons.codec.language.ColognePhonetic
import org.scalatest.FlatSpec

/**
  * Just to make sure, CologneTest works properly - and to learn how.
  */
class CologneTest extends FlatSpec {

  val phoneticEncoder = new ColognePhonetic

  "Cologne phonetic" should "do Müller-Lüdenscheidt" in {
    val res = phoneticEncoder.colognePhonetic("Müller-Lüdenscheidt")
    assert(res == "65752682")
  }

  it should "do even Muller-Ludenscheid" in {
    val res = phoneticEncoder.colognePhonetic("Muller-Ludenscheid")
    assert(res == "65752682")
  }

  it should "do Reißer" in {
    val res = phoneticEncoder.colognePhonetic("Reißer")
    assert(res == "787")
  }

  it should "do even Raisr" in {
    val res = phoneticEncoder.colognePhonetic("Raisr")
    assert(res == "787")
  }

  it should "do even Reeseer" in {
    val res = phoneticEncoder.colognePhonetic("Reeseer")
    assert(res == "787")
  }

  it should "do different Raiter" in {
    val res = phoneticEncoder.colognePhonetic("Raiter")
    assert(res == "727")
  }

  it should "do different Raißa" in {
    val res = phoneticEncoder.colognePhonetic("Raißa")
    assert(res == "78")
  }

  it should "do Philosoph" in {
    val res = phoneticEncoder.colognePhonetic("Philosoph")
    assert(res == "3583")
  }

  it should "do Filosof" in {
    val res = phoneticEncoder.colognePhonetic("Filosof")
    assert(res == "3583")
  }

}
