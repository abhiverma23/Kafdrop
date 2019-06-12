/*
 * Copyright 2017 HomeAdvisor, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.homeadvisor.kafdrop.service;

import com.google.gson.JsonObject;
import com.homeadvisor.kafdrop.model.MessageVO;
import com.homeadvisor.kafdrop.model.SearchStringComparatorVO;
import com.homeadvisor.kafdrop.model.TopicPartitionVO;
import com.homeadvisor.kafdrop.model.TopicVO;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class MessageInspector
{
   private final Logger LOG = LoggerFactory.getLogger(getClass());

   @Autowired
   private KafkaMonitor kafkaMonitor;

   private Properties consumerProperties = null;

   @Autowired
   private MessageInspector(KafkaMonitor kafkaMonitor) {
      this.kafkaMonitor = kafkaMonitor;
      this.consumerProperties = new Properties();
      this.consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG,"com.homeadvisor.kafdrop.service");
      this.consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
      this.consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
      this.consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
      this.consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
   }

   public List<MessageVO> getMessages(String topicName, int partitionId, long offset, long count, String searchBy)
   {
      final TopicVO topic = kafkaMonitor.getTopic(topicName).orElseThrow(TopicNotFoundException::new);
      final TopicPartitionVO partition = topic.getPartition(partitionId).orElseThrow(PartitionNotFoundException::new);

      long lastOffset = partition.getSize();
      final long finalCount = lastOffset >= offset + count ? count : lastOffset - offset;
      final SearchStringComparatorVO contains = new SearchStringComparatorVO(searchBy);

      //----------- here and remove return;

      KafkaConsumer<String, String> consumer = new KafkaConsumer(this.getConsumerConfiguration());
      TopicPartition topicPartition = new TopicPartition(topicName, partitionId);
      consumer.assign(Collections.singleton(topicPartition));
      consumer.seek(topicPartition, offset);

      List<MessageVO> messages = new ArrayList<>();

      ConsumerRecords<String, String> records = null;
      while (messages.size() < finalCount && (records==null || !records.isEmpty())) {
         records = consumer.poll(1000);

         if (StringUtils.isBlank(searchBy)) {
            StreamSupport.stream(records.spliterator(), false)
                    .limit(finalCount - messages.size())
                    .map(this::createMessage)
                    .forEach(messages::add);
         } else {
            StreamSupport.stream(records.spliterator(), false)
                    .map(this::createMessage)
                    .filter(messageVO -> contains.validate(messageVO.getMessage()))
                    .limit(finalCount - messages.size())
                    .forEach(messages::add);
         }
      }

      return messages;

      /*return kafkaMonitor.getBroker(partition.getLeader().getId())
         .map(broker -> {
            SimpleConsumer consumer2 = new SimpleConsumer(broker.getHost(), broker.getPort(), 10000, 100000, "");

            final FetchRequestBuilder fetchRequestBuilder = new FetchRequestBuilder()
               .clientId("KafDrop")
               .maxWait(5000) // todo: make configurable
               .minBytes(1);

            List<MessageVO> messages = new ArrayList<>();
            long currentOffset = offset;
            while (messages.size() < finalCount)
            {
               final FetchRequest fetchRequest =
                  fetchRequestBuilder
                     .addFetch(topicName, partitionId, currentOffset, 1024 * 1024)
                     .build();

               FetchResponse fetchResponse = consumer2.fetch(fetchRequest);

               final ByteBufferMessageSet messageSet = fetchResponse.messageSet(topicName, partitionId);
               if (messageSet.validBytes() <= 0) break;


               int oldSize = messages.size();
               StreamSupport.stream(messageSet.spliterator(), false)
                  .limit(finalCount - messages.size())
                  .map(MessageAndOffset::message)
                  .map(this::createMessage)
                  .forEach(messages::add);
               currentOffset += messages.size() - oldSize;
            }

            return messages;
         })
         .orElseGet(Collections::emptyList);*/
   }

   private MessageVO createMessage(ConsumerRecord<String, String> message)
   {
      MessageVO vo = new MessageVO();
      if (Objects.nonNull(message.key()))
      {
         vo.setKey(message.key());
      }
      if (Objects.nonNull(message.value()))
      {
         vo.setMessage(message.value());
      }
      if (Objects.nonNull(message.offset()))
      {
         vo.setOffset(message.offset());
      }


      if (Objects.nonNull(message.headers())) {
         JsonObject headers = new JsonObject();
         message.headers().forEach(h -> {
            headers.addProperty(h.key(),new String(h.value()));
         });
         vo.setHeaders(headers.toString());
      }

      //vo.setValid(message.isValid());
      vo.setCompressionCodec("");
      vo.setChecksum(message.checksum());
      //vo.setComputedChecksum(message.computeChecksum());
      //vo.setHeaders();

      return vo;
   }

   private String readString(ByteBuffer buffer)
   {
      try
      {
         return new String(readBytes(buffer), "UTF-8");
      }
      catch (UnsupportedEncodingException e)
      {
         return "<unsupported encoding>";
      }
   }

   private byte[] readBytes(ByteBuffer buffer)
   {
      return readBytes(buffer, 0, buffer.limit());
   }

   private byte[] readBytes(ByteBuffer buffer, int offset, int size)
   {
      byte[] dest = new byte[size];
      if (buffer.hasArray())
      {
         System.arraycopy(buffer.array(), buffer.arrayOffset() + offset, dest, 0, size);
      }
      else
      {
         buffer.mark();
         buffer.get(dest);
         buffer.reset();
      }
      return dest;
   }

   private Properties getConsumerConfiguration() {
      try {
         StringBuilder brokers = new StringBuilder();
         kafkaMonitor.getBrokers().forEach(brokerVO -> brokers.append(brokerVO.getHost()+":"+brokerVO.getPort()+","));
         this.consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers.toString());
         return consumerProperties;
      } catch (Exception ex) {
         LOG.error("Error while building consumer configuration.", ex);
         return consumerProperties;
      }
   }

}
