/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.apidefinition.config

import javax.inject.{Inject, Singleton}

import play.api.Configuration

@Singleton
class AppContext @Inject()(val configuration: Configuration) {
  lazy val buildProductionUrlForPrototypedAPIs: Boolean = configuration.getBoolean("buildProductionUrlForPrototypedAPIs").getOrElse(false)
  lazy val isSandbox = configuration.getBoolean("isSandbox").getOrElse(false)
}
