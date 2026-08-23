package com.fininsight.config.kafka;

/**
 * Centralized Kafka topic names and Dead Letter Topics (DLT).
 */
public final class KafkaTopicNames {

    private KafkaTopicNames() {
        // Utility class
    }

    public static final String TRANSACTION_EVENTS = "fininsight.transaction.events";
    public static final String TRANSACTION_EVENTS_DLT = "fininsight.transaction.events.DLT";

    public static final String BUDGET_EVENTS = "fininsight.budget.events";
    public static final String BUDGET_EVENTS_DLT = "fininsight.budget.events.DLT";

    public static final String CATEGORY_EVENTS = "fininsight.category.events";
    public static final String CATEGORY_EVENTS_DLT = "fininsight.category.events.DLT";

    public static final String CONSUMER_GROUP = "fininsight-consumer-group";
}
