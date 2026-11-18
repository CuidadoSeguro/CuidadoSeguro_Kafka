package cl.duoc.dsy1107.demo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    public static final String TOPIC_SIGNOS_VITALES = "signos-vitales";

    @Bean
    NewTopic signosVitales() {
        return TopicBuilder.name(TOPIC_SIGNOS_VITALES).partitions(3).replicas(1).build();
    }
}
