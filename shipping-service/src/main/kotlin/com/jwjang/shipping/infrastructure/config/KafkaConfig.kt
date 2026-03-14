package com.jwjang.shipping.infrastructure.config

import com.jwjang.shipping.infrastructure.event.PaymentCompletedEvent
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer

@Configuration
class KafkaConfig(
    @Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String,
    @Value("\${spring.kafka.consumer.group-id}") private val groupId: String
) {

    private fun baseConsumerProps(): Map<String, Any> = mapOf(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ConsumerConfig.GROUP_ID_CONFIG to groupId,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java
    )

    @Bean("paymentCompletedFactory")
    fun paymentCompletedContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, PaymentCompletedEvent> {
        val deserializer = JsonDeserializer(PaymentCompletedEvent::class.java).apply {
            addTrustedPackages("com.jwjang.*")
            setUseTypeHeaders(false)
        }
        val consumerFactory: ConsumerFactory<String, PaymentCompletedEvent> =
            DefaultKafkaConsumerFactory(baseConsumerProps(), StringDeserializer(), deserializer)
        return ConcurrentKafkaListenerContainerFactory<String, PaymentCompletedEvent>().apply {
            this.consumerFactory = consumerFactory
        }
    }

    @Bean("mapEventFactory")
    fun mapEventContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Map<*, *>> {
        val deserializer = JsonDeserializer(Map::class.java).apply {
            addTrustedPackages("*")
            setUseTypeHeaders(false)
        }
        @Suppress("UNCHECKED_CAST")
        val consumerFactory: ConsumerFactory<String, Map<*, *>> =
            DefaultKafkaConsumerFactory(baseConsumerProps(), StringDeserializer(), deserializer)
        return ConcurrentKafkaListenerContainerFactory<String, Map<*, *>>().apply {
            this.consumerFactory = consumerFactory
        }
    }
}
