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

package uk.gov.hmrc.apidefinition.service

import uk.gov.hmrc.apidefinition.models.APIStatus.APIStatus
import org.scalatest.BeforeAndAfterEach
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.apidefinition.models._
import uk.gov.hmrc.apidefinition.repository.APIDefinitionRepository
import uk.gov.hmrc.apidefinition.services.MigrationService
import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.apidefinition.utils.APIDefinitionMapper

import scala.concurrent.ExecutionContext.Implicits.global

class MigrationServiceSpec extends UnitSpec
  with MongoSpecSupport with WithFakeApplication with BeforeAndAfterEach {

  private def version(version: String, status: APIStatus) = {
    APIVersion(version, status, None,
      Seq(Endpoint("/today", "Get Today's Date", HttpMethod.GET, AuthType.NONE, ResourceThrottlingTier.UNLIMITED)),
      Some(true)
    )
  }

  private def definition(serviceName: String, context: String, versions: Seq[APIVersion]) = {
    APIDefinition(serviceName, "http://base.url", "API name", "API description", context, versions, None)
  }

  val publishedApi = definition("published", "published", Seq(version("1.0", APIStatus.PUBLISHED)))
  val prototypedApi = definition("prototyped", "prototyped", Seq(version("1.0", APIStatus.PROTOTYPED)))
  val betaApi = definition("beta", "beta", Seq(version("1.0", APIStatus.BETA)))

  val reactiveMongoComponent = new ReactiveMongoComponent { override def mongoConnector: MongoConnector = mongoConnectorForTest }
  val repository = new APIDefinitionRepository(reactiveMongoComponent)
  val apiDefinitionMapper = fakeApplication.injector.instanceOf[APIDefinitionMapper]

  override def beforeEach(): Unit = {
    await(repository.drop)
    super.beforeEach()
  }

  override def afterAll(): Unit = {
    await(repository.drop)
    super.afterAll()
  }

  private trait Setup {
    val underTest = new MigrationService(repository, apiDefinitionMapper)
  }

  "MigrationService" should {
    "migrate legacy statuses and re-save the changed API definitions" in new Setup {
      await(repository.save(publishedApi))
      await(repository.save(prototypedApi))
      await(repository.save(betaApi))

      await(underTest.migrate())

      val migratedPublishedApi = await(repository.fetch(publishedApi.serviceName))
      migratedPublishedApi.get.versions(0).status shouldBe APIStatus.STABLE

      val migratedPrototypedApi = await(repository.fetch(prototypedApi.serviceName))
      migratedPrototypedApi.get.versions(0).status shouldBe APIStatus.BETA
    }
  }
}
