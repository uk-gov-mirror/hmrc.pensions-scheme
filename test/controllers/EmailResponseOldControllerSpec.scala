/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import audit.testdoubles.StubSuccessfulAuditService
import audit.{AuditService, EmailAuditEvent, RACDACSubmissionEmailEvent}
import base.SpecBase
import models.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.*
import repositories.*
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.domain.PsaId

import java.time.Instant

class EmailResponseOldControllerSpec extends SpecBase with MockitoSugar {

  val psa: PsaId = PsaId("A7654321")
  val fakeAuditService: StubSuccessfulAuditService = new StubSuccessfulAuditService()
  val validJson: JsObject = Json.obj("name" -> "value")
  val emailEvents: EmailEvents =
    EmailEvents(Seq(
      EmailEvent(Sent, Instant.now()),
      EmailEvent(Delivered, Instant.now()),
      EmailEvent(Opened, Instant.now())
    ))

  val crypto: ApplicationCrypto = app.injector.instanceOf[ApplicationCrypto]
  val controller: EmailResponseOldController = app.injector.instanceOf[EmailResponseOldController]

  protected override def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].to(fakeAuditService),
      bind[LockRepository].toInstance(mock[LockRepository]),
      bind[RacdacSchemeSubscriptionCacheRepository].toInstance(mock[RacdacSchemeSubscriptionCacheRepository]),
      bind[SchemeCacheRepository].toInstance(mock[SchemeCacheRepository]),
      bind[SchemeDetailsCacheRepository].toInstance(mock[SchemeDetailsCacheRepository]),
      bind[SchemeDetailsWithIdCacheRepository].toInstance(mock[SchemeDetailsWithIdCacheRepository]),
      bind[SchemeSubscriptionCacheRepository].toInstance(mock[SchemeSubscriptionCacheRepository]),
      bind[UpdateSchemeCacheRepository].toInstance(mock[UpdateSchemeCacheRepository])
    )

  "EmailResponseController retrieveStatus" must {
    "respond OK when given EmailEvents" which {
      "will send events excluding Opened to audit service" in {
        val encrypted = crypto.QueryParameterCrypto.encrypt(PlainText(psa.id)).value
        val result = controller.retrieveStatus(encrypted)(fakeRequest.withBody(Json.toJson(emailEvents)))

        status(result) mustBe OK
        fakeAuditService.verifySent(EmailAuditEvent(psa, Sent)) mustBe true
        fakeAuditService.verifySent(EmailAuditEvent(psa, Delivered)) mustBe true
        fakeAuditService.verifySent(EmailAuditEvent(psa, Opened)) mustBe false
      }
    }

    "respond with BAD_REQUEST when not given EmailEvents" in {
      fakeAuditService.reset()

      val encrypted = crypto.QueryParameterCrypto.encrypt(PlainText(psa.id)).value
      val result = controller.retrieveStatus(encrypted)(fakeRequest.withBody(validJson))

      status(result) mustBe BAD_REQUEST
      fakeAuditService.verifyNothingSent() mustBe true
    }

    "respond with FORBIDDEN" when {
      "URL contains an id does not match PSAID pattern" in {
        fakeAuditService.reset()

        val psa = crypto.QueryParameterCrypto.encrypt(PlainText("psa")).value
        val result = controller.retrieveStatus(psa)(fakeRequest.withBody(Json.toJson(emailEvents)))

        status(result) mustBe FORBIDDEN
        contentAsString(result) mustBe "Malformed PSAID"
        fakeAuditService.verifyNothingSent() mustBe true
      }
    }
  }

  "EmailResponseController retrieveStatusRacDac" must {
    "respond OK when given EmailEvents" which {
      "will send events excluding Opened to audit service" in {
        fakeAuditService.reset()

        val encrypted = crypto.QueryParameterCrypto.encrypt(PlainText(psa.id)).value
        val result = controller.retrieveStatusRacDac(encrypted)(fakeRequest.withBody(Json.toJson(emailEvents)))

        status(result) mustBe OK
        fakeAuditService.verifySent(RACDACSubmissionEmailEvent(psa, Sent)) mustBe true
        fakeAuditService.verifySent(RACDACSubmissionEmailEvent(psa, Delivered)) mustBe true
        fakeAuditService.verifySent(RACDACSubmissionEmailEvent(psa, Opened)) mustBe false
      }
    }

    "respond with BAD_REQUEST when not given EmailEvents" in {
      fakeAuditService.reset()

      val encrypted = crypto.QueryParameterCrypto.encrypt(PlainText(psa.id)).value
      val result = controller.retrieveStatusRacDac(encrypted)(fakeRequest.withBody(validJson))

      status(result) mustBe BAD_REQUEST
      fakeAuditService.verifyNothingSent() mustBe true
    }

    "respond with FORBIDDEN" when {
      "URL contains an id does not match PSAID pattern" in {
        fakeAuditService.reset()

        val psa = crypto.QueryParameterCrypto.encrypt(PlainText("psa")).value
        val result = controller.retrieveStatusRacDac(psa)(fakeRequest.withBody(Json.toJson(emailEvents)))

        status(result) mustBe FORBIDDEN
        contentAsString(result) mustBe "Malformed PSAID"
        fakeAuditService.verifyNothingSent() mustBe true
      }
    }
  }
}
