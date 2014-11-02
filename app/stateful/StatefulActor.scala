package stateful

trait StatefulActor[A] {

  type Mutation = PartialFunction[Tuple2[A, Any], A]

  val initial: A

  private var state: A = initial

  val mutation: Mutation

  def get: A = state

  def mutate (b: Any) = state = mutation(state, b)
}
