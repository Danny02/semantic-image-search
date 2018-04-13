package shared.component

import scalatags.generic
import scalatags.generic.Bundle

class Components[Builder, Output <: FragT, FragT](bundle: Bundle[Builder, Output, FragT]) {
  import bundle.all._

  private type Frag = generic.Frag[Builder, FragT]

  trait Renderer[A] {
    def render(a: A): Frag
  }

  implicit val predRenderer = new Renderer[TagPrediction] {
    override def render(t: TagPrediction): Frag = {
      div(
        h3(f"${t.tag} - ${t.pred * 100}%3.2f %%"),
        div(cls := "photos")(t.images.map(image => img(src := image)).toList)
      )
    }
  }

  implicit def toFrag[A: Renderer](a: A): Frag = implicitly[Renderer[A]].render(a)

  implicit def seqFragable[A: Renderer](s: Seq[A]): Seq[Frag] = s.map(a => a: Frag)

}
