package com.llw.kafka.sample;

import org.apache.kafka.clients.producer.*;

import java.util.Properties;

/**
 * 生产者
 */
public class ProducerQuickStart {

    private static String ALI_YUN = "8.130.138.68:9092";

    public static void main(String[] args) {
        //1.kafka的配置信息
        Properties properties = new Properties();
        //kafka的连接地址
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ALI_YUN);
        //发送失败，失败的重试次数
        properties.put(ProducerConfig.RETRIES_CONFIG, 5);
        //消息key的序列化器
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        //消息value的序列化器
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        //2.生产者对象
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

        //封装发送的消息
        ProducerRecord<String, String> record = new ProducerRecord<String, String>("leadnews-topic", "100001", "hello kafka");

        //3.发送消息
        producer.send(record);

        ProducerRecord kvProducerRecord = new ProducerRecord("leadnews-topic", "hello");
        //异步消息发送
        producer.send(kvProducerRecord, new Callback() {
            @Override
            public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                if(e != null){
                    System.out.println("记录异常信息到日志表中");
                }
                System.out.println(recordMetadata.offset());
            }
        });

        //4.关闭消息通道，必须关闭，否则消息发送不成功
        producer.close();
    }

}