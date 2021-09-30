/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package computerdatabase.advanced

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class TestTomcatLarge extends Simulation {
 
  val httpConf = http
    .baseURL("http://192.168.1.190:8080")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")
//  .localAddresses("192.168.1.117", "192.168.1.118","192.168.1.119","192.168.1.120","192.168.1.121","192.168.1.122","192.168.1.123","192.168.1.124","192.168.1.125","192.168.1.126","192.168.1.127","192.168.1.128","192.168.1.129","192.168.1.130","192.168.1.131","192.168.1.132","192.168.1.133","192.168.1.134","192.168.1.135","192.168.1.136","192.168.1.137","192.168.1.138","192.168.1.139","192.168.1.140","192.168.1.141","192.168.1.142","192.168.1.143","192.168.1.144","192.168.1.145","192.168.1.146","192.168.1.147","192.168.1.148","192.168.1.149","192.168.1.150","192.168.1.151","192.168.1.152","192.168.1.153","192.168.1.154","192.168.1.155","192.168.1.156","192.168.1.157","192.168.1.158","192.168.1.159")

//  .localAddresses("192.168.1.117", "192.168.1.118","192.168.1.119","192.168.1.120","192.168.1.121","192.168.1.122","192.168.1.123","192.168.1.124","192.168.1.125","192.168.1.126","192.168.1.127","192.168.1.128","192.168.1.129","192.168.1.130","192.168.1.131","192.168.1.132","192.168.1.133","192.168.1.134","192.168.1.135","192.168.1.136","192.168.1.137","192.168.1.138","192.168.1.139","192.168.1.140","192.168.1.141","192.168.1.142","192.168.1.143","192.168.1.144","192.168.1.145","192.168.1.146","192.168.1.147","192.168.1.148","192.168.1.149","192.168.1.150","192.168.1.151","192.168.1.152","192.168.1.153","192.168.1.154","192.168.1.155","192.168.1.156","192.168.1.157","192.168.1.158","192.168.1.159","192.168.1.160","192.168.1.161","192.168.1.162","192.168.1.163","192.168.1.164","192.168.1.165","192.168.1.166","192.168.1.167","192.168.1.168","192.168.1.169","192.168.1.170","192.168.1.171","192.168.1.172","192.168.1.173","192.168.1.174","192.168.1.175","192.168.1.176","192.168.1.177","192.168.1.178","192.168.1.179","192.168.1.180","192.168.1.181","192.168.1.182","192.168.1.183","192.168.1.184","192.168.1.185","192.168.1.186","192.168.1.187","192.168.1.188","192.168.1.189","192.168.1.193","192.168.1.194","192.168.1.195","192.168.1.196","192.168.1.197","192.168.1.198","192.168.1.199","192.168.1.200","192.168.1.201","192.168.1.202","192.168.1.203","192.168.1.204","192.168.1.205","192.168.1.206","192.168.1.207","192.168.1.208","192.168.1.209","192.168.1.210","192.168.1.211","192.168.1.212","192.168.1.213","192.168.1.214","192.168.1.215","192.168.1.216","192.168.1.217","192.168.1.218","192.168.1.219","192.168.1.220","192.168.1.221","192.168.1.222","192.168.1.223","192.168.1.224","192.168.1.225","192.168.1.226","192.168.1.227","192.168.1.228","192.168.1.229","192.168.1.230","192.168.1.231","192.168.1.232","192.168.1.233","192.168.1.234","192.168.1.235","192.168.1.236","192.168.1.237","192.168.1.238","192.168.1.239")
 .localAddresses("192.168.1.117","192.168.1.2","192.168.1.2","192.168.1.3","192.168.1.4","192.168.1.5","192.168.1.6","192.168.1.7","192.168.1.8","192.168.1.9","192.168.1.10","192.168.1.11","192.168.1.12","192.168.1.13","192.168.1.14","192.168.1.15","192.168.1.16","192.168.1.17","192.168.1.18","192.168.1.19","192.168.1.20","192.168.1.21","192.168.1.22","192.168.1.23","192.168.1.24","192.168.1.25","192.168.1.26","192.168.1.27","192.168.1.28","192.168.1.29","192.168.1.30","192.168.1.31","192.168.1.32","192.168.1.33","192.168.1.34","192.168.1.35","192.168.1.36","192.168.1.37","192.168.1.38","192.168.1.39","192.168.1.40","192.168.1.41","192.168.1.42","192.168.1.43","192.168.1.44","192.168.1.45","192.168.1.46","192.168.1.47","192.168.1.48","192.168.1.49","192.168.1.50","192.168.1.51","192.168.1.52","192.168.1.53","192.168.1.54","192.168.1.55","192.168.1.56","192.168.1.57","192.168.1.58","192.168.1.59","192.168.1.60","192.168.1.61","192.168.1.62","192.168.1.63","192.168.1.64","192.168.1.65","192.168.1.66","192.168.1.67","192.168.1.68","192.168.1.69","192.168.1.70","192.168.1.71","192.168.1.72","192.168.1.73","192.168.1.74","192.168.1.75","192.168.1.76","192.168.1.77","192.168.1.78","192.168.1.79","192.168.1.80","192.168.1.81","192.168.1.82","192.168.1.83","192.168.1.84","192.168.1.85","192.168.1.86","192.168.1.87","192.168.1.88","192.168.1.89","192.168.1.90","192.168.1.91","192.168.1.92","192.168.1.93","192.168.1.94","192.168.1.95","192.168.1.96","192.168.1.97","192.168.1.98","192.168.1.99","192.168.1.100","192.168.1.101","192.168.1.102","192.168.1.103","192.168.1.104","192.168.1.105","192.168.1.106","192.168.1.107","192.168.1.108","192.168.1.109","192.168.1.110","192.168.1.111","192.168.1.112","192.168.1.113","192.168.1.114","192.168.1.115","192.168.1.116","192.168.1.118","192.168.1.119","192.168.1.120","192.168.1.121","192.168.1.122","192.168.1.123","192.168.1.124","192.168.1.125","192.168.1.126","192.168.1.127","192.168.1.128","192.168.1.129","192.168.1.130","192.168.1.131","192.168.1.132","192.168.1.133","192.168.1.134","192.168.1.135","192.168.1.136","192.168.1.137","192.168.1.138","192.168.1.139","192.168.1.140","192.168.1.141","192.168.1.142","192.168.1.143","192.168.1.144","192.168.1.145","192.168.1.146","192.168.1.147","192.168.1.148","192.168.1.149","192.168.1.150","192.168.1.151","192.168.1.152","192.168.1.153","192.168.1.154","192.168.1.155","192.168.1.156","192.168.1.157","192.168.1.158","192.168.1.159","192.168.1.160","192.168.1.161","192.168.1.162","192.168.1.163","192.168.1.164","192.168.1.165","192.168.1.166","192.168.1.167","192.168.1.168","192.168.1.169","192.168.1.170","192.168.1.171","192.168.1.172","192.168.1.173","192.168.1.174","192.168.1.175","192.168.1.176","192.168.1.177","192.168.1.178","192.168.1.179","192.168.1.180","192.168.1.181","192.168.1.182","192.168.1.183","192.168.1.184","192.168.1.185","192.168.1.186","192.168.1.187","192.168.1.188","192.168.1.189","192.168.1.193","192.168.1.194","192.168.1.195","192.168.1.196","192.168.1.197","192.168.1.198","192.168.1.199","192.168.1.200","192.168.1.201","192.168.1.202","192.168.1.203","192.168.1.204","192.168.1.205","192.168.1.206","192.168.1.207","192.168.1.208","192.168.1.209","192.168.1.210","192.168.1.211","192.168.1.212","192.168.1.213","192.168.1.214","192.168.1.215","192.168.1.216","192.168.1.217","192.168.1.218","192.168.1.219","192.168.1.220","192.168.1.221","192.168.1.222","192.168.1.223","192.168.1.224","192.168.1.225","192.168.1.226","192.168.1.227","192.168.1.228","192.168.1.229","192.168.1.230","192.168.1.231","192.168.1.232","192.168.1.233","192.168.1.234","192.168.1.235","192.168.1.236","192.168.1.237","192.168.1.238","192.168.1.239","192.168.1.240","192.168.1.241","192.168.1.242","192.168.1.243","192.168.1.244","192.168.1.245","192.168.1.246","192.168.1.247","192.168.1.248","192.168.1.249","192.168.1.250","192.168.1.251","192.168.1.252","192.168.1.253")















  val scn = scenario("TestTomcatLarge")
    .exec(http("Nb Request")

  .get("/ApiRest/rest/api/LargeCpudestroyer?id=100"))
setUp(scn.inject(
            rampUsersPerSec(1) to 10000  during(5 minutes),
            constantUsersPerSec(10000) during(10 minutes)

).protocols(httpConf))
}
