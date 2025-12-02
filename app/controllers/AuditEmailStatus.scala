/*
 * Copyright 2025 HM Revenue & Customs
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

import audit.{AuditEvent, AuditService, EmailAuditEvent, RACDACSubmissionEmailEvent}
import models.{EmailEvents, Event, Opened}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{Request, Result}
import play.api.mvc.Results.{BadRequest, Forbidden, Ok}
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter}
import uk.gov.hmrc.domain.PsaId

import scala.concurrent.ExecutionContext
import scala.util.Try

trait AuditEmailStatus {

  implicit val ec: ExecutionContext
  protected val logger: Logger
  protected val auditService: AuditService
  protected val crypto: Encrypter & Decrypter

  private def validatePsaId(id: String): Option[PsaId] =
    Try(
      PsaId(crypto.decrypt(Crypted(id)).value)
    ).toOption

  private def auditEmailStatus(id: String, f: (PsaId, Event) => AuditEvent)(implicit request: Request[JsValue]): Result =
    validatePsaId(id).fold(
      Forbidden("Malformed PSAID")
    )(psaId =>
      request.body.validate[EmailEvents] match {
        case JsSuccess(valid, _) =>
          valid.events.filterNot(
            _.event == Opened
          ).foreach { event =>
            logger.debug(s"Email Audit event is $event")
            auditService.sendEvent(f(psaId, event.event))
          }
          Ok
        case JsError(_) =>
          BadRequest("Bad request received for email call back event")
      }
    )

  private def emailAuditEvent(psaId: PsaId, event: Event): EmailAuditEvent = EmailAuditEvent(psaId, event)

  private def racDacSubmissionEmailAuditEvent(psaId: PsaId, event: Event): RACDACSubmissionEmailEvent = RACDACSubmissionEmailEvent(psaId, event)

  protected def auditRetrieveStatus(id: String)(implicit request: Request[JsValue]): Result =
    auditEmailStatus(id, emailAuditEvent)

  protected def auditRetrieveStatusRacDac(id: String)(implicit request: Request[JsValue]): Result =
    auditEmailStatus(id, racDacSubmissionEmailAuditEvent)

}
