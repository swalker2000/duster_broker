package com.duster.common.messagepublishinterface

import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto

/**
 * Действия, которые необходимо совершить, чтобы отправить сообщение consumer.
 */
fun interface ConsumerMessagePublishAction {
    /**
     * Действия, которые необходимо совершить, чтобы отправить сообщение consumer.
     */
    fun publishAction(deviseId: String, consumerMessageOutDto: ConsumerMessageOutDto)
}