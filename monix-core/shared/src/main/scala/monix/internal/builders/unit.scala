/*
 * Copyright (c) 2014-2016 by its authors. Some rights reserved.
 * See the project homepage at: https://monix.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package monix.internal.builders

import monix.internal._
import monix.internal.concurrent.NextThenCompleteRunnable
import monix.Observable
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal


private[monix] object unit {
  /**
    * Implementation for [[Observable.now]].
    */
  def now[A](elem: A): Observable[A] =
    Observable.unsafeCreate { s =>
      s.onNext(elem)
        .onContinueSignalComplete(s)(s.scheduler)
    }

  /**
    * Implementation for [[Observable.eval]].
    */
  def eval[A](t: => A): Observable[A] =
    Observable.unsafeCreate { subscriber =>
      import subscriber.{scheduler => s}

      s.execute(new Runnable {
        override def run(): Unit = {
          try {
            subscriber.onNext(t)
              .onContinueSignalComplete(subscriber)
          }
          catch {
            case NonFatal(ex) =>
              try subscriber.onError(ex) catch {
                case NonFatal(err) =>
                  s.reportFailure(ex)
                  s.reportFailure(err)
              }
          }
        }
      })
    }

  /**
    * Implementation for [[Observable.unitDelayed]].
    */
  def oneDelayed[A](delay: FiniteDuration, elem: A): Observable[A] =
    Observable.unsafeCreate { s =>
      s.scheduler.scheduleOnce(delay.length, delay.unit,
        NextThenCompleteRunnable(s, elem))
    }

  /**
    * Implementation for [[Observable.empty]].
    */
  def empty: Observable[Nothing] =
    Observable.unsafeCreate(_.onComplete())

  /**
    * Implementation for [[Observable.failed]].
    */
  def error(ex: Throwable): Observable[Nothing] =
    Observable.unsafeCreate(_.onError(ex))

  /**
    * Implementation for [[Observable.never]].
    */
  def never: Observable[Nothing] =
    Observable.unsafeCreate { _ => () }
}