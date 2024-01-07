package com.rockthejvm.part3concurrency

import zio.{ZIO, ZIOAppDefault}
import com.rockthejvm.utils.*
import zio._

object Interruptions extends ZIOAppDefault{

  val zioWithTime =
    (
    ZIO.succeed("starting computation").debugThread *>
    ZIO.sleep(2.seconds) *>
    ZIO.succeed(42).debugThread
    )
      .onInterrupt(ZIO.succeed("I was interrupted").debugThread)

  val interruption = for{
    fib <- zioWithTime.fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("Interrupting").debugThread *> fib.interrupt /* is an effect*/
    _ <- ZIO.succeed("Interruption successful").debugThread
    result <- fib.join
  } yield result

  val interruption_v2 = for {
    fib <- zioWithTime.fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("Interrupting").debugThread *> fib.interruptFork /* is an effect*/
    _ <- ZIO.succeed("Interruption successful").debugThread
    result <- fib.join
  } yield result

  /**
   *
   * Automatic Interruption
   */
  // outliving a parent fiber

  val parentEffect = ZIO.succeed("spawaning fiber").debugThread *>
//    zioWithTime.fork *>// child fiber
    zioWithTime.forkDaemon *>  // this fiber will now be a child for MAIN fiber
    ZIO.sleep(1.second) *>
    ZIO.succeed("parent successful").debugThread // done here

  val testOutlivingFiber = for{
    parentEffectFibers <- parentEffect.fork
    _ <- ZIO.sleep(3.seconds)
    _ <- parentEffectFibers.join
  } yield()
  // child fiber is automatically completed if the parent fiber is completed


  // racing
  val slowEffect = (ZIO.sleep(2.seconds) *> ZIO.succeed("slow").debugThread).onInterrupt(ZIO.succeed("[slow] interrupted").debugThread)
  val fastEffect = (ZIO.sleep(1.second) *> ZIO.succeed("fast").debugThread).onInterrupt(ZIO.succeed("[fast] interrupted").debugThread)
  val aRace = slowEffect.race(fastEffect)
  val testRace = aRace.fork *> ZIO.sleep(3.seconds)

  def run = testRace

}
