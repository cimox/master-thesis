import threading
import ujson

from api import logger
from kafka import KafkaConsumer


class Consumer(threading.Thread):
    daemon = True

    def __init__(self, topic, bootstrap_servers='localhost:9092'):
        logger.info('Opening consumer for topic {} on {}'.format(topic, bootstrap_servers))
        super(Consumer, self).__init__()
        self.consumer = KafkaConsumer(bootstrap_servers=bootstrap_servers, auto_offset_reset='latest')
        self.consumer.subscribe(topic)

    def close(self):
        self.consumer.close()

    @staticmethod
    def deserialize_msg(msg):
        try:
            # deserialize bytes kafka response
            return ujson.loads(msg.value)
        except ValueError as e:
            logger.exception('While deserialization msg {}'.format(msg.value))

    @staticmethod
    def serialize(data):
        try:
            return ujson.dumps(data)
        except ValueError as e:
            logger.exception('Serializing data {}'.format(data))

    def process_messages(self):
        for msg in self.consumer:
            logger.debug('Emitting message {}'.format(msg.value))
            yield self.serialize(self.deserialize_msg(msg))
