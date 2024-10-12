package com.rockthejvm.part3concurrency

import zio.{UIO, ZIO, ZIOAppDefault}
import com.rockthejvm.utils.*


object BlockingEffect extends ZIOAppDefault{

  def blockingTask(n: Int): UIO[Int] = {
    ZIO.succeed(s"runing blocking task $n").debugThread *>
      ZIO.succeed(Thread.sleep(10000)) *>
      blockingTask(n)
  }

  val program = ZIO.foreachPar((1 to 100).toList)(blockingTask)
  // thread starvation

  // blocking thread pool
  val aBlockingPro = ZIO.attemptBlocking{
    println(s"${Thread.currentThread().getName} running a long computation")
    Thread.sleep(10000)
    42
  }
  def run = aBlockingPro





}
