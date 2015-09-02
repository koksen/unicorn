/*******************************************************************************
 * (C) Copyright 2015 ADP, LLC.
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
 *******************************************************************************/

package com.adp.unicorn.demo

import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

/**
 * @author Haifeng Li
 */
object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("unicorn-demo")

  // create and start our service actor
  val service = system.actorOf(Props[SearchDemoServiceActor], "unicorn-demo-service")

  implicit val timeout = Timeout(5.seconds)

  val conf = ConfigFactory.load()
  val serverPort = conf.getInt("spray.can.server.port")

  // start a new HTTP server on port 3801 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = serverPort)
}