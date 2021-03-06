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

package uk.gov.hmrc.apidefinition.models

import uk.gov.hmrc.apidefinition.models.JsonFormatters._
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class APIDefinitionSpec extends UnitSpec {

  "APIDefinition" should {

    "fail validation if an empty serviceBaseUrl is provided" in {
      lazy val apiDefinition: APIDefinition = APIDefinition("calendar", "", "Calendar API", "My Calendar API", "calendar", Seq(APIVersion("1.0", APIStatus.PROTOTYPED, Some(PublicAPIAccess()), Seq(Endpoint("/today", "Get Today's Date", HttpMethod.GET, AuthType.NONE, ResourceThrottlingTier.UNLIMITED)))), Some(false))
      assertValidationFailure(apiDefinition, "serviceBaseUrl is required")
    }

    "fail validation if an empty serviceName is provided" in {
      lazy val apiDefinition: APIDefinition = APIDefinition("", "http://calendar", "Calendar API", "My Calendar API", "calendar", Seq(APIVersion("1.0", APIStatus.PROTOTYPED, Some(PublicAPIAccess()), Seq(Endpoint("/today", "Get Today's Date", HttpMethod.GET, AuthType.NONE, ResourceThrottlingTier.UNLIMITED)))), Some(false))
      assertValidationFailure(apiDefinition, "serviceName is required")
    }

    "fail validation if a version number is referenced more than once" in {
      lazy val apiDefinition: APIDefinition = APIDefinition(
        "calendar",
        "http://calendar",
        "Calendar API",
        "My Calendar API",
        "calendar",
        Seq(
          APIVersion("1.0", APIStatus.PROTOTYPED, Some(PublicAPIAccess()), Seq(Endpoint("/today", "Get Today's Date", HttpMethod.GET, AuthType.NONE, ResourceThrottlingTier.UNLIMITED))),
          APIVersion("1.1", APIStatus.PROTOTYPED, Some(PublicAPIAccess()), Seq(Endpoint("/today", "Get Today's Date", HttpMethod.GET, AuthType.NONE, ResourceThrottlingTier.UNLIMITED))),
          APIVersion("1.1", APIStatus.PROTOTYPED, Some(PublicAPIAccess()), Seq(Endpoint("/today", "Get Today's Date", HttpMethod.GET, AuthType.NONE, ResourceThrottlingTier.UNLIMITED))),
          APIVersion("1.2", APIStatus.PROTOTYPED, Some(PublicAPIAccess()), Seq(Endpoint("/today", "Get Today's Date", HttpMethod.GET, AuthType.NONE, ResourceThrottlingTier.UNLIMITED)))),
        Some(false))
      assertValidationFailure(apiDefinition, "version numbers must be unique")
    }

    "fail validation if an empty name is provided" in {
      lazy val apiDefinition: APIDefinition = APIDefinition("calendar", "http://calendar", "", "My Calendar API", "calendar", Seq(APIVersion("1.0", APIStatus.PROTOTYPED, Some(PublicAPIAccess()), Seq(Endpoint("/today", "Get Today's Date", HttpMethod.GET, AuthType.NONE, ResourceThrottlingTier.UNLIMITED)))), Some(false))
      assertValidationFailure(apiDefinition, "name is required")
    }

    "fail validation if an empty context is provided" in {
      lazy val apiDefinition: APIDefinition = APIDefinition("calendar", "http://calendar", "Calendar API", "My Calendar API", "", Seq(APIVersion("1.0", APIStatus.PROTOTYPED, Some(PublicAPIAccess()), Seq(Endpoint("/today", "Get Today's Date", HttpMethod.GET, AuthType.NONE, ResourceThrottlingTier.UNLIMITED)))), Some(false))
      assertValidationFailure(apiDefinition, "context is required")
    }

    "fail validation if an empty description is provided" in {
      lazy val apiDefinition: APIDefinition = APIDefinition("calendar", "http://calendar", "Calendar API", "", "calendar", Seq(APIVersion("1.0", APIStatus.PROTOTYPED, Some(PublicAPIAccess()), Seq(Endpoint("/today", "Get Today's Date", HttpMethod.GET, AuthType.NONE, ResourceThrottlingTier.UNLIMITED)))), Some(false))
      assertValidationFailure(apiDefinition, "description is required")
    }

    "fail validation when no APIVersion is provided" in {
      lazy val apiDefinition: APIDefinition = APIDefinition("calendar", "http://calendar", "Calendar API", "My Calendar API", "calendar", Nil, None)
      assertValidationFailure(apiDefinition, "at least one version is required")
    }

    "fail validation when no Endpoint is provided" in {
      lazy val apiDefinition: APIDefinition = APIDefinition("calendar", "http://calendar", "Calendar API", "My Calendar API", "calendar", Seq(APIVersion("1.0", APIStatus.PROTOTYPED, Some(PublicAPIAccess()), Nil)), Some(false))
      assertValidationFailure(apiDefinition, "at least one endpoint is required")
    }

    val moneyEndpoint = Endpoint(
      uriPattern = "/payments",
      endpointName = "Check Payments",
      method = HttpMethod.GET,
      authType = AuthType.USER,
      throttlingTier = ResourceThrottlingTier.UNLIMITED,
      scope = Some("read:money"))

    val moneyApiVersion = APIVersion(
      version = "1.0",
      status = APIStatus.PROTOTYPED,
      endpoints = Seq(moneyEndpoint))

    lazy val moneyApiDefinition = APIDefinition(
      serviceName = "money",
      serviceBaseUrl = "http://www.money.com",
      name = "Money API",
      description = "API for checking payments",
      context = "money",
      versions = Seq(moneyApiVersion),
      requiresTrust = Some(false))

    val invalidEndpointUriScenarios = Map(
      ':' -> "/payments/:startDate",
      '?' -> "/payments?startDate={startDate}",
      '&' -> "/payments/tom&jerry"
    )

    invalidEndpointUriScenarios.foreach { case (char, endpointUri) =>
      s"fail validation if an Endpoint contains $char in the URI" in {

        lazy val apiDefinition = moneyApiDefinition.copy(
          versions = Seq(moneyApiVersion.copy(endpoints = Seq(moneyEndpoint.copy(uriPattern = endpointUri))))
        )

        assertValidationFailure(apiDefinition, s"endpoint cannot contain $char in the URI")
      }
    }

    val moneyQueryParameter = Parameter("startDate")

    "fail validation when a query parameter name is empty" in {

      lazy val apiDefinition = moneyApiDefinition.copy(
        versions = Seq(moneyApiVersion.copy(endpoints = Seq(moneyEndpoint.copy(queryParameters = Some(Seq(moneyQueryParameter.copy(name = "")))))))
      )

      assertValidationFailure(apiDefinition, "query parameter name cannot be empty")
    }

    val invalidQueryPamameterNameScenarios = Seq('/', '?', ':', '&', '}', '{')

    invalidQueryPamameterNameScenarios.foreach { char =>
      s"fail validation when a query parameter name contains $char in the name" in {

        lazy val apiDefinition = moneyApiDefinition.copy(
          versions = Seq(moneyApiVersion.copy(endpoints = Seq(moneyEndpoint.copy(queryParameters = Some(Seq(moneyQueryParameter.copy(name = s"param$char")))))))
        )

        assertValidationFailure(apiDefinition, s"query parameter name cannot contain $char in the name")
      }
    }

    "fail validation when a scope is provided but auth type is 'application'" in {
      lazy val apiDefinition: APIDefinition = APIDefinition("calendar", "http://calendar", "Calendar API", "My Calendar API", "calendar", Seq(APIVersion("1.0", APIStatus.PROTOTYPED, Some(PublicAPIAccess()), Seq(Endpoint("uriPattern", "endpointName", HttpMethod.GET, AuthType.APPLICATION, ResourceThrottlingTier.UNLIMITED, Some("scope"))))), None)
      assertValidationFailure(apiDefinition, "scope is not required if authType is APPLICATION")
    }

    "read from JSON when the API access type is PUBLIC and there is no whitelist" in {
      val body =
        """{
          |   "serviceName":"calendar",
          |   "name":"Calendar API",
          |   "description":"My Calendar API",
          |   "serviceBaseUrl":"http://calendar",
          |   "context":"calendar",
          |   "versions":[
          |      {
          |         "version":"1.0",
          |         "status":"PUBLISHED",
          |         "access": {
          |           "type": "PUBLIC"
          |         },
          |         "endpoints":[
          |            {
          |               "uriPattern":"/today",
          |               "endpointName":"Get Today's Date",
          |               "method":"GET",
          |               "authType":"NONE",
          |               "throttlingTier":"UNLIMITED"
          |            }
          |         ]
          |      }
          |   ]
          |}""".stripMargin.replaceAll("\n", " ")

      val apiDefinition = Json.parse(body).as[APIDefinition]
      apiDefinition.versions.head.access shouldBe Some(PublicAPIAccess())
    }

    "read from JSON when the API access type is PUBLIC and there is an empty whitelist" in {
      val body =
        """{
          |   "serviceName":"calendar",
          |   "name":"Calendar API",
          |   "description":"My Calendar API",
          |   "serviceBaseUrl":"http://calendar",
          |   "context":"calendar",
          |   "versions":[
          |      {
          |         "version":"1.0",
          |         "status":"PUBLISHED",
          |         "access": {
          |           "type": "PUBLIC",
          |           "whitelistedApplicationIds" : []
          |         },
          |         "endpoints":[
          |            {
          |               "uriPattern":"/today",
          |               "endpointName":"Get Today's Date",
          |               "method":"GET",
          |               "authType":"NONE",
          |               "throttlingTier":"UNLIMITED"
          |            }
          |         ]
          |      }
          |   ]
          |}""".stripMargin.replaceAll("\n", " ")

      val apiDefinition = Json.parse(body).as[APIDefinition]
      apiDefinition.versions.head.access shouldBe Some(PublicAPIAccess())
    }

    "fail to read from JSON when the API access type is PUBLIC and there is a non-empty whitelist" in {
      val body =
        """{
          |   "serviceName":"calendar",
          |   "name":"Calendar API",
          |   "description":"My Calendar API",
          |   "serviceBaseUrl":"http://calendar",
          |   "context":"calendar",
          |   "versions":[
          |      {
          |         "version":"1.0",
          |         "status":"PUBLISHED",
          |         "access": {
          |           "type": "PUBLIC",
          |           "whitelistedApplicationIds" : [ "an-application-id" ]
          |         },
          |         "endpoints":[
          |            {
          |               "uriPattern":"/today",
          |               "endpointName":"Get Today's Date",
          |               "method":"GET",
          |               "authType":"NONE",
          |               "throttlingTier":"UNLIMITED"
          |            }
          |         ]
          |      }
          |   ]
          |}""".stripMargin.replaceAll("\n", " ")

      intercept[RuntimeException] { Json.parse(body).as[APIDefinition] }
    }

    "read from JSON when the API access type is PRIVATE and there is an empty whitelist" in {
      val body =
        """{
          |   "serviceName":"calendar",
          |   "name":"Calendar API",
          |   "description":"My Calendar API",
          |   "serviceBaseUrl":"http://calendar",
          |   "context":"calendar",
          |   "versions":[
          |      {
          |         "version":"1.0",
          |         "status":"PUBLISHED",
          |         "access": {
          |           "type": "PRIVATE",
          |           "whitelistedApplicationIds" : []
          |         },
          |         "endpoints":[
          |            {
          |               "uriPattern":"/today",
          |               "endpointName":"Get Today's Date",
          |               "method":"GET",
          |               "authType":"NONE",
          |               "throttlingTier":"UNLIMITED"
          |            }
          |         ]
          |      }
          |   ]
          |}""".stripMargin.replaceAll("\n", " ")

      val apiDefinition = Json.parse(body).as[APIDefinition]
      apiDefinition.versions.head.access shouldBe Some(PrivateAPIAccess(Seq.empty))
    }

    "read from JSON when the API access type is PRIVATE and there is a non-empty whitelist" in {
      val body =
        """{
          |   "serviceName":"calendar",
          |   "name":"Calendar API",
          |   "description":"My Calendar API",
          |   "serviceBaseUrl":"http://calendar",
          |   "context":"calendar",
          |   "versions":[
          |      {
          |         "version":"1.0",
          |         "status":"PUBLISHED",
          |         "access": {
          |           "type": "PRIVATE",
          |           "whitelistedApplicationIds" : [ "an-application-id" ]
          |         },
          |         "endpoints":[
          |            {
          |               "uriPattern":"/today",
          |               "endpointName":"Get Today's Date",
          |               "method":"GET",
          |               "authType":"NONE",
          |               "throttlingTier":"UNLIMITED"
          |            }
          |         ]
          |      }
          |   ]
          |}""".stripMargin.replaceAll("\n", " ")

      val apiDefinition = Json.parse(body).as[APIDefinition]
      apiDefinition.versions.head.access shouldBe Some(PrivateAPIAccess(Seq("an-application-id")))
    }

    "fail to read from JSON when the API access type is PRIVATE and there is no whitelist" in {
      val body =
        """{
          |   "serviceName":"calendar",
          |   "name":"Calendar API",
          |   "description":"My Calendar API",
          |   "serviceBaseUrl":"http://calendar",
          |   "context":"calendar",
          |   "versions":[
          |      {
          |         "version":"1.0",
          |         "status":"PUBLISHED",
          |         "access": {
          |           "type": "PRIVATE"
          |         },
          |         "endpoints":[
          |            {
          |               "uriPattern":"/today",
          |               "endpointName":"Get Today's Date",
          |               "method":"GET",
          |               "authType":"NONE",
          |               "throttlingTier":"UNLIMITED"
          |            }
          |         ]
          |      }
          |   ]
          |}""".stripMargin.replaceAll("\n", " ")

      intercept[RuntimeException] { Json.parse(body).as[APIDefinition] }
    }

  }

  private def assertValidationFailure(apiDefinition: => APIDefinition, failureMessage: String): Unit = {
    try {
      apiDefinition
      fail("IllegalArgumentException was expected but not thrown")
    } catch {
      case e: IllegalArgumentException =>
        e.getMessage shouldBe s"requirement failed: $failureMessage"
    }
  }

}
