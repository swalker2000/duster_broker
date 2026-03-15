package com.duster.common.messagepublishinterface

import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto

/**
 * Действия, которые необходимо совершить, чтобы отправить сообщение producer.
 */
fun interface ProducerMessagePublishAction {

    /**
     * Действия, которые необходимо совершить, чтобы отправить сообщение producer.
     */
    fun publishAction(deviseId: String, consumerMessageOutDto: ConsumerMessageOutDto)
}