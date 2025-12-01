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

import audit.AuditService
import com.google.inject.Inject
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.*
import uk.gov.hmrc.crypto.{ApplicationCrypto, Decrypter, Encrypter}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

class EmailResponseOldController @Inject()(
                                         val auditService: AuditService,
                                         applicationCrypto: ApplicationCrypto,
                                         cc: ControllerComponents,
                                         parsers: PlayBodyParsers
                                       )(implicit val ec: ExecutionContext)
  extends BackendController(cc) with AuditEmailStatus {

  override protected val logger: Logger = Logger(classOf[EmailResponseOldController])
  override protected val crypto: Encrypter & Decrypter = applicationCrypto.QueryParameterCrypto

  def retrieveStatus(id: String): Action[JsValue] = Action(parsers.tolerantJson) {
    implicit request =>
      logger.warn("application parameter encrypted psaId email status parameter")
      auditRetrieveStatus(id)
  }

  def retrieveStatusRacDac(id: String): Action[JsValue] = Action(parsers.tolerantJson) {
    implicit request =>
      logger.warn("application parameter encrypted psaId email status parameter")
      auditRetrieveStatusRacDac(id)
  }

}
