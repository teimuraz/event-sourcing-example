/*
 *
 * Copyright 2021 TK
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package tk.lib.pubsub

/**
  * Message acknowledgement
  */
sealed trait AckReply

object AckReply {

  /**
    * Acknowledges that the message has been successfully processed. Pubsub will not send the
    * message again.
    */
  case object Ack extends AckReply

  /**
    * Signals that the message has not been successfully processed. Pubsub will resend the
    * message.
    */
  case object Nack extends AckReply
}
