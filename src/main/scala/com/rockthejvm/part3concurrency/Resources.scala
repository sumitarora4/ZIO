package com.rockthejvm.part3concurrency

import zio.*
import com.rockthejvm.utils.*

import java.util.Scanner

object Resources extends ZIOAppDefault{

  // finalizers
  def unsafeMethod(): Int = throw new RuntimeException("Not an int here for you!")

  val anAttempt = ZIO.attempt(unsafeMethod())

  // finalizers
  val attemptWithFinalizer = anAttempt.ensuring(ZIO.succeed("finalizer!").debugThread)
  // multiple finalizers
  val attemptWith2Finalizers = attemptWithFinalizer.ensuring(ZIO.succeed("another finalizer!").debugThread)
  // .onInterrupt, .onError, .onDone, .onExit

  // resource lifecycle
  class Connection(url: String) {
    def open() = ZIO.succeed(s"opening connection to $url...").debugThread

    def close() = ZIO.succeed(s"closing connection to $url").debugThread
  }

  object Connection {
    def create(url: String) = ZIO.succeed(new Connection(url))
  }

  val fetchUrl = for {
    conn <- Connection.create("rockthejvm.com")
    fib <- (conn.open() *> ZIO.sleep(300.seconds)).fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("interrupting").debugThread *> fib.interrupt
    _ <- fib.join
  } yield () // resource leak

  val correctFetchUrl = for {
    conn <- Connection.create("rockthejvm.com")
    fib <- (conn.open() *> ZIO.sleep(300.seconds)).ensuring(conn.close()).fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("interrupting").debugThread *> fib.interrupt
    _ <- fib.join
  } yield () // preventing leaks

  // tedious

  /**
   * acquireRelease:
   * - acquiring can't be interrupted
   * - all finalizers are guaranteed to run
   */

  val cleanConnection = ZIO.acquireRelease(Connection.create("rockthejvm.com"))(_.close())
  val fetchWithResource = for {
    conn <- cleanConnection
    fib <- (conn.open() *> ZIO.sleep(300.seconds)).fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("interrupting").debugThread *> fib.interrupt
    _ <- fib.join
  } yield ()

  val fetchWithScopedResource = ZIO.scoped(fetchWithResource)  // scoped is needed with acquire release to get the result

  // acquireReleaseWith
  val cleanConnection_v2 = ZIO.acquireReleaseWith(
    Connection.create("rockthejvm.com") // acquire
  )(
    _.close() // release
  )(
    conn => conn.open() *> ZIO.sleep(300.seconds) // use
  )

  val fetchWithResource_v2 = for {
    fib <- cleanConnection_v2.fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("interrupting").debugThread *> fib.interrupt
    _ <- fib.join
  } yield ()


  /**
   * Exercise: 1. Use the acquireRelease to open a file, print all lines, (one every 100 mili seconds), then close the file
   */

  import java.io.{File, FileWriter}

  def openFileScanner(path: String): UIO[Scanner] = ZIO.succeed(new Scanner(new File(path)))

    def acquireOpenFile(path: String):UIO[Unit] = ???

  val testInterruptedFileDisplay = for {
    fib <- openFileScanner("src/main/scala/part3concurrency/Resources.scala")
    _ <- ZIO.sleep(2.seconds)
  } yield()

  def run = fetchWithResource_v2

}
